package io.github.dode5656.donorrole.commands;

import io.github.dode5656.donorrole.DonorRole;
import io.github.dode5656.donorrole.utilities.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.logging.Level;

public class ReloadCommand extends Command {
    private DonorRole plugin;

    public ReloadCommand(final DonorRole plugin) {
        super("donorreload", "", "dreload");
        this.plugin = plugin;
    }

    public void execute(final CommandSender commandSender, final String[] strings) {
        if (commandSender instanceof ProxiedPlayer && !commandSender.hasPermission("donorrole.reload"))
            commandSender.sendMessage(plugin.getMessageManager().format(Message.NOPERMCMD));
        try {
            plugin.getConfigStorage().reload(plugin);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error while trying to reload config" + e);
            commandSender.sendMessage(plugin.getMessageManager().format(Message.CONFIGRELOADERROR));
            return;
        }
        commandSender.sendMessage(plugin.getMessageManager().format(Message.CONFIGRELOADED));
    }
}
