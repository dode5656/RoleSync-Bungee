package io.github.dode5656.rolesync.utilities;

import io.github.dode5656.rolesync.RoleSync;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.config.Configuration;

public class MessageManager {
    private final BaseComponent[] prefix;
    private final Configuration messages;

    public MessageManager(RoleSync plugin) {
        messages = plugin.getMessages().read();
        prefix = color(plugin.getConfig().getString(Message.PREFIX.getMessage()) + " ");
    }

    public final BaseComponent[] color(String message) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message));
    }

    public final BaseComponent[] format(String msg) {
        ComponentBuilder componentBuilder = new ComponentBuilder(TextComponent.toLegacyText(prefix));
        componentBuilder.append(color(msg));
        return componentBuilder.create();
    }

    public final BaseComponent[] formatBase(Message msg) {
        return format(this.messages.getString(msg.getMessage()));
    }

    public final String format(Message msg) {
        return TextComponent.toLegacyText(format(this.messages.getString(msg.getMessage())));
    }

    public final String formatDiscord(Message msg) { return this.messages.getString(msg.getMessage()); }

    public final BaseComponent[] replacePlaceholders(String msg, String discordTag, String playerName, String guildName) {
        ComponentBuilder componentBuilder = new ComponentBuilder();
        componentBuilder.append(color(msg
                .replaceAll("\\{discord_tag}", discordTag)
                .replaceAll("\\{player_name}", playerName)
                .replaceAll("\\{discord_server_name}", guildName)));
        return componentBuilder.create();
    }

    public final String replacePlaceholdersDiscord(String msg, String discordTag, String playerName, String guildName) {
        return msg
                .replaceAll("\\{discord_tag}", discordTag)
                .replaceAll("\\{player_name}", playerName)
                .replaceAll("\\{discord_server_name}", guildName);
    }

    public final String defaultError(String value) {
        return this.messages.getString(Message.DEFAULT_VALUE.getMessage()).replaceAll("\\{value}", value);
    }
}
