package io.github.dode5656.rolesync.commands;


import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import io.github.dode5656.rolesync.RoleSync;
import io.github.dode5656.rolesync.utilities.Message;
import io.github.dode5656.rolesync.utilities.MessageManager;
import io.github.dode5656.rolesync.utilities.PluginStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class SyncCommand extends Command {
    private final RoleSync plugin;
    private EventWaiter waiter;
    private JDA jda;

    public SyncCommand(final RoleSync plugin) {
        super("sync");
        this.plugin = plugin;
        if (plugin.getPluginStatus() == PluginStatus.ENABLED) {
            this.waiter = new EventWaiter();
            this.jda = plugin.getJDA();
            this.jda.addEventListener(this.waiter);
        }
    }

    public void execute(final CommandSender sender, final String[] args) {
        boolean rgb = sender instanceof ProxiedPlayer && ((ProxiedPlayer) sender).getPendingConnection().getVersion() >= 735;
        MessageManager messageManager = plugin.getMessageManager();
        if (plugin.getPluginStatus() == PluginStatus.DISABLED) {
            sender.sendMessage(messageManager.formatBase(Message.PLUGIN_DISABLED, rgb));
            return;
        }
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(messageManager.formatBase(Message.PLAYER_ONLY, rgb));
            return;
        }

        if (!sender.hasPermission("rolesync.use")) {
            sender.sendMessage(messageManager.formatBase(Message.NO_PERM_CMD, rgb));
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 1) {
            sender.sendMessage(messageManager.formatBase(Message.USAGE, rgb));
            return;
        }

        final Guild guild = jda.getGuildById(plugin.getConfig().getString("server-id"));

        if (guild == null) {
            sender.sendMessage(messageManager.formatBase(Message.ERROR, rgb));
            plugin.getLogger().severe(Message.INVALID_SERVER_ID.getMessage());
            return;
        }

        Member member = null;
        try {
            member = guild.getMemberByTag(args[0]);
        } catch (Exception ignored) {
        }

        if (member == null) {
            boolean result = false;
            if (args[0].equals("id")) {
                Member idMember = guild.retrieveMemberById(args[1]).complete();
                if (idMember != null) {
                    member = idMember;
                    result = true;
                }
            }

            if (!result) {
                sender.sendMessage(messageManager.replacePlaceholders(messageManager.formatDiscord(Message.BAD_NAME),
                        args[0], sender.getName(), guild.getName(), rgb));
                return;
            }
        }

        if (plugin.getPlayerCache().read() != null && plugin.getPlayerCache().read().contains("verified." + player.getUniqueId().toString())) {
            List<Role> memberRoles = member.getRoles();

            if (!plugin.getPlayerCache().read().getString("verified." + player.getUniqueId().toString()).equals(member.getId())) {
                player.sendMessage(messageManager.replacePlaceholders(messageManager
                        .formatDiscord(Message.ALREADY_VERIFIED), member
                        .getUser().getAsTag(), sender.getName(), guild.getName(),rgb));
                return;
            }

            Collection<String> roles = plugin.getConfig().getSection("roles").getKeys();
            Collection<Role> added = new ArrayList<>();
            Collection<Role> removed = new ArrayList<>();
            plugin.getUtil().populateAddedRemoved(guild,roles,player,memberRoles,added,removed);

            String nickname = plugin.getConfig().getString("nickname-format").replaceAll("\\{ign}", player.getName());
            if (added.isEmpty() && removed.isEmpty() && member.getNickname() != null && member.getNickname().equals(nickname)) {
                player.sendMessage(messageManager.replacePlaceholders(
                        messageManager.formatDiscord(Message.ALREADY_VERIFIED),
                        member.getUser().getAsTag(), sender.getName(), guild.getName(), rgb));

                return;
            }

            if (!added.isEmpty() || !removed.isEmpty())
                if (!plugin.getUtil().modifyMemberRoles(guild,member,added,removed,player)) return;
            if (this.plugin.getConfig().getBoolean("change-nickname"))
                if (!plugin.getUtil().changeNickname(guild,member,player)) return;
            player.sendMessage(messageManager.formatBase(Message.UPDATED_ROLES, rgb));

            return;
        }

        if (plugin.getPlayerCache().read() != null) {
            Configuration verified = plugin.getPlayerCache().read().getSection("verified");
            if (verified != null && plugin.getUtil().sectionContainsValue(verified,member.getId())) {
                player.sendMessage(messageManager.replacePlaceholders(messageManager
                        .formatDiscord(Message.ALREADY_VERIFIED), member
                        .getUser().getAsTag(), sender.getName(), guild.getName(), rgb));
                return;
            }
        }

        final Member finalMember = member;
        member.getUser().openPrivateChannel().queue(privateChannel -> {

            privateChannel.sendMessage(messageManager.replacePlaceholdersDiscord(messageManager.formatDiscord(Message.VERIFY_REQUEST),
                    privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue(m->{
                player.sendMessage(messageManager.replacePlaceholders(messageManager.formatDiscord(Message.REQUEST_REPLY),
                        privateChannel.getUser().getAsTag(),sender.getName(),guild.getName(),rgb));
                waiter.waitForEvent(PrivateMessageReceivedEvent.class, event -> event.getChannel().getId()
                        .equals(privateChannel.getId()) &&
                        !event.getMessage().getAuthor().isBot(), event -> {

                    if (event.getMessage().getContentRaw().equalsIgnoreCase("yes")) {

                        plugin.getPlayerCache().reload(plugin);
                        Configuration playerCache = plugin.getPlayerCache().read();
                        playerCache.set("verified." + player.getUniqueId().toString(),
                                privateChannel.getUser().getId());
                        plugin.getPlayerCache().save(plugin);


                        Collection<String> roles = plugin.getConfig().getSection("roles").getKeys();
                        Collection<Role> added = new ArrayList<>();
                        for (String role : roles) {
                            String value = plugin.getConfig().getSection("roles").getString(role);
                            Role roleAffected = guild.getRoleById(value);
                            if (roleAffected == null) continue;

                            if (sender.hasPermission("rolesync.role." + role)) {
                                added.add(roleAffected);
                            }
                        }

                        if (!plugin.getUtil().modifyMemberRoles(guild,finalMember,added,null,player)) return;
                        if (plugin.getConfig().getBoolean("change-nickname"))
                            if (!plugin.getUtil().changeNickname(guild,finalMember,player)) return;

                        sender.sendMessage(messageManager.replacePlaceholders(messageManager.formatDiscord(Message.VERIFIED_MINECRAFT),
                                privateChannel.getUser().getAsTag(), sender.getName(), guild.getName(), rgb));

                        privateChannel.sendMessage(messageManager.replacePlaceholdersDiscord(
                                messageManager.formatDiscord(Message.VERIFIED_DISCORD),
                                privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();

                    } else if (event.getMessage().getContentRaw().equalsIgnoreCase("no")) {

                        event.getChannel().sendMessage(messageManager.replacePlaceholdersDiscord(
                                messageManager.formatDiscord(Message.DENIED_DISCORD),
                                privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();
                        sender.sendMessage(messageManager.replacePlaceholders(messageManager.formatDiscord(Message.DENIED_MINECRAFT),
                                privateChannel.getUser().getAsTag(), sender.getName(), guild.getName(), rgb));

                    }

                }, plugin.getConfig().getInt("verifyTimeout"), TimeUnit.MINUTES, () -> {

                    privateChannel.sendMessage(messageManager.replacePlaceholdersDiscord(
                            messageManager.formatDiscord(Message.TOO_LONG_DISCORD),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();

                    sender.sendMessage(messageManager.replacePlaceholders(messageManager.formatDiscord(Message.TOO_LONG_MC),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName(), rgb));

                });
            },e->player.sendMessage(messageManager.replacePlaceholders(messageManager.formatDiscord(Message.DM_FAILED),
                    finalMember.getUser().getAsTag(), sender.getName(), guild.getName(),rgb)));
        });
    }
}
