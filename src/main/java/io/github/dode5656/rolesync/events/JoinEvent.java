package io.github.dode5656.rolesync.events;

import io.github.dode5656.rolesync.RoleSync;
import io.github.dode5656.rolesync.utilities.Message;
import io.github.dode5656.rolesync.utilities.MessageManager;
import io.github.dode5656.rolesync.utilities.PluginStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class JoinEvent implements Listener {
    private final RoleSync plugin;

    public JoinEvent(RoleSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        if (plugin.getPluginStatus() == PluginStatus.DISABLED) return;
        JDA jda = plugin.getJDA();
        ProxiedPlayer player = e.getPlayer();
        Configuration playerCache = plugin.getPlayerCache().read();
        MessageManager messageManager = plugin.getMessageManager();

        if (playerCache == null) return;
        if (playerCache.contains("verified." + player.getUniqueId().toString())) {
            Guild guild = jda.getGuildById(plugin.getConfig().getString("server-id"));

            if (guild == null) {

                player.sendMessage(new TextComponent(messageManager.format(Message.ERROR)));
                plugin.getLogger().severe(Message.INVALID_SERVER_ID.getMessage());

                return;

            }

            Member member = guild.getMemberById(playerCache.getString("verified." + player.getUniqueId().toString()));

            if (member == null) return;

            List<Role> memberRoles = member.getRoles();

            Collection<String> roles = plugin.getConfig().getSection("roles").getKeys();
            Collection<Role> added = new ArrayList<>();
            Collection<Role> removed = new ArrayList<>();
            for (String role : roles) {
                String value = plugin.getConfig().getSection("roles").getString(role);
                Role roleAffected = guild.getRoleById(value);
                if (roleAffected == null) continue;
                if (player.hasPermission("rolesync.role." + role) && !memberRoles.contains(guild.getRoleById(value))) {
                    added.add(roleAffected);
                } else if (!player.hasPermission("rolesync.role." + role) && memberRoles.contains(guild.getRoleById(value))) {
                    removed.add(roleAffected);
                }

            }

            if (added.isEmpty() && removed.isEmpty()) return;

            guild.modifyMemberRoles(member, added, removed).queue();

            player.sendMessage(new TextComponent(messageManager.format(Message.UPDATED_ROLES)));
        }

    }

}
