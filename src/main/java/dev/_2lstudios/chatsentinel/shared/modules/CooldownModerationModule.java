package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class CooldownModerationModule extends ModerationModule {
	private int repeatTimeGlobal;
	private int repeatTime;
	private int normalTime;
	private int commandTime;

	private long lastMessageTime = 0L;
	private String lastMessage = "";

	public void loadData(boolean enabled, int repeatTimeGlobal, int repeatTime,
			int normalTime,
			int commandTime) {
		setEnabled(enabled);
		this.repeatTimeGlobal = repeatTimeGlobal;
		this.repeatTime = repeatTime;
		this.normalTime = normalTime;
		this.commandTime = commandTime;
	}

	public float getRemainingTime(ChatPlayer chatPlayer, String message) {
		if (isEnabled() && message != null) {
			long currentTime = System.currentTimeMillis();
			long lastMessageTimePassed = currentTime - chatPlayer.getLastMessageTime();
			long lastMessageTimePassedGlobal = currentTime - this.lastMessageTime;
			long remainingTime;

			if (message.startsWith("/")) {
				remainingTime = this.commandTime - lastMessageTimePassed;
			} else if (chatPlayer.isLastMessage(message) && lastMessageTimePassed < this.repeatTime) {
				remainingTime = this.repeatTime - lastMessageTimePassed;
			} else if (this.lastMessage.equals(message) && lastMessageTimePassedGlobal < this.repeatTimeGlobal) {
				remainingTime = this.repeatTimeGlobal - lastMessageTimePassedGlobal;
			} else {
				remainingTime = this.normalTime - lastMessageTimePassed;
			}

			if (remainingTime > 0) {
				return ((int) (remainingTime / 100F)) / 10F;
			}
		}

		return 0;
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String originalMessage, String lang) {
		if (isEnabled() && getRemainingTime(chatPlayer, originalMessage) > 0) {
			return new ChatEventResult(originalMessage, true);
		}

		return null;
	}

	@Override
	public String getName() {
		return "Cooldown";
	}

	@Override
	public String getCustomName() {
		return getName();
	}

	@Override
	public String getWarnNotification(String[][] placeholders) {
		return null;
	}

	public void setLastMessage(String lastMessage, long lastMessageTime) {
		this.lastMessage = lastMessage;
		this.lastMessageTime = lastMessageTime;
	}
}
