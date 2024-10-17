package dev._2lstudios.chatsentinel.bungee.listeners;

import dev._2lstudios.chatsentinel.bungee.ChatSentinel;
import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatNotificationManager;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.modules.WhitelistModule;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ChatListener implements Listener {
	private WhitelistModule whitelistModule;
	private ChatPlayerManager chatPlayerManager;
	private ChatNotificationManager chatNotificationManager;

	public ChatListener(WhitelistModule whitelistModule, ChatPlayerManager chatPlayerManager, ChatNotificationManager chatNotificationManager) {
		this.whitelistModule = whitelistModule;
		this.chatPlayerManager = chatPlayerManager;
		this.chatNotificationManager = chatNotificationManager;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onChatEvent(ChatEvent event) {
		if (event.isCancelled()) {
			return;
		}

		// Sender
		Connection sender = event.getSender();
		
		if (!(sender instanceof ProxiedPlayer)) {
			return;
		}

		// Get player
		ProxiedPlayer player = (ProxiedPlayer) sender;

		// Check if the player's current server is on the whitelist
		if (player.getServer() != null) {
			String playerCurrentServer = player.getServer().getInfo().getName();
			if (whitelistModule.getWhitelistedServers().contains(playerCurrentServer)) {
				return;
			}
		}

		// Check if player has bypass
		if (player.hasPermission("chatsentinel.bypass")) {
			return;
		}

		// Get event variables
		String message = event.getMessage();

		// Get chat player
		ChatPlayer chatPlayer = chatPlayerManager.getPlayer(player);

		// Process the event
		ChatEventResult finalResult = ChatSentinel.getInstance().processEvent(chatPlayer, player, message, chatNotificationManager);

		// Apply modifiers to event
		if (finalResult.isCancelled()) {
			event.setCancelled(true);
		} else {
			event.setMessage(finalResult.getMessage());
		}

		// Set last message
		if (!event.isCancelled()) {
			chatPlayer.addLastMessage(finalResult.getMessage(), System.currentTimeMillis());
		}
	}
}
