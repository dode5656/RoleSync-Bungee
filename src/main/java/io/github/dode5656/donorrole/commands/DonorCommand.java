package io.github.dode5656.donorrole.commands;


import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import io.github.dode5656.donorrole.DonorRole;
import io.github.dode5656.donorrole.utilities.Message;
import io.github.dode5656.donorrole.utilities.MessageManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DonorCommand extends Command {
    private DonorRole plugin;
    private EventWaiter waiter;
    private JDA jda;

    public DonorCommand(final DonorRole plugin) {
        super("donor", "donorrole.use");
        this.waiter = new EventWaiter();
        this.plugin = plugin;
        this.jda = plugin.getJDA();
        this.jda.addEventListener(this.waiter);
    }

    public void execute(final CommandSender sender, final String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(messageManager.format(Message.PLAYERONLY)));
            return;
        }

        if (!sender.hasPermission("donorrole.use")) {
            sender.sendMessage(new TextComponent(messageManager.format(Message.NOPERMCMD)));
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 1) {
            sender.sendMessage(new TextComponent(messageManager.format("/<command> <discordname> - Don't forget the numbers after the #")));
            return;
        }

        final Guild guild = jda.getGuildById(plugin.getConfig().getString("server-id"));

        if (guild == null) {

            sender.sendMessage(new TextComponent(messageManager.format(Message.ERROR)));
            plugin.getLogger().severe(Message.INVALIDSERVERID.getMessage());
            return;

        }
        Member member = null;
        try {
            member = guild.getMemberByTag(args[0]);
        } catch (Exception ignored) {
        }
        if (member == null) {

            sender.sendMessage(new TextComponent(messageManager.replacePlaceholders(
                    messageManager.format(Message.BADNAME),
                    args[0], sender.getName(), guild.getName())));

            return;
        }
        final Member finalMember = member;
        member.getUser().openPrivateChannel().queue(privateChannel -> {

            privateChannel.sendMessage(messageManager.replacePlaceholders(messageManager.formatDiscord(Message.VERIFYREQUEST),
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
                            player.sendMessage(new TextComponent(messageManager.replacePlaceholders(
                                    messageManager.format(Message.ALREADYVERIFIED),
                                    privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())));

                            privateChannel.sendMessage(messageManager.replacePlaceholders(
                                    messageManager.formatDiscord(Message.ALREADYVERIFIED),
                                    privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();
                            return;
                        }

                    } else {
                        Configuration playerCache = plugin.getPlayerCache().read();
                        if (playerCache != null) {
                            playerCache.set("verified." + player.getUniqueId().toString(),
                                    privateChannel.getUser().getId());
                        }

                        plugin.getPlayerCache().save(plugin);
                        plugin.getPlayerCache().reload();

                    }

                    Collection<String> roles = plugin.getConfig().getSection("roles").getKeys();
                    List<String> roleIDs = new ArrayList<>();
                    for (String role : roles) {
                        String value = plugin.getConfig().getSection("roles").getString(role);
                        if (sender.hasPermission("donorrole.role." + role)) {
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

                    sender.sendMessage(new TextComponent(messageManager.replacePlaceholders(
                            messageManager.format(Message.VERIFIEDMINECRAFT),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())));

                    privateChannel.sendMessage(messageManager.replacePlaceholders(
                            messageManager.formatDiscord(Message.VERIFIEDDISCORD),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();

                } else if (event.getMessage().getContentRaw().equalsIgnoreCase("no")) {

                    event.getChannel().sendMessage(messageManager.replacePlaceholders(
                            messageManager.formatDiscord(Message.DENIEDDISCORD),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();
                    sender.sendMessage(new TextComponent(messageManager.replacePlaceholders(
                            messageManager.format(Message.DENIEDMINECRAFT),
                            privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())));

                }

            }, plugin.getConfig().getInt("verifyTimeout"), TimeUnit.MINUTES, () -> {

                privateChannel.sendMessage(messageManager.replacePlaceholders(
                        messageManager.formatDiscord(Message.TOOLONGDISCORD),
                        privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())).queue();

                sender.sendMessage(new TextComponent(messageManager.replacePlaceholders(
                        messageManager.format(Message.TOOLONGMC),
                        privateChannel.getUser().getAsTag(), sender.getName(), guild.getName())));

            });

        });

    }
}
