package io.github.dode5656.rolesync.storage;

import io.github.dode5656.rolesync.RoleSync;
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
    private final File file;
    private Configuration fileStorage;

    public FileStorage(String name, File location) {
        this.file = new File(location, name);
    }

    public final void save(RoleSync main) {
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

    public final void reload(RoleSync main) {
        Logger logger = main.getLogger();
        try {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdir();
            if (!file.exists())
                file.createNewFile();
            this.fileStorage = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not reload " + file.getName() + " file!", e);
        }
    }

    public final void saveDefaults(RoleSync main) {
        if (this.file.exists()) {
            reload(main);
            return;
            }
            if (!file.getParentFile().exists())
                file.getParentFile().mkdir();

        if (!file.exists()) {
            try (InputStream in = main.getResourceAsStream(file.getName())) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        reload(main);
    }

}