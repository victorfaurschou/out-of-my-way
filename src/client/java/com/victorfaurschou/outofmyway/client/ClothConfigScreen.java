package com.victorfaurschou.outofmyway.client;

import com.victorfaurschou.outofmyway.OutOfMyWayConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ClothConfigScreen {

    private static Component tip(String key) {
        return Component.translatable("config.out-of-my-way.tooltip." + key);
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.out-of-my-way.title"))
                .setSavingRunnable(OutOfMyWayConfig::save);

        ConfigEntryBuilder e = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("config.out-of-my-way.category.general"));

        general.addEntry(e.startIntSlider(
                        Component.translatable("config.out-of-my-way.min_distance"),
                        OutOfMyWayConfig.minDistance, 1, 8)
                .setDefaultValue(3)
                .setTextGetter(v -> Component.literal(v + " blocks"))
                .setTooltip(tip("min_distance"))
                .setSaveConsumer(v -> OutOfMyWayConfig.minDistance = v)
                .build());

        BooleanListEntry inViewEntry = e.startBooleanToggle(
                        Component.translatable("config.out-of-my-way.in_view"),
                        OutOfMyWayConfig.inView)
                .setDefaultValue(true)
                .setTooltip(tip("in_view"))
                .setSaveConsumer(v -> OutOfMyWayConfig.inView = v)
                .build();
        general.addEntry(inViewEntry);

        general.addEntry(e.startIntSlider(
                        Component.translatable("config.out-of-my-way.in_view_distance"),
                        OutOfMyWayConfig.inViewDistance, 4, 16)
                .setDefaultValue(8)
                .setTextGetter(v -> Component.literal(v + " blocks"))
                .setTooltip(tip("in_view_distance"))
                .setRequirement(Requirement.isTrue(inViewEntry))
                .setSaveConsumer(v -> OutOfMyWayConfig.inViewDistance = v)
                .build());

        general.addEntry(e.startIntSlider(
                        Component.translatable("config.out-of-my-way.item_hold_disable_delay"),
                        OutOfMyWayConfig.itemHoldDisableDelay, 2, 10)
                .setDefaultValue(4)
                .setTextGetter(v -> Component.literal(v + "s"))
                .setTooltip(tip("item_hold_disable_delay"))
                .setSaveConsumer(v -> OutOfMyWayConfig.itemHoldDisableDelay = v)
                .build());

        general.addEntry(e.startIntSlider(
                        Component.translatable("config.out-of-my-way.speed_multiplier"),
                        Math.round(OutOfMyWayConfig.speedMultiplier * 10), 10, 30)
                .setDefaultValue(12)
                .setTextGetter(v -> Component.literal(String.format("%.1fx", v / 10.0f)))
                .setTooltip(tip("speed_multiplier"))
                .setSaveConsumer(v -> OutOfMyWayConfig.speedMultiplier = v / 10.0f)
                .build());

        ConfigCategory animals = builder.getOrCreateCategory(
                Component.translatable("config.out-of-my-way.category.animals"));

        BooleanListEntry wolvesEntry = e.startBooleanToggle(
                        Component.translatable("config.out-of-my-way.enable_wolves"),
                        OutOfMyWayConfig.enableWolves)
                .setDefaultValue(true)
                .setSaveConsumer(v -> OutOfMyWayConfig.enableWolves = v)
                .build();
        animals.addEntry(wolvesEntry);
        // Using plain string list instead of DropdownBoxEntry<Item> inside NestedListListEntry:
        // Cloth Config's NestedListListEntry doesn't route keyboard events to nested entries,
        // making the text field uneditable. The same bug exists in the Cloth Config demo screen.
        animals.addEntry(e.startStrList(
                        Component.translatable("config.out-of-my-way.custom_wolf_items"),
                        OutOfMyWayConfig.customWolfItems)
                .setDefaultValue(List.of("minecraft:rotten_flesh"))
                .setTooltip(tip("custom_items"))
                .setSaveConsumer(v -> OutOfMyWayConfig.customWolfItems = v)
                .build());

        BooleanListEntry catsEntry = e.startBooleanToggle(
                        Component.translatable("config.out-of-my-way.enable_cats"),
                        OutOfMyWayConfig.enableCats)
                .setDefaultValue(false)
                .setSaveConsumer(v -> OutOfMyWayConfig.enableCats = v)
                .build();
        animals.addEntry(catsEntry);

        BooleanListEntry parrotsEntry = e.startBooleanToggle(
                        Component.translatable("config.out-of-my-way.enable_parrots"),
                        OutOfMyWayConfig.enableParrots)
                .setDefaultValue(false)
                .setSaveConsumer(v -> OutOfMyWayConfig.enableParrots = v)
                .build();
        animals.addEntry(parrotsEntry);

        return builder.build();
    }
}
