package dev._2lstudios.chatsentinel.shared.modules;

public abstract class ModuleManager {
	private CapsModerationModule capsModule;
	private CooldownModerationModule cooldownModule;
	private FloodModerationModule floodModule;
	private MessagesModule messagesModule;
	private GeneralModule generalModule;
	private BlacklistModerationModule blacklistModule;
	private SyntaxModerationModule syntaxModule;
	private WhitelistModule whitelistModule;

	private DiscordWebhookModule discordWebhookModule;

	public ModuleManager() {
		this.capsModule = new CapsModerationModule();
		this.cooldownModule = new CooldownModerationModule();
		this.floodModule = new FloodModerationModule();
		this.blacklistModule = new BlacklistModerationModule(this);
		this.syntaxModule = new SyntaxModerationModule();
		this.messagesModule = new MessagesModule();
		this.generalModule = new GeneralModule();
		this.whitelistModule = new WhitelistModule();
		this.discordWebhookModule = new DiscordWebhookModule();
	}

	public CooldownModerationModule getCooldownModule() {
		return cooldownModule;
	}

	public CapsModerationModule getCapsModule() {
		return capsModule;
	}

	public FloodModerationModule getFloodModule() {
		return floodModule;
	}

	public BlacklistModerationModule getBlacklistModule() {
		return blacklistModule;
	}

	public SyntaxModerationModule getSyntaxModule() {
		return syntaxModule;
	}

	public MessagesModule getMessagesModule() {
		return messagesModule;
	}

	public GeneralModule getGeneralModule() {
		return generalModule;
	}

	public WhitelistModule getWhitelistModule() {
		return whitelistModule;
	}

	public DiscordWebhookModule getDiscordWebhookModule() {
		return discordWebhookModule;
	}

	public abstract void reloadData();
}
