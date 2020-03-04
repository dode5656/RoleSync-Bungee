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

public class SyncCommand extends Command {
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
        MessageManager messageManager = plugin.getMessageManager();
        if (plugin.getPluginStatus() == PluginStatus.DISABLED) {
            sender.sendMessage(messageManager.format(Message.PLUGIN_DISABLED));
            return;
        }
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(messageManager.format(Message.PLAYER_ONLY));
            return;
        }

        if (!sender.hasPermission("rolesync.use")) {
            sender.sendMessage(messageManager.format(Message.NO_PERM_CMD));
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 1) {
            sender.sendMessage(messageManager.format("/<command> <discordname> - Don't forget the numbers after the #"));
            return;
        }

        final Guild guild = jda.getGuildById(plugin.getConfig().getString("server-id"));

        if (guild == null) {

            sender.sendMessage(messageManager.format(Message.ERROR));
            plugin.getLogger().severe(Message.INVALID_SERVER_ID.getMessage());
            return;

        }
        Member member = null;
        try {
            member = guild.getMemberByTag(args[0]);
        } catch (Exception ignored) {
        }
        if (member == null) {

            sender.sendMessage(messageManager.replacePlaceholders(Message.BAD_NAME.getMessage(),
                    args[0], sender.getName(), guild.getName()));

            return;
        }
        final Member finalMember = member;
        member.getUser().openPrivateChannel().queue(privateChannel -> {

            privateChannel.sendMessage(messageManager.replacePlaceholdersDiscord(messageManager.formatDiscord(Message.VERIFY_REQUEST),
                    privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();
            waiter.waitForEvent(PrivateMessageReceivedEvent.class, event -> event.getChannel().getId()
                    .equals(privateChannel.getId()) &&
                    !event.getMessage().getAuthor().isBot(), event -> {

                if (event.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                    if (plugin.getPlayerCache().read() != null && plugin.getPlayerCache().read().contains("verified." + player.getUniqueId().toString())) {
                        boolean result = false;
                        for ( String roleID : plugin.getConfig().getSection("roles").getKeys()) {
                            String value = plugin.getConfig().getSection("roles").getString(roleID);
                            Role role = guild.getRoleById( value);
                            if (role == null) {
                                continue;
                            }
                            if(finalMember.getRoles().contains(role)) {
                                result = true;
                            }
                        }
                        if (result) {
                            player.sendMessage(messageManager.replacePlaceholders(Message.ALREADY_VERIFIED.getMessage(),
                                    privateChannel.getUser().getAsTag(), sender.getName(), guild.getName()));

                            privateChannel.sendMessage(messageManager.replacePlaceholdersDiscord(
                                    messageManager.formatDiscord(Message.ALREADY_VERIFIED),
                                    privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();
                            return;
                        }

                    } else {
                        plugin.getPlayerCache().reload(plugin);
                        Configuration playerCache = plugin.getPlayerCache().read();
                        playerCache.set("verified." + player.getUniqueId().toString(),
                                privateChannel.getUser().getId());
                        plugin.getPlayerCache().save(plugin);

                    }

                    Collection<String> roles = plugin.getConfig().getSection("roles").getKeys();
                    List<String> roleIDs = new ArrayList<>();
                    for (String role : roles) {
                        String value = plugin.getConfig().getSection("roles").getString(role);
                        if (sender.hasPermission("rolesync.role." + role)) {
                            roleIDs.add(value);
                        }
                    }

                    for (String roleID : roleIDs) {
                        Role role = guild.getRoleById(roleID);
                        if (role == null) {
                            continue;
                        }
                        guild.addRoleToMember(finalMember, role).queue();
                    }

                    sender.sendMessage(messageManager.replacePlaceholders(Message.VERIFIED_MINECRAFT.getMessage(),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName()));

                    privateChannel.sendMessage(messageManager.replacePlaceholdersDiscord(
                            messageManager.formatDiscord(Message.VERIFIED_DISCORD),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();

                } else if (event.getMessage().getContentRaw().equalsIgnoreCase("no")) {

                    event.getChannel().sendMessage(messageManager.replacePlaceholdersDiscord(
                            messageManager.formatDiscord(Message.DENIED_DISCORD),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();
                    sender.sendMessage(messageManager.replacePlaceholders(Message.DENIED_MINECRAFT.getMessage(),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName()));

                }

            }, plugin.getConfig().getInt("verifyTimeout"), TimeUnit.MINUTES, () -> {

                privateChannel.sendMessage(messageManager.replacePlaceholdersDiscord(
                        messageManager.formatDiscord(Message.TOO_LONG_DISCORD),
                        privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();

                sender.sendMessage(messageManager.replacePlaceholders(Message.TOO_LONG_MC.getMessage(),
                        privateChannel.getUser().getAsTag(), sender.getName(), guild.getName()));

            });

        });

    }
}
