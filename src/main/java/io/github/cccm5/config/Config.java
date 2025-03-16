package io.github.cccm5.config;

import org.bukkit.ChatColor;

public class Config {
    public static final String ERROR_TAG = ChatColor.RED + "Error: " + ChatColor.DARK_RED;
    public static final String SUCCESS_TAG = ChatColor.DARK_AQUA + "Cargo: " + ChatColor.WHITE;

    public static double scanRange = 100.0;
    public static int delay;
    public static double loadTax = 0.01D;
    public static double unloadTax = 0.01D;
    public static boolean cardinalDistance = true;
    public static boolean debug = false;
}
