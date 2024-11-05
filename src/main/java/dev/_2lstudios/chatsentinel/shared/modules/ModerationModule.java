package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.utils.PlaceholderUtil;

public abstract class ModerationModule {
	private boolean enabled = true;
    private int maxWarns = 0;
    private String warnNotification = null;
    private boolean webhookEnabled = true;
	private String[] commands = new String[0];

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isWebhookEnabled() {
        return webhookEnabled;
    }

    public int getMaxWarns() {
        return maxWarns;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setWebhookEnabled(boolean webhookEnabled) {
        this.webhookEnabled = webhookEnabled;
    }

    public void setMaxWarns(int maxWarns) {
        this.maxWarns = maxWarns;
    }

    public String getWarnNotification(String[][] placeholders) {
		if (!this.warnNotification.isEmpty()) {
			return PlaceholderUtil.replacePlaceholders(this.warnNotification, placeholders);
		}

        return null;
	}

    public void setWarnNotification(String warnNotification) {
        this.warnNotification = warnNotification;
    }

    public boolean hasExceededWarns(ChatPlayer chatPlayer) {
        return chatPlayer.getWarns(this) >= maxWarns && maxWarns > 0;
    }

    public abstract String getName();

    public abstract String getCustomName();

    public abstract ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName, String originalMessage, String lang);

    public String getBypassPermission() {
        return "chatsentinel.bypass." + getName();
    }

    public String[] getCommands(String[][] placeholders) {
		if (this.commands.length > 0) {
			String[] cmds = this.commands.clone();

			for (int i = 0; i < cmds.length; i++) {
				cmds[i] = PlaceholderUtil.replacePlaceholders(cmds[i], placeholders);
			}

			return cmds;
		} else
			return new String[0];
	}

    public void setCommands(String[] commands) {
        this.commands = commands;
    }
}