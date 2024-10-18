package dev._2lstudios.chatsentinel.shared.modules;

import java.util.regex.Pattern;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class FloodModerationModule extends ModerationModule {
	private String customName;
	private boolean replace;
	private Pattern pattern;

	public void loadData(boolean enabled, String customName, boolean replace, int maxWarns, String pattern,
			String warnNotification, String[] commands) {
		setEnabled(enabled);
		setMaxWarns(maxWarns);
		setWarnNotification(warnNotification);
		setCommands(commands);
		this.customName = customName;
		this.replace = replace;
		this.pattern = Pattern.compile(pattern);
	}

	public boolean isReplace() {
		return this.replace;
	}

	public String replace(String string) {
		return pattern.matcher(string).replaceAll("");
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String message, String lang) {
		if (isEnabled() && pattern.matcher(message).find()) {
			boolean cancelled = true;

			if (isReplace()) {
				String replacedString = replace(message);

				if (!replacedString.isEmpty()) {
					message = replacedString;
					cancelled = false;
				}
			}

			return new ChatEventResult(message, cancelled);
		}

		return null;
	}

	@Override
	public String getName() {
		return "Flood";
	}

	@Override
	public String getCustomName() {
		return customName;
	}
}
