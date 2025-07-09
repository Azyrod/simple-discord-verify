package com.azyrod.rpa_whitelist.Discord;

import com.azyrod.rpa_whitelist.RPAWhitelist;
import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.apache.commons.io.IOUtils;
import reactor.core.publisher.Mono;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommandRegistrar {
    private final RestClient restClient;

    // The name of the folder the commands json is in, inside our resources folder
    private static final String commandsFolderName = "discord_commands/";

    public CommandRegistrar(RestClient restClient) {
        this.restClient = restClient;
    }

    public void registerCommands(long guild_id) throws IOException {
        //Create an ObjectMapper that supports Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();
        final Mono<Long> applicationId = restClient.getApplicationId();
        final ApplicationService applicationService = restClient.getApplicationService();

        //Get our commands json from resources as command data
        List<ApplicationCommandRequest> commands = new ArrayList<>();
        for (String json : getCommandsJson()) {
            ApplicationCommandRequest request = d4jMapper.getObjectMapper().readValue(json, ApplicationCommandRequest.class);

            commands.add(request);
        }

        applicationService.bulkOverwriteGuildApplicationCommand(applicationId.block(), guild_id, commands)
                .doOnNext(cmd -> RPAWhitelist.LOGGER.debug("Successfully registered Global Command {}", cmd.name()))
                .doOnError(e -> RPAWhitelist.LOGGER.error("Failed to register global commands", e))
                .subscribe();
    }

    private static List<String> getCommandsJson() throws IOException {
        List<String> jsonCommands = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph().acceptPaths("discord_commands").scan()) {
            scanResult.getResourcesWithExtension("json").forEachByteArrayIgnoringIOException((Resource res, byte[] fileContent) -> {
                jsonCommands.add(new String(fileContent, StandardCharsets.UTF_8));
            });
        }
        return jsonCommands;
    }
}