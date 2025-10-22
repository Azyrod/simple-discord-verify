package com.azyrod.rpa_whitelist.mixins;

import com.azyrod.rpa_whitelist.RPAWhitelist;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

// TODO: Replace with ResourceManagerHelper.addReloadListener

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "reloadResources(Ljava/util/Collection;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    public void reloadResources(Collection<String> dataPacks, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        RPAWhitelist.INSTANCE.refresh();
    }

    @Shadow
    public abstract GameRules getGameRules();

    @Inject(
            at = {@At("HEAD")},
            method = {"isPvpEnabled"},
            cancellable = true
    )
    private void isPvpEnabled(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue((this.getGameRules().get(RPAWhitelist.PVP)).get());
    }
}
