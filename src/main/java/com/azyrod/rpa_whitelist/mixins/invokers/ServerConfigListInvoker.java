package com.azyrod.rpa_whitelist.mixins.invokers;

import net.minecraft.server.ServerConfigList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerConfigList.class)
public interface ServerConfigListInvoker {
    @Invoker
    boolean callContains(Object object);
}
