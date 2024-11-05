package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.utils.PlaceholderUtil;
import me.micartey.webhookly.DiscordWebhook;
import me.micartey.webhookly.embeds.*;

import java.awt.*;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;

public class DiscordWebhookModule {
    private boolean enabled;
    private String webhookUrl;
    private String senderAvatarUrl;
    private String senderUsername;
    private String authorName;
    private String authorUrl;
    private String authorIconUrl;
    private String title;
    private String color;
    private String description;
    private String messageFieldName;
    private String serverFieldName;
    private String footerText;
    private String footerIconUrl;
    private String thumbnailUrl;
    private DiscordWebhook webhook;

    public void loadData(boolean enabled, String webhookUrl, String senderAvatarUrl, String senderUsername, String authorName, String authorUrl, String authorIconUrl, String title, String color, String description, String messageFieldName, String serverFieldName, String footerText, String footerIconUrl, String thumbnailUrl) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.senderAvatarUrl = senderAvatarUrl;
        this.senderUsername = senderUsername;
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        this.authorIconUrl = authorIconUrl;
        this.title = title;
        this.color = color;
        this.description = description;
        this.messageFieldName = messageFieldName;
        this.serverFieldName = serverFieldName;
        this.footerText = footerText;
        this.footerIconUrl = footerIconUrl;
        this.thumbnailUrl = thumbnailUrl;
        if (isEnabled()) setupDiscordWebhook();
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void setupDiscordWebhook() {
        this.webhook = new DiscordWebhook(webhookUrl);
        webhook.setAvatarUrl(senderAvatarUrl);
        webhook.setUsername(senderUsername);
    }

    public void dispatchWebhookNotification(ModerationModule moderationModule, String[][] placeholders) {
        if (!isEnabled() || !moderationModule.isWebhookEnabled() || moderationModule.getWarnNotification(placeholders) == null) {
            return;
        }

        EmbedObject embed = new EmbedObject()
                .setAuthor(new Author(authorName, authorUrl, authorIconUrl))
                .setTitle(title)
                .setColor(Color.decode(color))
                .setDescription(PlaceholderUtil.replacePlaceholders(description, placeholders))
                .setFooter(new Footer(footerText, footerIconUrl))
                .setThumbnail(new Thumbnail(thumbnailUrl))
                .setTimestamp(OffsetDateTime.now());

        embed.getFields().addAll(new LinkedHashSet<Field>(){{
            add(new Field(messageFieldName, PlaceholderUtil.replacePlaceholders("%message%", placeholders), true));
            add(new Field(serverFieldName, PlaceholderUtil.replacePlaceholders("%server_name%", placeholders), true));
        }});

        webhook.getEmbeds().add(embed);
        try {
            webhook.execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            webhook.getEmbeds().clear();
        }
    }
}
