package com.azyrod.rpa_whitelist.Discord;

import com.azyrod.rpa_whitelist.RPAWhitelist;
import com.azyrod.rpa_whitelist.config.ModConfig;
import discord4j.common.JacksonResources;
import discord4j.discordjson.json.*;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record CommandRegistrar(RestClient restClient) {
    // The name of the folder the commands json is in, inside our resources folder
    private static final String commandsFolderName = "discord_commands";

    public void registerCommands(ModConfig config) throws IOException {
        //Create an ObjectMapper that supports Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();
        final Mono<Long> applicationId = restClient.getApplicationId();
        final ApplicationService applicationService = restClient.getApplicationService();

        // Get our commands json from resources as command data
        List<ApplicationCommandRequest> commands = new ArrayList<>();
        for (String json : getCommandsJson()) {
            ApplicationCommandRequest request = d4jMapper.getObjectMapper().readValue(json, ApplicationCommandRequest.class);

            if (Objects.equals(request.name(), "rpa_verify")) {
                List<ApplicationCommandOptionData> options = request.options().get();
                ApplicationCommandOptionData mc_server_option = options.stream().filter(option -> option.name().equals("mc_server")).findFirst().orElseThrow();

                options.remove(mc_server_option);
                List<ApplicationCommandOptionChoiceData> choices = config.values.server_config().server_names().stream().map(name -> {
                    return (ApplicationCommandOptionChoiceData)ApplicationCommandOptionChoiceData.builder().name(name).value(name).build();
                }).toList();

                // Dynamically append choices
                mc_server_option = ImmutableApplicationCommandOptionData.builder().from(mc_server_option).choices(choices).build();
                options.addFirst(mc_server_option);
                request = ImmutableApplicationCommandRequest.builder().from(request).options(options).build();
            }
            commands.add(request);
        }

        applicationService.bulkOverwriteGuildApplicationCommand(applicationId.block(), config.values.discord_server_id(), commands)
                .doOnNext(cmd -> RPAWhitelist.LOGGER.debug("Successfully registered Global Command {}", cmd.name()))
                .doOnError(e -> RPAWhitelist.LOGGER.error("Failed to register global commands", e))
                .subscribe();
    }

    private static List<String> getCommandsJson() throws IOException {
        List<String> jsonCommands = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph().acceptPaths(commandsFolderName).scan()) {
            scanResult.getResourcesWithExtension("json").forEachByteArrayIgnoringIOException((Resource res, byte[] fileContent) -> {
                jsonCommands.add(new String(fileContent, StandardCharsets.UTF_8));
            });
        }
        return jsonCommands;
    }
}