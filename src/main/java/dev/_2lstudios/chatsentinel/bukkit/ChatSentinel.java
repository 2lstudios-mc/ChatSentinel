package dev._2lstudios.chatsentinel.bukkit;

import dev._2lstudios.chatsentinel.shared.chat.ChatNotificationManager;
import dev._2lstudios.chatsentinel.shared.modules.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import dev._2lstudios.chatsentinel.bukkit.commands.ChatSentinelCommand;
import dev._2lstudios.chatsentinel.bukkit.listeners.AsyncPlayerChatListener;
import dev._2lstudios.chatsentinel.bukkit.listeners.PlayerJoinListener;
import dev._2lstudios.chatsentinel.bukkit.listeners.PlayerQuitListener;
import dev._2lstudios.chatsentinel.bukkit.listeners.ServerCommandListener;
import dev._2lstudios.chatsentinel.bukkit.modules.BukkitModuleManager;
import dev._2lstudios.chatsentinel.bukkit.utils.ConfigUtil;
import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;

public class ChatSentinel extends JavaPlugin {
	// Static instance
	private static ChatSentinel instance;

	public static ChatSentinel getInstance() {
		return instance;
	}

	public static void setInstance(ChatSentinel instance) {
		ChatSentinel.instance = instance;
	}

	// Module Manager
	private BukkitModuleManager moduleManager;

	public BukkitModuleManager getModuleManager() {
		return moduleManager;
	}

	@Override
	public void onEnable() {
		setInstance(this);

		ConfigUtil configUtil = new ConfigUtil(this);
		Server server = getServer();

		moduleManager = new BukkitModuleManager(configUtil);
		GeneralModule generalModule = moduleManager.getGeneralModule();
		ChatPlayerManager chatPlayerManager = new ChatPlayerManager();
		ChatNotificationManager chatNotificationManager = new ChatNotificationManager();
		PluginManager pluginManager = server.getPluginManager();

		pluginManager.registerEvents(new AsyncPlayerChatListener(chatPlayerManager, chatNotificationManager), this);
		pluginManager.registerEvents(new PlayerJoinListener(generalModule, chatPlayerManager, chatNotificationManager), this);
		pluginManager.registerEvents(new PlayerQuitListener(moduleManager.getGeneralModule(), chatPlayerManager, chatNotificationManager), this);
		pluginManager.registerEvents(new ServerCommandListener(chatPlayerManager, chatNotificationManager), this);

		getCommand("chatsentinel").setExecutor(new ChatSentinelCommand(chatPlayerManager, chatNotificationManager, moduleManager, server));

		getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
			if (generalModule.needsNicknameCompile()) {
				generalModule.compileNicknamesPattern();
			}
		}, 20L, 20L);
	}

	public void dispatchCommmands(ModerationModule moderationModule, ChatPlayer chatPlayer, String[][] placeholders) {
		Server server = getServer();

		server.getScheduler().runTask(this, () -> {
			ConsoleCommandSender console = server.getConsoleSender();

			for (String command : moderationModule.getCommands(placeholders)) {
				server.dispatchCommand(console, command);
			}
		});

		chatPlayer.clearWarns();
	}

	public void dispatchNotification(ModerationModule moderationModule, String[][] placeholders, ChatNotificationManager chatNotificationManager) {
		Server server = getServer();
		String notificationMessage = moderationModule.getWarnNotification(placeholders);

		if (notificationMessage != null && !notificationMessage.isEmpty()) {
			for (ChatPlayer chatPlayer : chatNotificationManager.getAllPlayers()) {
				Player player = Bukkit.getPlayer(chatPlayer.getUniqueId());
                if (player != null) {
					player.sendMessage(notificationMessage);
				}
			}

			server.getConsoleSender().sendMessage(notificationMessage);
		}
	}

	public String[][] getPlaceholders(Player player, ChatPlayer chatPlayer, ModerationModule moderationModule, String message) {
		String playerName = player.getName();
		String customModuleName = moderationModule.getCustomName();
		int warns = chatPlayer.getWarns(moderationModule);
		int maxWarns = moderationModule.getMaxWarns();
		float remainingTime = moduleManager.getCooldownModule().getRemainingTime(chatPlayer, message);

		return new String[][] {
				{ "%player%", "%module%", "%message%", "%warns%", "%maxwarns%", "%cooldown%" },
				{ playerName, customModuleName, message, String.valueOf(warns), String.valueOf(maxWarns), String.valueOf(remainingTime) }
		};
	}

	public void sendWarning(String[][] placeholders, ModerationModule moderationModule, Player player, String lang) {
		String warnMessage = moduleManager.getMessagesModule().getWarnMessage(placeholders, lang, moderationModule.getName());

		if (warnMessage != null && !warnMessage.isEmpty()) {
			player.sendMessage(warnMessage);
		}
	}

	public ChatEventResult processEvent(ChatPlayer chatPlayer, Player player, String originalMessage, ChatNotificationManager chatNotificationManager) {
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

				// Send discord webhook notification
				Server server = getServer();
				DiscordWebhookModule discordWebhookModule = moduleManager.getDiscordWebhookModule();
				server.getScheduler().runTaskAsynchronously(this, () -> discordWebhookModule.dispatchWebhookNotification(moderationModule, placeholders));

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