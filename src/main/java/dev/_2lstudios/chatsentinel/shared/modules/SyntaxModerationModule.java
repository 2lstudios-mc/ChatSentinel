package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class SyntaxModerationModule extends ModerationModule {
	private String customName;
	private String[] whitelist;

	public void loadData(boolean enabled, String customName, int maxWarns, String warnNotification,
			String[] whitelist, String[] commands) {
		setEnabled(enabled);
		setMaxWarns(maxWarns);
		setWarnNotification(warnNotification);
		setCommands(commands);
		this.customName = customName;
		this.whitelist = whitelist;
	}

	public boolean isWhitelisted(String message) {
		if (whitelist.length > 0)
			for (String string : whitelist)
				if (message.startsWith(string))
					return true;

		return false;
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String message, String lang) {
		if (isEnabled() && !isWhitelisted(message) && hasSyntax(message)) {
			return new ChatEventResult(message, true);
		}

		return null;
	}

	@Override
	public String getName() {
		return "Syntax";
	}

	@Override
	public String getCustomName() {
		return customName;
	}

	private boolean hasSyntax(String message) {
		if (message.startsWith("/")) {
			String command;

			if (message.contains(" ")) {
				command = message.split(" ")[0];
			} else {
				command = message;
			}

			String[] syntax = command.split(":");

			if (syntax.length > 1) {
				return true;
			}
		}

		return false;
	}
}
