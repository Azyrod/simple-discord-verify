package com.azyrod.rpa_whitelist.config;

import com.azyrod.rpa_whitelist.RPAWhitelist;
import com.azyrod.rpa_whitelist.config.json.SnowflakeDeserializer;
import com.azyrod.rpa_whitelist.config.json.SnowflakeSerializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import discord4j.common.util.Snowflake;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class DiscordUserCache {
    public static final Path FILE_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("rpa_whitelist").resolve("discord_user_cache.json").normalize();

    private final BiMap<UUID, Snowflake> cache = HashBiMap.create();
    private final File file = new File(FILE_PATH.toString());
    private final ObjectMapper mapper;

    public DiscordUserCache() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Snowflake.class, new SnowflakeSerializer());
        module.addDeserializer(Snowflake.class, new SnowflakeDeserializer());
        mapper.registerModule(module);
    }

    public synchronized UUID getKey(Snowflake id) {
        return cache.inverse().get(id);
    }

    public synchronized Snowflake get(UUID uuid) {
        return cache.get(uuid);
    }

    public synchronized boolean remove(UUID uuid) {
        boolean result = cache.remove(uuid) != null;

        trySave();
        return result;
    }

    public synchronized boolean remove(Snowflake id) {
        boolean result = cache.inverse().remove(id) != null;

        trySave();
        return result;
    }

    public synchronized void put(UUID uuid, Snowflake discord_user) {
        cache.put(uuid, discord_user);
        trySave();
    }

    public synchronized void load() throws IOException {
        Files.createDirectories(FILE_PATH.getParent());

        if (file.createNewFile()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("{}");
            }
            cache.clear();
            return;
        }

        TypeReference<Map<UUID, Snowflake>> typeRef = new TypeReference<>(){};
        Map<UUID, Snowflake> map = mapper.readValue(file, typeRef);
        cache.clear();
        cache.putAll(map);
    }

    private void trySave() {
        try {
            save();
        } catch (IOException e) {
            RPAWhitelist.LOGGER.error("Failed to save Discord User cache", e);
        }
    }

    private void save() throws IOException {
        mapper.writeValue(file, cache);
    }
}
