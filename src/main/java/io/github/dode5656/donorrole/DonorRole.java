package io.github.dode5656.donorrole;

import io.github.dode5656.donorrole.commands.DonorCommand;
import io.github.dode5656.donorrole.commands.ReloadCommand;
import io.github.dode5656.donorrole.events.JoinEvent;
import io.github.dode5656.donorrole.storage.FileStorage;
import io.github.dode5656.donorrole.utilities.MessageManager;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.logging.Level;

public class DonorRole extends Plugin {

    private FileStorage playerCache;
    private FileStorage messages;
    private FileStorage config;
    private MessageManager messageManager;
    private JDA jda;

    @Override
    public void onEnable() {

        config = new FileStorage("config.yml", new File(getDataFolder().getPath()));
        config.saveDefaults(this);

        playerCache = new FileStorage("playerCache.yml", new File(getDataFolder().getPath(), "cache"));

        messages = new FileStorage("messages.yml", new File(getDataFolder().getPath()));
        messages.saveDefaults(this);
        messageManager = new MessageManager(this);

        if (getConfig().getString("bot-token").equals("REPLACEBOTTOKEN")) {

            getLogger().severe(messageManager.defaultError("Bot Token"));
            disablePlugin();
            return;

        } else if (getConfig().getString("server-id").equals("REPLACESERVERID")) {

            getLogger().severe(messageManager.defaultError("Server ID"));
            disablePlugin();
            return;

        } else if (getConfig().getSection("roles").contains("REPLACEROLEID")) {
            getLogger().severe(messageManager.defaultError("a Role ID"));
            disablePlugin();
            return;
        }

        if (!startBot()) { return; }

        getProxy().getPluginManager().registerCommand(this, new DonorCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand(this));
        getProxy().getPluginManager().registerListener(this, new JoinEvent(this));

    }

    @Override
    public void onDisable() {
        if (jda != null && jda.getStatus() == JDA.Status.CONNECTED) jda.shutdown();
    }

    public final FileStorage getMessages() {
        return messages;
    }

    public final FileStorage getPlayerCache() {
        return playerCache;
    }

    public final MessageManager getMessageManager() {
        return messageManager;
    }

    public Configuration getConfig() {
        return config.read();
    }

    public FileStorage getConfigStorage() {
        return config;
    }

    public final JDA getJDA() {
        return jda;
    }

    private boolean startBot() {
        try {
            this.jda = new JDABuilder(AccountType.BOT).setToken(getConfig().getString("bot-token")).build();

            return true;
        } catch (LoginException e) {
            getLogger().log(Level.SEVERE, "Error when logging in!");
            disablePlugin();
        }

        return false;
    }

    private void disablePlugin() {
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().getPlugin(getClass().getSimpleName()).onDisable();
        getProxy().getScheduler().cancel(this);
    }
}
