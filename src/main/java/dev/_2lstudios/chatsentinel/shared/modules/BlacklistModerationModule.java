package dev._2lstudios.chatsentinel.shared.modules;

import java.util.regex.Pattern;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.utils.PatternUtil;

public class BlacklistModerationModule extends ModerationModule {
	private ModuleManager moduleManager;

	private String customName;

	private boolean fakeMessage;
  	private boolean blockRawMessage;
	private Pattern pattern;

	private boolean censorshipEnabled;
	private String censorshipReplacement;

	public BlacklistModerationModule(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
	}

	public void loadData(boolean enabled, String customName, boolean fakeMessage, boolean censorshipEnabled, String censorshipReplacement, int maxWarns,
        String warnNotification, boolean webhookEnabled, String[] commands, String[] patterns, boolean blockRawMessage) {
		setEnabled(enabled);
		setMaxWarns(maxWarns);
		setWarnNotification(warnNotification);
		setWebhookEnabled(webhookEnabled);
		setCommands(commands);
		this.customName = customName;
		this.fakeMessage = fakeMessage;
		this.censorshipEnabled = censorshipEnabled;
		this.censorshipReplacement = censorshipReplacement;
		this.pattern = PatternUtil.compile(patterns);
		this.blockRawMessage = blockRawMessage;
	}

	public boolean isFakeMessage() {
		return this.fakeMessage;
	}

	public boolean isCensorshipEnabled() {
		return censorshipEnabled;
	}

	public String getCensorshipReplacement() {
		return censorshipReplacement;
	}

	public boolean isBlockRawMessage() {
		return this.blockRawMessage;
	}

	public Pattern getPattern() {
		return pattern;
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String message, String lang) {
		if (!isEnabled()) {
			return null;
		}

		boolean cancelled = false;
		boolean hide = false;

		GeneralModule generalModule = moduleManager.getGeneralModule();
		WhitelistModule whitelistModule = moduleManager.getWhitelistModule();

		String sanitizedMessage = message;

		// Filter the arguments of the commands
		if (sanitizedMessage.startsWith("/") && message.contains(" ")) {
			sanitizedMessage = sanitizedMessage.substring(message.indexOf(" "));
		}

		// Remove weird stuff
		if (generalModule.isSanitizeEnabled()) {
			sanitizedMessage = generalModule.sanitize(message);
		}

		// Remove names
		if (generalModule.isSanitizeNames()) {
			sanitizedMessage = generalModule.sanitizeNames(message);
		}

		// Remove whitelisted stuff
		if (whitelistModule.isEnabled()) {
			sanitizedMessage = whitelistModule.getPattern().matcher(message).replaceAll("");
		}

		if (pattern.matcher(sanitizedMessage).find()) {
			if (isFakeMessage()) {
				hide = true;
			} else if (isCensorshipEnabled()) {
				message = pattern.matcher(message).replaceAll(getCensorshipReplacement());
			} else if (isBlockRawMessage()) {
				cancelled = true;
			}

			return new ChatEventResult(message, cancelled, hide);
		}

		return null;
	}

	@Override
	public String getName() {
		return "Blacklist";
	}

	@Override
	public String getCustomName() {
		return customName;
	}
}
