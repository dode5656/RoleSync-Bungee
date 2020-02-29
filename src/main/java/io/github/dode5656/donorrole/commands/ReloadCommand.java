package io.github.dode5656.donorrole.commands;

import io.github.dode5656.donorrole.DonorRole;
import io.github.dode5656.donorrole.utilities.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.logging.Level;

public class ReloadCommand extends Command {
    private DonorRole plugin;

    public ReloadCommand(final DonorRole plugin) {
        super("donorreload", "donorrole.reload", "dreload");
        this.plugin = plugin;
    }

    public void execute(final CommandSender commandSender, final String[] strings) {
        try {
            plugin.getConfigStorage().reload();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error while trying to reload config" + e);
            commandSender.sendMessage(new TextComponent(plugin.getMessageManager().format(Message.CONFIGRELOADERROR)));
            return;
        }
        commandSender.sendMessage(new TextComponent(plugin.getMessageManager().format(Message.CONFIGRELOADED)));
    }
}
