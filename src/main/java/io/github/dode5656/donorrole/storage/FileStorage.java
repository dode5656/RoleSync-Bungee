package io.github.dode5656.donorrole.storage;

import io.github.dode5656.donorrole.DonorRole;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileStorage {
    private File file;
    private Configuration fileStorage;

    public FileStorage(String name, File location) {
        this.file = new File(location, name);
        reload();
    }

    public final void save(DonorRole main) {
        Logger logger = main.getLogger();
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(fileStorage, file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save " + file.getName() + " file!", e);
        }
    }

    public final Configuration read() {
        return fileStorage;
    }

    public final void reload() {
        try {
            this.fileStorage = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final void saveDefaults(DonorRole main) {
        if (this.file.exists()) {
            reload();
            return;
        }
        if (!main.getDataFolder().exists())
            main.getDataFolder().mkdir();

        File file = new File(main.getDataFolder(), "config.yml");

        if (!file.exists()) {
            try (InputStream in = main.getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        reload();
    }

}