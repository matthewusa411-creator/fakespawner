package dev.fakespawner;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Settings changed from the Right Shift menu. Saved in config/fakespawner.properties. */
public final class FakeConfig {
    public static boolean spawnerEnabled = true;
    public static boolean elytraEnabled = true;
    public static boolean flightEnabled = true;
    public static long spawnerWorth = 1_000;      // $ per spawner (a stack of 64 = 64x this)
    public static long elytraWorth = 250_000;     // $ per elytra
    public static int lootSeconds = 45;           // seconds per bone/arrow per spawner

    private FakeConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("fakespawner.properties");
    }

    public static void load() {
        Path path = file();
        if (!Files.exists(path)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
            spawnerEnabled = Boolean.parseBoolean(p.getProperty("spawnerEnabled", "true"));
            elytraEnabled = Boolean.parseBoolean(p.getProperty("elytraEnabled", "true"));
            flightEnabled = Boolean.parseBoolean(p.getProperty("flightEnabled", "true"));
            spawnerWorth = Long.parseLong(p.getProperty("spawnerWorth", "1000"));
            elytraWorth = Long.parseLong(p.getProperty("elytraWorth", "250000"));
            lootSeconds = Math.max(1, Integer.parseInt(p.getProperty("lootSeconds", "45")));
        } catch (IOException | NumberFormatException ignored) {
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("spawnerEnabled", Boolean.toString(spawnerEnabled));
        p.setProperty("elytraEnabled", Boolean.toString(elytraEnabled));
        p.setProperty("flightEnabled", Boolean.toString(flightEnabled));
        p.setProperty("spawnerWorth", Long.toString(spawnerWorth));
        p.setProperty("elytraWorth", Long.toString(elytraWorth));
        p.setProperty("lootSeconds", Integer.toString(lootSeconds));
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Fake Spawner settings");
        } catch (IOException ignored) {
        }
    }

    /** 32000 -> "32K", 1500 -> "1.5K", 2500000 -> "2.5M". */
    public static String money(long value) {
        if (value >= 1_000_000) return trim(value / 1_000_000.0) + "M";
        if (value >= 1_000) return trim(value / 1_000.0) + "K";
        return Long.toString(value);
    }

    private static String trim(double d) {
        String s = String.format(Locale.ROOT, "%.1f", d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
