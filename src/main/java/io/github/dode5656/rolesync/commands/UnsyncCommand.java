package io.github.dode5656.rolesync.commands;

import io.github.dode5656.rolesync.RoleSync;
import io.github.dode5656.rolesync.utilities.Message;
import io.github.dode5656.rolesync.utilities.PluginStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.Collection;

public final class UnsyncCommand extends Command {

    private final RoleSync plugin;
    private JDA jda;

    public UnsyncCommand(RoleSync plugin) {
        super("unsync");
        this.plugin = plugin;
        if (plugin.getPluginStatus() == PluginStatus.ENABLED) {
            this.jda = plugin.getJDA();
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean rgb = !sender.equals(ProxyServer.getInstance().getConsole()) && ((ProxiedPlayer) sender).getPendingConnection().getVersion() >= 735;

        if (!sender.hasPermission("rolesync.unsync")) {
            sender.sendMessage(plugin.getMessageManager().formatBase(Message.NO_PERM_CMD, rgb));
            return;
        }

        if (sender.hasPermission("rolesync.unsync.others")) {
            if (!(args.length >= 1)) {
                if (sender.equals(ProxyServer.getInstance().getConsole())) {
                    sender.sendMessage(plugin.getMessageManager().formatBase(Message.USAGE, rgb));
                    return;
                }
                unsync((ProxiedPlayer) sender, sender, rgb);
                return;
            }

            ProxiedPlayer player = null;

            for (ProxiedPlayer pPlayer : ProxyServer.getInstance().getPlayers()) {
                if (pPlayer.getName().equals(args[0])) {
                    player = pPlayer;
                    break;
                }
            }

            if (player == null) {
                sender.sendMessage(plugin.getMessageManager().formatBase(Message.PLAYER_NOT_FOUND, rgb));
                return;
            }

            unsync(player, sender, rgb);
        } else {
            unsync((ProxiedPlayer) sender, sender, rgb);
        }

    }

    private void unsync(ProxiedPlayer player, CommandSender sender, boolean rgb) {
        if (!(plugin.getPlayerCache().read() != null && plugin.getPlayerCache().read().contains("verified." + player.getUniqueId().toString()))) {
            sender.sendMessage(plugin.getMessageManager().formatBase(Message.NOT_SYNCED, rgb));
            return;
        }

        Guild guild = jda.getGuildById(plugin.getConfig().getString("server-id"));

        if (guild == null) {
            sender.sendMessage(plugin.getMessageManager().formatBase(Message.ERROR, rgb));
            plugin.getLogger().severe(plugin.getMessageManager().format(Message.INVALID_SERVER_ID,rgb));
            return;
        }

        Member member = guild.retrieveMemberById(plugin.getPlayerCache().read().getString("verified." + player.getUniqueId().toString())).complete();

        if (member == null) return;

        Collection<String> roles = plugin.getConfig().getSection("roles").getKeys();
        Collection<Role> removed = new ArrayList<>();
        for (String entry : roles) {
            String value = plugin.getConfig().getSection("roles").getString(entry);
            Role role = guild.getRoleById(value);
            if (role == null) continue;
            removed.add(role);
        }

        if(!plugin.getUtil().modifyMemberRoles(guild,member,null,removed,player)) return;
        if(!plugin.getUtil().changeNickname(guild,member,player,null)) return;

        plugin.getPlayerCache().read().set("verified."+player.getUniqueId().toString() ,null);

        plugin.getPlayerCache().save(plugin);
        plugin.getPlayerCache().reload(plugin);

        sender.sendMessage(plugin.getMessageManager().formatBase(Message.UNSYNCED_SUCCESSFULLY, rgb));

    }

}
