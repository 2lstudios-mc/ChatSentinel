package dev._2lstudios.chatsentinel.velocity.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import dev._2lstudios.chatsentinel.shared.modules.WhitelistModule;
import dev._2lstudios.chatsentinel.velocity.ChatSentinel;
import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class ChatListener {
	private final ChatSentinel plugin;
	private final WhitelistModule whitelistModule;

	public ChatListener(ChatSentinel plugin, WhitelistModule whitelistModule) {
		this.plugin = plugin;
        this.whitelistModule = whitelistModule;
    }

	@Subscribe(order = PostOrder.LAST)
	public void onChatEvent(PlayerChatEvent event) {
		if (!event.getResult().isAllowed()) {
			return;
		}

		// Sender
		Player player = event.getPlayer();
		
		if (player == null) {
			return;
		}

		// Check if the player's current server is on the whitelist
		if (player.getCurrentServer().isPresent()) {
			String playerCurrentServer = player.getCurrentServer().get().getServerInfo().getName();
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
		ChatPlayer chatPlayer = plugin.getChatPlayerManager().getPlayer(player);

		// Process the event
		ChatEventResult finalResult = plugin.processEvent(chatPlayer, player, message);

		// Apply modifiers to event
		if (finalResult.isCancelled()) {
			event.setResult(PlayerChatEvent.ChatResult.denied());
		} else {
			event.setResult(PlayerChatEvent.ChatResult.message(finalResult.getMessage()));
		}

		// Set last message
		if (event.getResult().isAllowed()) {
			chatPlayer.addLastMessage(finalResult.getMessage(), System.currentTimeMillis());
		}
	}
}
