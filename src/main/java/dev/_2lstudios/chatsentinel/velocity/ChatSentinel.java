package dev._2lstudios.chatsentinel.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatNotificationManager;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.modules.*;
import dev._2lstudios.chatsentinel.velocity.commands.ChatSentinelCommand;
import dev._2lstudios.chatsentinel.velocity.listeners.ChatListener;
import dev._2lstudios.chatsentinel.velocity.listeners.PlayerDisconnectListener;
import dev._2lstudios.chatsentinel.velocity.listeners.PostLoginListener;
import dev._2lstudios.chatsentinel.velocity.modules.VelocityModuleManager;
import dev._2lstudios.chatsentinel.velocity.utils.ConfigUtil;
import dev._2lstudios.chatsentinel.velocity.utils.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(
		id = Constants.ID,
		name = Constants.NAME,
		version = Constants.VERSION,
		description = Constants.DESCRIPTION,
		url = Constants.URL,
		authors = Constants.AUTHOR
)
public class ChatSentinel {

	private final ProxyServer server;
	private final ComponentLogger logger;
	private final Path dataDirectory;
	private VelocityModuleManager moduleManager;
	private GeneralModule generalModule;
	private ChatPlayerManager chatPlayerManager;
	private ChatNotificationManager chatNotificationManager;

	@Inject
	public ChatSentinel(ProxyServer server, ComponentLogger logger, @DataDirectory Path dataDirectory) {
		this.server = server;
		this.logger = logger;
		this.dataDirectory = dataDirectory;
	}

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {

		ConfigUtil configUtil = new ConfigUtil(this);

		moduleManager = new VelocityModuleManager(configUtil);
		generalModule = moduleManager.getGeneralModule();
		chatPlayerManager = new ChatPlayerManager();
		chatNotificationManager = new ChatNotificationManager();

		EventManager eventManager = server.getEventManager();
		eventManager.register(this, new ChatListener(this, moduleManager.getWhitelistModule()));
		eventManager.register(this, new PlayerDisconnectListener(generalModule, chatPlayerManager, chatNotificationManager));
		eventManager.register(this, new PostLoginListener(generalModule, chatPlayerManager, chatNotificationManager));

		CommandManager commandManager = server.getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("chatsentinel")
				.plugin(this)
				.build();
		SimpleCommand chatSentinelCommand = new ChatSentinelCommand(chatPlayerManager, chatNotificationManager, moduleManager, server);

		commandManager.register(commandMeta, chatSentinelCommand);

		server.getScheduler().buildTask(this, () -> {
			if (generalModule.needsNicknameCompile()) {
				generalModule.compileNicknamesPattern();
			}
		}).delay(1L, TimeUnit.SECONDS).repeat(1L, TimeUnit.SECONDS).schedule();
	}

	public void dispatchCommmands(ModerationModule moderationModule, ChatPlayer chatPlayer, String[][] placeholders) {
		server.getScheduler().buildTask(this, () -> {
			CommandSource console = server.getConsoleCommandSource();

			for (String command : moderationModule.getCommands(placeholders)) {
				server.getCommandManager().executeAsync(console, command);
			}
		}).schedule();

		chatPlayer.clearWarns();
	}

	public void dispatchNotification(ModerationModule moderationModule, String[][] placeholders) {
		ProxyServer server = getServer();
		String notificationMessage = moderationModule.getWarnNotification(placeholders);

		if (notificationMessage != null && !notificationMessage.isEmpty()) {
			for (ChatPlayer chatPlayer : chatNotificationManager.getAllPlayers()) {
				Optional<Player> player = server.getPlayer(chatPlayer.getUniqueId());
                player.ifPresent(player1 -> player1.sendMessage(Component.text(notificationMessage)));
			}

			logger.info(LegacyComponentSerializer.legacySection().deserialize(notificationMessage));
		}
	}

	public String[][] getPlaceholders(Player player, ChatPlayer chatPlayer, ModerationModule moderationModule, String message) {
		String playerName = player.getUsername();
		String customModuleName = moderationModule.getCustomName();
		int warns = chatPlayer.getWarns(moderationModule);
		int maxWarns = moderationModule.getMaxWarns();
		float remainingTime = moduleManager.getCooldownModule().getRemainingTime(chatPlayer, message);
		Optional<ServerConnection> serverConnection = player.getCurrentServer();
		String serverName = serverConnection.isPresent() ? serverConnection.get().getServerInfo().getName() : "";

		return new String[][] {
				{ "%player%", "%module%", "%message%", "%warns%", "%maxwarns%", "%cooldown%", "%server_name%" },
				{ playerName, customModuleName, message, String.valueOf(warns), String.valueOf(maxWarns), String.valueOf(remainingTime), serverName }
		};
	}

	public void sendWarning(String[][] placeholders, ModerationModule moderationModule, Player player, String lang) {
		String warnMessage = moduleManager.getMessagesModule().getWarnMessage(placeholders, lang, moderationModule.getName());

		if (warnMessage != null && !warnMessage.isEmpty()) {
			player.sendMessage(Component.text(warnMessage));
		}
	}

	public ChatEventResult processEvent(ChatPlayer chatPlayer, Player player, String originalMessage) {
		ChatEventResult finalResult = new ChatEventResult(originalMessage, false, false);
		MessagesModule messagesModule = moduleManager.getMessagesModule();
		String playerName = player.getUsername();
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
			boolean isNormalCommmand = moduleManager.getGeneralModule()
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
				String[][] placeholders = getPlaceholders(player, chatPlayer, moderationModule,
						message);

				// Send warning
				sendWarning(placeholders, moderationModule, player, lang);

				// Send punishment comamnds
				if (moderationModule.hasExceededWarns(chatPlayer)) {
					dispatchCommmands(moderationModule, chatPlayer, placeholders);
				}

				// Send admin notification
				dispatchNotification(moderationModule, placeholders);

				// Send discord webhook notification
				DiscordWebhookModule discordWebhookModule = moduleManager.getDiscordWebhookModule();
				server.getScheduler().buildTask(this, () -> discordWebhookModule.dispatchWebhookNotification(moderationModule, placeholders)).schedule();

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

	public ProxyServer getServer() {
		return server;
	}

	public ComponentLogger getLogger() {
		return logger;
	}

	public Path getDataDirectory() {
		return dataDirectory;
	}

	public ChatPlayerManager getChatPlayerManager() {
		return chatPlayerManager;
	}
}