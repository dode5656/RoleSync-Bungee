package io.github.dode5656.rolesync.utilities;

import io.github.dode5656.rolesync.RoleSync;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public final class MessageManager {
    private final RoleSync plugin;

    public MessageManager(RoleSync plugin) {
        this.plugin = plugin;
    }

    public final BaseComponent[] color(String message) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message));
    }

    public final BaseComponent[] format(String msg) {
        return color(plugin.getConfig().getString(Message.PREFIX.getMessage())+msg);
    }

    public final BaseComponent[] formatBase(Message msg) {
        return format(plugin.getMessages().read().getString(msg.getMessage()));
    }

    public final String format(Message msg) {
        return TextComponent.toLegacyText(format(plugin.getMessages().read().getString(msg.getMessage())));
    }

    public final String formatDiscord(Message msg) { return plugin.getMessages().read().getString(msg.getMessage()); }

    public final BaseComponent[] replacePlaceholders(String msg, String discordTag, String playerName, String guildName) {
        return color(plugin.getConfig().getString(Message.PREFIX.getMessage())+msg
                .replaceAll("\\{discord_tag}", discordTag)
                .replaceAll("\\{player_name}", playerName)
                .replaceAll("\\{discord_server_name}", guildName));
    }

    public final String replacePlaceholdersDiscord(String msg, String discordTag, String playerName, String guildName) {
        return msg
                .replaceAll("\\{discord_tag}", discordTag)
                .replaceAll("\\{player_name}", playerName)
                .replaceAll("\\{discord_server_name}", guildName);
    }

    public final String defaultError(String value) {
        return plugin.getMessages().read().getString(Message.DEFAULT_VALUE.getMessage()).replaceAll("\\{value}", value);
    }
}
