package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class CapsModerationModule extends ModerationModule {
	private String customName;
	private boolean replace;
	private int maxCaps;

	public void loadData(boolean enabled, String customName, boolean replace, int max, int maxWarns,
			String warnNotification, String[] commands) {
		setEnabled(enabled);
		setMaxWarns(maxWarns);
		setWarnNotification(warnNotification);
		setCommands(commands);
		this.customName = customName;
		this.replace = replace;
		this.maxCaps = max;
	}

	public boolean isReplace() {
		return this.replace;
	}

	public long capsCount(String string) {
		return string.codePoints().filter(c -> c >= 'A' && c <= 'Z').count();
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String originalMessage, String lang) {
		if (isEnabled() && this.capsCount(originalMessage) > maxCaps) {
			boolean cancelled = false;

			if (isReplace()) {
				originalMessage = originalMessage.toLowerCase();
			} else {
				cancelled = true;
			}

			return new ChatEventResult(originalMessage, cancelled);
		}

		return null;
	}

	@Override
	public String getName() {
		return "Caps";
	}

	public String getCustomName() {
		return customName;
	}
}
