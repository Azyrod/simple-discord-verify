package com.azyrod.rpa_whitelist.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

public class ModConfig {
    public static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("rpa_whitelist.config").normalize();

    public static final String[] REQUIRED_PROPERTIES = { "discord_bot_token", "discord_server_id", "discord_role_id" };
    boolean incomplete = true;
    public ConfigValues values;

    public void load() throws IOException {
        load(false);
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public void load(boolean initialLoad) throws IOException {
        Properties properties = new Properties();

        try (FileInputStream f = new FileInputStream(CONFIG_FILE_PATH.toString())) {
            properties.load(f);
        } catch (NoSuchFileException | FileNotFoundException e) {
            if (!initialLoad) {
                throw e;
            }
            createDefaultConfig();
            load();
            return;
        }

        ObjectMapper m = new ObjectMapper();
        values = m.convertValue(properties, ConfigValues.class);
        incomplete = Arrays.stream(REQUIRED_PROPERTIES).anyMatch((property) -> properties.getProperty(property).isBlank());
    }

    // TODO: Replace this with a default Config file in Jar Resources (like LambsDynamicLights does) so we can have comments in it
    public void createDefaultConfig() throws IOException {
        Properties properties = new Properties();
        Arrays.stream(REQUIRED_PROPERTIES).forEach((property) -> properties.setProperty(property, ""));

        Files.createDirectories(CONFIG_FILE_PATH.getParent());
        properties.store(new FileOutputStream(CONFIG_FILE_PATH.toString()), null);
    }

    public record ConfigValues(String discord_bot_token, long discord_server_id, long discord_role_id) {
    }
}
