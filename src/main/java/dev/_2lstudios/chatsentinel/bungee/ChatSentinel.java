package dev._2lstudios.chatsentinel.bungee;

import java.util.concurrent.TimeUnit;

import dev._2lstudios.chatsentinel.bungee.commands.ChatSentinelCommand;
import dev._2lstudios.chatsentinel.bungee.listeners.ChatListener;
import dev._2lstudios.chatsentinel.bungee.listeners.PlayerDisconnectListener;
import dev._2lstudios.chatsentinel.bungee.listeners.PostLoginListener;
import dev._2lstudios.chatsentinel.bungee.modules.BungeeModuleManager;
import dev._2lstudios.chatsentinel.bungee.utils.ConfigUtil;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.modules.GeneralModule;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bukkit.Bukkit;

public class ChatSentinel extends Plugin {
	@Override
	public void onEnable() {
		ConfigUtil configUtil = new ConfigUtil(this);

		configUtil.create("%datafolder%/config.yml");
		configUtil.create("%datafolder%/messages.yml");
		configUtil.create("%datafolder%/whitelist.yml");
		configUtil.create("%datafolder%/blacklist.yml");

		ProxyServer server = getProxy();
		BungeeModuleManager moduleManager = new BungeeModuleManager(configUtil);
		GeneralModule generalModule = moduleManager.getGeneralModule();
		ChatPlayerManager chatPlayerManager = new ChatPlayerManager();
		PluginManager pluginManager = server.getPluginManager();

		pluginManager.registerListener(this, new ChatListener(this, moduleManager, chatPlayerManager));
		pluginManager.registerListener(this, new PlayerDisconnectListener(generalModule));
		pluginManager.registerListener(this, new PostLoginListener(generalModule, chatPlayerManager));

		pluginManager.registerCommand(this, new ChatSentinelCommand(chatPlayerManager, moduleManager, server));

		Bukkit.getAsyncScheduler().runAtFixedRate((org.bukkit.plugin.Plugin) this, task -> {
			if (generalModule.needsNicknameCompile()) {
				generalModule.compileNicknamesPattern();
			}
		}, 1000L, 1000L, TimeUnit.MILLISECONDS);
	}
}