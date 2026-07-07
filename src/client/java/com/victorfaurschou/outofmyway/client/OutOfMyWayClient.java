package com.victorfaurschou.outofmyway.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.victorfaurschou.outofmyway.OutOfMyWay;
import com.victorfaurschou.outofmyway.OutOfMyWayConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class OutOfMyWayClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("out-of-my-way")
                        .then(ClientCommands.literal("config")
                                .executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    mc.execute(() -> mc.setScreen(ClothConfigScreen.create(null)));
                                    return 1;
                                }))
                        .then(ClientCommands.literal("enabled")
                                .then(ClientCommands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean value = BoolArgumentType.getBool(ctx, "value");
                                            OutOfMyWayConfig.enabled = value;
                                            OutOfMyWayConfig.save();
                                            ctx.getSource().sendFeedback(Component.literal(
                                                    "Out of My Way " + (value ? "enabled" : "disabled")));
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("version")
                                .executes(ctx -> {
                                    String version = FabricLoader.getInstance()
                                            .getModContainer(OutOfMyWay.MOD_ID)
                                            .map(c -> c.getMetadata().getVersion().getFriendlyString())
                                            .orElse("unknown");
                                    ctx.getSource().sendFeedback(Component.literal("Out of My Way " + version));
                                    return 1;
                                }))));
    }
}
