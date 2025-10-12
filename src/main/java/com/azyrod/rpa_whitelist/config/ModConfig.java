package com.azyrod.rpa_whitelist.config;

import com.azyrod.rpa_whitelist.RPAWhitelist;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ModConfig {
    public static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("rpa_whitelist.yaml").normalize();

    boolean incomplete = true;
    public ConfigValues values;
    boolean initialLoad = true;

    public boolean isIncomplete() {
        return incomplete;
    }

    public void load() throws IOException {
        try (FileInputStream f = new FileInputStream(CONFIG_FILE_PATH.toString())) {
            ObjectMapper m = new ObjectMapper(new YAMLFactory());
            m.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            try {
                values = m.readValue(f, ConfigValues.class);

                checkIfRequiredFieldsAreNotNull(values);
                incomplete = false;
            } catch (JsonMappingException e) {
                RPAWhitelist.LOGGER.error("Failed to load config: ", e);
                incomplete = true;
            }
        } catch (NoSuchFileException | FileNotFoundException e) {
            if (!initialLoad) {
                throw e;
            }
            initialLoad = false;
            createDefaultConfig();
            load();
        }
    }

    public void createDefaultConfig() throws IOException {
        InputStream defaultConfigFile = RPAWhitelist.class.getResourceAsStream("/assets/config/default_config.yaml");

        if (defaultConfigFile == null) {
            throw new IllegalStateException("This distribution of RPAWhitelist is broken: cannot find the default configuration file inside of the mod's JAR.");
        }

        Files.createDirectories(CONFIG_FILE_PATH.getParent());
        Files.copy(defaultConfigFile, CONFIG_FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
    }

    private void checkIfRequiredFieldsAreNotNull(Object o) throws JsonMappingException {
        try {
            Arrays.stream(o.getClass().getDeclaredFields()).filter(field -> {
                JsonProperty annotation = field.getAnnotation(JsonProperty.class);

                return annotation != null && annotation.required();
            }).forEach(field -> {
                field.setAccessible(true);
                try {
                    Object value = field.get(o);
                    if (value == null) {
                        throw new JsonMappingException(null, "Required field '%s' cannot be null".formatted(field.getName()));
                    }

                    try {
                        if ((Boolean) value.getClass().getMethod("isEmpty").invoke(value)) {
                            throw new JsonMappingException(null, "Required field '%s' cannot be empty".formatted(field.getName()));
                        }
                    } catch (NoSuchMethodException e) {
                        // No isEmpty method -> pass
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }

                    if (!field.getType().isPrimitive()) {
                        checkIfRequiredFieldsAreNotNull(value);
                    }
                } catch (IllegalAccessException | JsonMappingException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof JsonMappingException cause) {
                throw cause;
            }
        }
    }

    public record ConfigValues(
            @JsonProperty(required = true) String discord_bot_token,
            @JsonProperty(required = true) Long discord_server_id,
            @JsonProperty(required = true) WhitelistConfig whitelist_config
    ) {
    }

    public record WhitelistConfig(
            @JsonProperty(required = true) ArrayList<Long> allowed_discord_roles,
            ArrayList<Long> disallowed_discord_roles
    ) {
        public WhitelistConfig {
            if (disallowed_discord_roles == null) {
                disallowed_discord_roles = new ArrayList<>();
            }
        }
    }
}
