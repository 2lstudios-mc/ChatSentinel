package dev._2lstudios.chatsentinel.bungee;

import java.util.concurrent.TimeUnit;

import dev._2lstudios.chatsentinel.bungee.commands.ChatSentinelCommand;
import dev._2lstudios.chatsentinel.bungee.listeners.ChatListener;
import dev._2lstudios.chatsentinel.bungee.listeners.PlayerDisconnectListener;
import dev._2lstudios.chatsentinel.bungee.listeners.PostLoginListener;
import dev._2lstudios.chatsentinel.bungee.modules.BungeeModuleManager;
import dev._2lstudios.chatsentinel.bungee.utils.ConfigUtil;
import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatNotificationManager;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.modules.CooldownModerationModule;
import dev._2lstudios.chatsentinel.shared.modules.GeneralModule;
import dev._2lstudios.chatsentinel.shared.modules.MessagesModule;
import dev._2lstudios.chatsentinel.shared.modules.ModerationModule;
import dev._2lstudios.chatsentinel.shared.modules.SyntaxModerationModule;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

public class ChatSentinel extends Plugin {
	// Static instance
	private static ChatSentinel instance;

	public static ChatSentinel getInstance() {
		return instance;
	}

	public static void setInstance(ChatSentinel instance) {
		ChatSentinel.instance = instance;
	}

	// Module Manager
	private BungeeModuleManager moduleManager;

	public BungeeModuleManager getModuleManager() {
		return moduleManager;
	}

	@Override
	public void onEnable() {
		setInstance(this);

		ConfigUtil configUtil = new ConfigUtil(this);

		configUtil.create("%datafolder%/config.yml");
		configUtil.create("%datafolder%/messages.yml");
		configUtil.create("%datafolder%/whitelist.yml");
		configUtil.create("%datafolder%/blacklist.yml");

		ProxyServer server = getProxy();
		moduleManager = new BungeeModuleManager(configUtil);
		GeneralModule generalModule = moduleManager.getGeneralModule();
		ChatPlayerManager chatPlayerManager = new ChatPlayerManager();
		ChatNotificationManager chatNotificationManager = new ChatNotificationManager();
		PluginManager pluginManager = server.getPluginManager();

		pluginManager.registerListener(this, new ChatListener(moduleManager.getWhitelistModule(), chatPlayerManager, chatNotificationManager));
		pluginManager.registerListener(this, new PlayerDisconnectListener(generalModule, chatPlayerManager, chatNotificationManager));
		pluginManager.registerListener(this, new PostLoginListener(generalModule, chatPlayerManager, chatNotificationManager));

		pluginManager.registerCommand(this, new ChatSentinelCommand(chatPlayerManager, chatNotificationManager, moduleManager, server));

		getProxy().getScheduler().schedule(this, () -> {
			if (generalModule.needsNicknameCompile()) {
				generalModule.compileNicknamesPattern();
			}
		}, 1000L, 1000L, TimeUnit.MILLISECONDS);
	}

	public void dispatchCommmands(ModerationModule moderationModule, ChatPlayer chatPlayer, String[][] placeholders) {
		ProxyServer server = getProxy();

		server.getScheduler().runAsync(this, () -> {
			CommandSender console = server.getConsole();

			for (String command : moderationModule.getCommands(placeholders)) {
				server.getPluginManager().dispatchCommand(console, command);
			}
		});

		chatPlayer.clearWarns();
	}

	public void dispatchNotification(ModerationModule moderationModule, String[][] placeholders, ChatNotificationManager chatNotificationManager) {
		ProxyServer server = getProxy();
		String notificationMessage = moderationModule.getWarnNotification(placeholders);

		if (notificationMessage != null && !notificationMessage.isEmpty()) {
			for (ChatPlayer chatPlayer : chatNotificationManager.getAllPlayers()) {
				ProxiedPlayer player = server.getPlayer(chatPlayer.getUniqueId());
				if (player != null) {
					player.sendMessage(notificationMessage);
				}
			}

			server.getConsole().sendMessage(notificationMessage);
		}
	}

	public String[][] getPlaceholders(ProxiedPlayer player, ChatPlayer chatPlayer, ModerationModule moderationModule, String message) {
		String playerName = player.getName();
		int warns = chatPlayer.getWarns(moderationModule);
		int maxWarns = moderationModule.getMaxWarns();
		float remainingTime = moduleManager.getCooldownModule().getRemainingTime(chatPlayer, message);
		Server server = player.getServer();
		String serverName = server != null ? server.getInfo().getName() : "";

		return new String[][] {
				{ "%player%", "%message%", "%warns%", "%maxwarns%", "%cooldown%", "%server_name%" },
				{ playerName, message, String.valueOf(warns), String.valueOf(maxWarns), String.valueOf(remainingTime), serverName }
		};
	}

	public void sendWarning(String[][] placeholders, ModerationModule moderationModule, ProxiedPlayer player, String lang) {
		String warnMessage = moduleManager.getMessagesModule().getWarnMessage(placeholders, lang, moderationModule.getName());

		if (warnMessage != null && !warnMessage.isEmpty()) {
			player.sendMessage(warnMessage);
		}
	}

	public ChatEventResult processEvent(ChatPlayer chatPlayer, ProxiedPlayer player, String originalMessage, ChatNotificationManager chatNotificationManager) {
		ChatEventResult finalResult = new ChatEventResult(originalMessage, false, false);
		MessagesModule messagesModule = moduleManager.getMessagesModule();
		String playerName = player.getName();
		String lang = chatPlayer.getLocale();
		ModerationModule[] moderationModulesToProcess = {
				moduleManager.getSyntaxModule(),
				moduleManager.getCapsModule(),
				moduleManager.getCooldownModule(),
				moduleManager.getFloodModule(),
				moduleManager.getBlacklistModule()
		};

		for (ModerationModule moderationModule : moderationModulesToProcess) {
			// Do not check annormal commands (unless syntax or cooldown)
			boolean isCommmand = originalMessage.startsWith("/");
			boolean isNormalCommmand = ChatSentinel.getInstance().getModuleManager().getGeneralModule()
					.isCommand(originalMessage);
			if (!(moderationModule instanceof SyntaxModerationModule) &&
					!(moderationModule instanceof CooldownModerationModule) &&
					isCommmand &&
					!isNormalCommmand) {
				continue;
			}

			// Get the modified message
			String message = finalResult.getMessage();

			// Check if player has bypass
			if (player.hasPermission(moderationModule.getBypassPermission())) {
				continue;
			}

			// Process
			ChatEventResult result = moderationModule.processEvent(chatPlayer, messagesModule, playerName, message, lang);

			// Skip result
			if (result != null) {
				// Add warning
				chatPlayer.addWarn(moderationModule);

				// Get placeholders
				String[][] placeholders = ChatSentinel.getInstance().getPlaceholders(player, chatPlayer, moderationModule,
						message);

				// Send warning
				ChatSentinel.getInstance().sendWarning(placeholders, moderationModule, player, lang);

				// Send punishment comamnds
				if (moderationModule.hasExceededWarns(chatPlayer)) {
					ChatSentinel.getInstance().dispatchCommmands(moderationModule, chatPlayer, placeholders);
				}

				// Send admin notification
				ChatSentinel.getInstance().dispatchNotification(moderationModule, placeholders, chatNotificationManager);

				// Update message
				finalResult.setMessage(result.getMessage());

				// Update hide
				if (result.isHide())
					finalResult.setHide(true);

				// Update cancelled
				if (result.isCancelled()) {
					finalResult.setCancelled(true);
					break;
				}
			}
		}

		return finalResult;
	}
}