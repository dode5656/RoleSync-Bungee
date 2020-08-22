package io.github.dode5656.rolesync.utilities;

import io.github.dode5656.rolesync.RoleSync;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageManager {
    private final RoleSync plugin;

    public MessageManager(RoleSync plugin) {
        this.plugin = plugin;
    }

    public final BaseComponent[] color(String message, boolean rgb) {
        if (rgb) {
            return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                    convertHexToColor(message)));
        }
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',message));
    }

    public final BaseComponent[] format(String msg, boolean rgb) {
        return color(plugin.getConfig().getString(Message.PREFIX.getMessage())+msg, rgb);
    }

    public final BaseComponent[] formatBase(Message msg, boolean rgb) {
        return format(plugin.getMessages().read().getString(msg.getMessage()), rgb);
    }

    public final String format(Message msg, boolean rgb) {
        return TextComponent.toLegacyText(format(plugin.getMessages().read().getString(msg.getMessage()), rgb));
    }

    public final String formatDiscord(Message msg) { return plugin.getMessages().read().getString(msg.getMessage()); }

    public final BaseComponent[] replacePlaceholders(String msg, String discordTag, String playerName, String guildName, boolean rgb) {
        return color(plugin.getConfig().getString(Message.PREFIX.getMessage())+msg
                .replaceAll("\\{discord_tag}", discordTag)
                .replaceAll("\\{player_name}", playerName)
                .replaceAll("\\{discord_server_name}", guildName), rgb);
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

    String convertHexToColor(String msg) {
        Pattern p = Pattern.compile("&x[a-f0-9A-F]{6}");
        Matcher m = p.matcher(msg);
        String s = msg;
        while (m.find()) {
            String hexString = ChatColor.of('#' + m.group().substring(2)).toString();
            s = s.replace(m.group(), hexString);
        }
        return s.replaceAll("§", "&");
    }

}
