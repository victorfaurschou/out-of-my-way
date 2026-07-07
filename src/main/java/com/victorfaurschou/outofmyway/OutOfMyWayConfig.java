package com.victorfaurschou.outofmyway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OutOfMyWayConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve(OutOfMyWay.MOD_ID + ".json").toFile();

    public static boolean      enabled              = true;
    public static boolean      enableWolves         = true;
    public static boolean      enableCats           = false;
    public static boolean      enableParrots        = false;
    public static List<String> customWolfItems      = new ArrayList<>(List.of("minecraft:rotten_flesh"));
    public static int          minDistance          = 3;
    public static float        speedMultiplier      = 1.2f;
    public static boolean      inView               = true;
    public static int          inViewDistance       = 8;
    public static int          itemHoldDisableDelay = 4;

    public static void load() {
        try {
            if (CONFIG_FILE.exists()) {
                try (FileReader r = new FileReader(CONFIG_FILE)) {
                    ConfigData d = GSON.fromJson(r, ConfigData.class);
                    if (d != null) {
                        enabled              = d.enabled              != null ? d.enabled              : true;
                        enableWolves         = d.enableWolves         != null ? d.enableWolves         : true;
                        enableCats           = d.enableCats           != null ? d.enableCats           : false;
                        enableParrots        = d.enableParrots        != null ? d.enableParrots        : false;
                        customWolfItems      = d.customWolfItems      != null ? d.customWolfItems      : new ArrayList<>(List.of("minecraft:rotten_flesh"));
                        minDistance          = d.minDistance          != null ? Math.max(1, d.minDistance)                       : 3;
                        speedMultiplier      = d.speedMultiplier      != null ? Math.max(1f, d.speedMultiplier)                  : 1.2f;
                        inView               = d.inView               != null ? d.inView               : true;
                        inViewDistance       = d.inViewDistance       != null ? Math.max(4, Math.min(16, d.inViewDistance))      : 8;
                        itemHoldDisableDelay = d.itemHoldDisableDelay != null ? Math.max(2, Math.min(10, d.itemHoldDisableDelay)) : 4;
                    }
                }
            }
        } catch (IOException e) {
            OutOfMyWay.LOGGER.warn("Failed to load config", e);
        }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(new ConfigData(enabled, enableWolves, enableCats, enableParrots,
                        customWolfItems, minDistance, speedMultiplier, inView, inViewDistance,
                        itemHoldDisableDelay), w);
            }
        } catch (IOException e) {
            OutOfMyWay.LOGGER.warn("Failed to save config", e);
        }
    }

    static class ConfigData {
        Boolean      enabled;
        Boolean      enableWolves;
        Boolean      enableCats;
        Boolean      enableParrots;
        List<String> customWolfItems;
        Integer      minDistance;
        Float        speedMultiplier;
        Boolean      inView;
        Integer      inViewDistance;
        Integer      itemHoldDisableDelay;

        ConfigData() {}

        ConfigData(boolean enabled, boolean enableWolves, boolean enableCats, boolean enableParrots,
                   List<String> customWolfItems,
                   int minDistance, float speedMultiplier, boolean inView, int inViewDistance,
                   int itemHoldDisableDelay) {
            this.enabled              = enabled;
            this.enableWolves         = enableWolves;
            this.enableCats           = enableCats;
            this.enableParrots        = enableParrots;
            this.customWolfItems      = customWolfItems;
            this.minDistance          = minDistance;
            this.speedMultiplier      = speedMultiplier;
            this.inView               = inView;
            this.inViewDistance       = inViewDistance;
            this.itemHoldDisableDelay = itemHoldDisableDelay;
        }
    }
}
