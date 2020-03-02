package io.github.dode5656.rolesync.commands;

import io.github.dode5656.rolesync.RoleSync;
import io.github.dode5656.rolesync.utilities.Message;
import io.github.dode5656.rolesync.utilities.PluginStatus;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.logging.Level;

public class ReloadCommand extends Command {
    private RoleSync plugin;

    public ReloadCommand(final RoleSync plugin) {
        super("rolesyncreload", "", "rreload", "sreload", "syncreload");
        this.plugin = plugin;
    }

    public void execute(final CommandSender commandSender, final String[] strings) {
        if (commandSender instanceof ProxiedPlayer && !commandSender.hasPermission("rolesync.reload")) {
            commandSender.sendMessage(plugin.getMessageManager().format(Message.NO_PERM_CMD));
            return;
        }
        try {
            plugin.getConfigStorage().reload(plugin);
            if (plugin.getPluginStatus() == PluginStatus.DISABLED) plugin.setPluginStatus(PluginStatus.ENABLED);
            plugin.getConfigChecker().checkDefaults();
            plugin.startBot();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error while trying to reload config" + e);
            commandSender.sendMessage(plugin.getMessageManager().format(Message.CONFIG_RELOAD_ERROR));
            return;
        }
        commandSender.sendMessage(plugin.getMessageManager().format(Message.CONFIG_RELOADED));
    }
}
