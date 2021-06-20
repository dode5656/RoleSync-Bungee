package io.github.dode5656.rolesync.utilities;

import io.github.dode5656.rolesync.RoleSync;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public final class Util {
    private final RoleSync plugin;
    public Util(RoleSync plugin) { this.plugin = plugin; }

    public boolean modifyMemberRoles(Guild guild, Member member, Collection<Role> added, Collection<Role> removed, ProxiedPlayer player) {
        boolean rgb = player.getPendingConnection().getVersion() >= 735;
        try {
            guild.modifyMemberRoles(member, added, removed).queue();
        } catch (InsufficientPermissionException | HierarchyException e) {
            player.sendMessage(plugin.getMessageManager().formatBase(Message.ERROR, rgb));
            if (e instanceof InsufficientPermissionException) {
                plugin.getLogger().log(Level.SEVERE, "Bot has insufficient permissions. Cannot manage roles.");
                return false;
            }
            player.sendMessage(plugin.getMessageManager().formatBase(Message.HIERARCHY_ERROR, rgb));
            plugin.getLogger().log(Level.SEVERE,
                    plugin.getMessageManager().format(plugin.getMessageManager().replacePlaceholders("Failed to apply role changes to {discord_tag} with IGN {player_name} due to Hierarchy error. Some of the specified roles are higher than the bot's role.",
                            member.getUser().getAsTag(),player.getName(), guild.getName(), rgb)));
            return false;
        }
        return true;
    }

    public boolean changeNickname(Guild guild, Member member, ProxiedPlayer player) {
        return changeNickname(guild,member,player,plugin.getConfig().getString("nickname-format")
                .replaceAll("\\{ign}", player.getName()));
    }

    public boolean changeNickname(Guild guild, Member member, ProxiedPlayer player, String nickname) {
        boolean rgb = player.getPendingConnection().getVersion() >= 735;
        try {
            member.modifyNickname(nickname).queue();
        } catch (InsufficientPermissionException | HierarchyException e) {
            player.sendMessage(plugin.getMessageManager().formatBase(Message.ERROR, rgb));
            if (e instanceof InsufficientPermissionException) {
                plugin.getLogger().log(Level.SEVERE, "Bot has insufficient permissions. Cannot give nicknames to people.");
                return false;
            }
            player.sendMessage(plugin.getMessageManager().formatBase(Message.HIERARCHY_ERROR, rgb));
            plugin.getLogger().log(Level.SEVERE,
                    plugin.getMessageManager().format(plugin.getMessageManager().replacePlaceholders("Failed to change nickname of {discord_tag} with IGN {player_name} due to Hierarchy error. Their role is higher than the bot's role.",
                            member.getUser().getAsTag(),player.getName(), guild.getName(), rgb)));
            return false;
        }
        return true;
    }

    public void populateAddedRemoved(Guild guild, Collection<String> roles, ProxiedPlayer player, List<Role> memberRoles, Collection<Role> added, Collection<Role> removed) {
        for (String role : roles) {
            String value = plugin.getConfig().getSection("roles").getString(role);
            Role roleAffected = guild.getRoleById(value);
            if (roleAffected == null) continue;
            if (player.hasPermission("rolesync.role." + role) && !memberRoles.contains(roleAffected)) {
                added.add(roleAffected);
            } else if (removed != null && !player.hasPermission("rolesync.role." + role) && memberRoles.contains(roleAffected)) {
                removed.add(roleAffected);
            }
        }
    }

    public List<Object> getSectionValues(Configuration section) {
        List<Object> values = new ArrayList<>();
        for (String key : section.getKeys()) {
            Object value = section.get(key);
            values.add(value);
        }
        return values;
    }

    public boolean sectionContainsValue(Configuration section, String value) {
        return getSectionValues(section).contains(value);
    }
}