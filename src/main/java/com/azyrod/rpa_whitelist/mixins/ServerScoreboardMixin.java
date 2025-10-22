package com.azyrod.rpa_whitelist.mixins;

import com.azyrod.rpa_whitelist.RPAWhitelist;
import com.google.common.collect.ImmutableBiMap;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.reactivestreams.Publisher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Mixin(ServerScoreboard.class)
public abstract class ServerScoreboardMixin extends Scoreboard {
    @Shadow
    @Final
    private MinecraftServer server;
    @Unique
    private static final ImmutableBiMap<String, Snowflake> team_role_map = new ImmutableBiMap.Builder<String, Snowflake>()
            .build();

    @Inject(method = "addScoreHolderToTeam(Ljava/lang/String;Lnet/minecraft/scoreboard/Team;)Z", at = @At("HEAD"))
    public void onAddScoreHolderToTeam(String scoreHolderName, Team new_team, CallbackInfoReturnable<Boolean> cir) {
        RPAWhitelist rpa = RPAWhitelist.INSTANCE;
        if (true) { // Disabling this Team logic
            return;
        }

        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(scoreHolderName);
        if (player == null) {
            RPAWhitelist.LOGGER.warn("Couldn't get player from scoreHolder '{}' - Not a Player ?", scoreHolderName);
            return;
        }
        Snowflake player_id = rpa.usercache.get(player.getUuid());
        if (player_id == null) {
            RPAWhitelist.LOGGER.error("Couldn't get Discord ID for Player '{}' - NOT SUPPOSED TO HAPPEN", player.getUuid());
            return;
        }

        Team previous_team = this.getScoreHolderTeam(scoreHolderName);
        Snowflake previous_role_id;
        Snowflake new_role_id = team_role_map.get(new_team.getName());
        if (previous_team != null) {
            previous_role_id = team_role_map.get(previous_team.getName());
        } else {
            previous_role_id = null;
        }

        rpa.guild.getMemberById(player_id).map((Member member) -> {
            Publisher<?> remove_old_role = previous_role_id == null ? Mono.empty() : member.removeRole(previous_role_id);
            Publisher<?> add_new_role = new_role_id == null ? Mono.empty() : member.addRole(new_role_id);

            return Mono.when(remove_old_role, add_new_role);
        }).subscribe();
    }

    @Inject(
            method = {"updateScore(Lnet/minecraft/scoreboard/ScoreHolder;Lnet/minecraft/scoreboard/ScoreboardObjective;Lnet/minecraft/scoreboard/ScoreboardScore;)V"},
            at = {@At("TAIL")}
    )
    public void onUpdateScore(ScoreHolder scoreHolder, ScoreboardObjective objective, ScoreboardScore score, CallbackInfo ci) {
        RPAWhitelist rpa = RPAWhitelist.INSTANCE;

        if (Objects.equals(scoreHolder.getNameForScoreboard(), "$lvl") && Objects.equals(objective.getName(), "warp_level")) {
            long channel_id = 1409502210160726058L;

            rpa.guild.getChannelById(Snowflake.of(channel_id)).ofType(MessageChannel.class).flatMap(
                    channel -> channel.createMessage(String.format("Warp Level is now %d", score.getScore()))
            ).subscribe();
        }
    }
}
