package dev._2lstudios.chatsentinel.velocity.modules;

import dev._2lstudios.chatsentinel.velocity.utils.ConfigUtil;
import dev._2lstudios.chatsentinel.shared.modules.ModuleManager;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class VelocityModuleManager extends ModuleManager {
	private final ConfigUtil configUtil;

	public VelocityModuleManager(ConfigUtil configUtil) {
		this.configUtil = configUtil;
		reloadData();
	}

	@Override
	public void reloadData() {
		configUtil.create("config.yml");
		configUtil.create("messages.yml");
		configUtil.create("blacklist.yml");
		configUtil.create("whitelist.yml");

		CommentedConfigurationNode blacklistYml = configUtil.get("blacklist.yml");
		CommentedConfigurationNode configYml = configUtil.get("config.yml");
		CommentedConfigurationNode messagesYml = configUtil.get("messages.yml");
		CommentedConfigurationNode whitelistYml = configUtil.get("whitelist.yml");
		Map<String, Map<String, String>> locales = new HashMap<>();

		for (Object lang : messagesYml.node("langs").childrenMap().keySet()) {
			ConfigurationNode langSection = messagesYml.node("langs", lang);
			Map<String, String> messages = new HashMap<>();

			for (Object key : langSection.childrenMap().keySet()) {
				String value = langSection.node(key).getString();

				messages.put((String) key, value);
			}

			locales.put((String) lang, messages);
		}

		getCapsModule().loadData(configYml.node("caps", "enabled").getBoolean(),
				configYml.node("caps", "custom-module-name").getString(),
				configYml.node("caps", "replace").getBoolean(),
				configYml.node("caps", "max").getInt(), configYml.node("caps", "warn", "max").getInt(),
				configYml.node("caps", "warn", "notification").getString(),
				configYml.node("caps", "warn", "webhook-notification").getBoolean(),
				configYml.node("caps", "punishments").childrenList().stream()
						.map(ConfigurationNode::getString)
						.toArray(String[]::new));
		getCooldownModule().loadData(configYml.node("cooldown", "enabled").getBoolean(),
				configYml.node("cooldown", "time", "repeat-global").getInt(),
				configYml.node("cooldown", "time", "repeat").getInt(),
				configYml.node("cooldown", "time", "normal").getInt(),
				configYml.node("cooldown", "time", "command").getInt());
		getFloodModule().loadData(configYml.node("flood", "enabled").getBoolean(),
				configYml.node("flood", "custom-module-name").getString(),
				configYml.node("flood", "replace").getBoolean(),
				configYml.node("flood", "warn", "max").getInt(), configYml.node("flood", "pattern").getString(),
				configYml.node("flood", "warn", "notification").getString(),
				configYml.node("flood", "warn", "webhook-notification").getBoolean(),
				configYml.node("flood", "punishments").childrenList().stream()
						.map(ConfigurationNode::getString)
						.toArray(String[]::new));
		getMessagesModule().loadData(messagesYml.node("default").getString(), locales);
		getGeneralModule().loadData(configYml.node("general", "sanitize").getBoolean(true),
				configYml.node("general", "sanitize-names").getBoolean(true),
				configYml.node("general", "filter-other").getBoolean(false),
				configYml.node("general", "commands").childrenList().stream()
						.map(ConfigurationNode::getString)
						.collect(Collectors.toList()));
		getWhitelistModule().loadData(configYml.node("whitelist", "enabled").getBoolean(),
				whitelistYml.node("servers").childrenList().stream()
						.map(ConfigurationNode::getString)
						.collect(Collectors.toList()),
				whitelistYml.node("expressions").childrenList().stream()
						.map(ConfigurationNode::getString)
						.toArray(String[]::new));
		boolean censorshipEnabled = configYml.node("blacklist", "censorship", "enabled").getBoolean(false);
		String censorshipReplacement = configYml.node("blacklist", "censorship", "replacement").getString("***");
		getBlacklistModule().loadData(configYml.node("blacklist", "enabled").getBoolean(),
				configYml.node("blacklist", "custom-module-name").getString(),
				configYml.node("blacklist", "fake_message").getBoolean(),
				censorshipEnabled,
				censorshipReplacement,
				configYml.node("blacklist", "warn", "max").getInt(),
				configYml.node("blacklist", "warn", "notification").getString(),
				configYml.node("blacklist", "warn", "webhook-notification").getBoolean(),
				configYml.node("blacklist", "punishments").childrenList().stream()
						.map(ConfigurationNode::getString)
						.toArray(String[]::new),
				blacklistYml.node("expressions").childrenList().stream()
						.map(ConfigurationNode::getString)
						.toArray(String[]::new),
				configYml.node("blacklist", "block_raw_message").getBoolean());
		getSyntaxModule().loadData(configYml.node("syntax", "enabled").getBoolean(),
				configYml.node("syntax", "custom-module-name").getString(),
				configYml.node("syntax", "warn", "max").getInt(),
				configYml.node("syntax", "warn", "notification").getString(),
				configYml.node("syntax", "warn", "webhook-notification").getBoolean(),
				configYml.node("syntax", "whitelist").childrenList().stream()
						.map(ConfigurationNode::getString)
						.toArray(String[]::new),
				configYml.node("syntax", "punishments").childrenList().stream()
						.map(ConfigurationNode::getString)
						.toArray(String[]::new));
		getDiscordWebhookModule().loadData(configYml.node("discord-webhook", "enabled").getBoolean(),
				configYml.node("discord-webhook", "webhook-url").getString(),
				configYml.node("discord-webhook", "sender", "avatar-url").getString(),
				configYml.node("discord-webhook", "sender", "username").getString(),
				configYml.node("discord-webhook", "author", "name").getString(),
				configYml.node("discord-webhook", "author", "url").getString(),
				configYml.node("discord-webhook", "author", "icon-url").getString(),
				configYml.node("discord-webhook", "title").getString(),
				configYml.node("discord-webhook", "color").getString(),
				configYml.node("discord-webhook", "description").getString(),
				configYml.node("discord-webhook", "field-names", "message").getString(),
				configYml.node("discord-webhook", "field-names", "server").getString(),
				configYml.node("discord-webhook", "footer", "text").getString(),
				configYml.node("discord-webhook", "footer", "icon-url").getString(),
				configYml.node("discord-webhook", "thumbnail-url").getString());
	}
}
