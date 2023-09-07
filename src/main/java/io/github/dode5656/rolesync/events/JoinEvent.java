package io.github.dode5656.rolesync.events;

import io.github.dode5656.rolesync.RoleSync;
import io.github.dode5656.rolesync.utilities.Message;
import io.github.dode5656.rolesync.utilities.MessageManager;
import io.github.dode5656.rolesync.utilities.PluginStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
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
        boolean rgb = e.getPlayer().getPendingConnection().getVersion() >= 735;
        if (plugin.getPluginStatus() == PluginStatus.DISABLED) return;
        JDA jda = plugin.getJDA();
        Configuration playerCache = plugin.getPlayerCache().read();
        MessageManager messageManager = plugin.getMessageManager();
        ProxiedPlayer player = e.getPlayer();

        if (playerCache == null) return;
        if (playerCache.contains("verified." + player.getUniqueId().toString())) {
            Guild guild = jda.getGuildById(plugin.getConfig().getString("server-id"));

            if (guild == null) {
                player.sendMessage(messageManager.formatBase(Message.ERROR, rgb));
                plugin.getLogger().severe(messageManager.format(Message.INVALID_SERVER_ID,rgb));
                return;
            }

            Member member = guild.retrieveMemberById(playerCache.getString("verified." + player.getUniqueId().toString())).complete();
            if (member == null) return;
            List<Role> memberRoles = member.getRoles();

            Collection<String> roles = plugin.getConfig().getSection("roles").getKeys();
            Collection<Role> added = new ArrayList<>();
            Collection<Role> removed = new ArrayList<>();
            plugin.getUtil().populateAddedRemoved(guild,roles,player,memberRoles,added,removed);

            boolean changed = false;
            if (!added.isEmpty() || !removed.isEmpty()) {
                if (!plugin.getUtil().modifyMemberRoles(guild, member, added, removed, player)) return;
                changed = true;
            }

            String nickname = plugin.getMessageManager().replacePlaceholdersDiscord(plugin.getConfig().getString("nickname-format")
                    .replaceAll("\\{ign}", player.getName()),member.getUser().getAsTag(),player.getName(),guild.getName());
            if (this.plugin.getConfig().getBoolean("change-nickname") && (member.getNickname() == null || !member.getNickname().equals(nickname))) {
                if (!plugin.getUtil().changeNickname(guild, member, player)) return;
                changed = true;
            }
            if (changed) player.sendMessage(messageManager.formatBase(Message.UPDATED_ROLES, rgb));
        }

    }

}
