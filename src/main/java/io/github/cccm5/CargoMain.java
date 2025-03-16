package io.github.cccm5;

import com.degitise.minevid.dtlTraders.Main;
import com.degitise.minevid.dtlTraders.guis.AGUI;
import com.degitise.minevid.dtlTraders.guis.gui.TradeGUI;
import com.degitise.minevid.dtlTraders.guis.gui.TradeGUIPage;
import com.degitise.minevid.dtlTraders.guis.items.AGUIItem;
import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import com.degitise.minevid.dtlTraders.utils.citizens.TraderTrait;
import io.github.cccm5.async.LoadTask;
import io.github.cccm5.async.ProcessingTask;
import io.github.cccm5.async.UnloadTask;
import io.github.cccm5.commands.CargoCommand;
import io.github.cccm5.commands.LoadCommand;
import io.github.cccm5.commands.UnloadCommand;
import io.github.cccm5.config.Config;
import io.github.cccm5.listener.SignClickListener;
import io.github.cccm5.listener.SignPlaceListener;
import io.github.cccm5.util.CraftInventoryUtil;
import io.github.cccm5.util.NPCUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CargoMain extends JavaPlugin implements Listener {
    private static Economy economy;
    private static ArrayList<Player> playersInQue;
    private static CargoMain instance;
    private static Main dtlTradersPlugin;

    private void loadConfig() {
        FileConfiguration config = getConfig();

        config.addDefault("Scan range",100.0);
        config.addDefault("Transfer delay ticks",300);
        config.addDefault("Load tax percent", 0.01D);
        config.addDefault("Unload tax percent", 0.01D);
        config.addDefault("Cardinal distance",true);
        config.addDefault("Debug mode",false);
        config.options().copyDefaults(true);
        saveConfig();

        Config.scanRange = config.getDouble("Scan range") >= 1.0 ? config.getDouble("Scan range") : 100.0;
        Config.delay = config.getInt("Transfer delay ticks");
        Config.loadTax = config.getDouble("Load tax percent")<=1.0 && config.getDouble("Load tax percent")>=0.0 ? config.getDouble("Load tax percent") : 0.01;
        Config.unloadTax = config.getDouble("Unload tax percent")<=1.0 && config.getDouble("Unload tax percent")>=0.0 ? config.getDouble("Unload tax percent") : 0.01;
        Config.cardinalDistance = config.getBoolean("Cardinal distance");
        Config.debug = config.getBoolean("Debug mode");
    }

    public void onEnable() {
        playersInQue = new ArrayList<>();
        instance = this;

        loadConfig();
        //************************
        //*    Load Movecraft    *
        //************************
        if(getServer().getPluginManager().getPlugin("Movecraft") == null || !getServer().getPluginManager().getPlugin("Movecraft").isEnabled()) {
            getLogger().severe("Movecraft not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        //************************
        //*    Load  Citizens    *
        //************************
        if(getServer().getPluginManager().getPlugin("Citizens") == null || !getServer().getPluginManager().getPlugin("Citizens").isEnabled()) {
            getLogger().severe("Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        }
        if(CitizensAPI.getTraitFactory().getTrait(CargoTrait.class)==null)
            CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(CargoTrait.class));
        //************************
        //*      Load Vault      *
        //************************
        if (getServer().getPluginManager().getPlugin("Vault") == null || !getServer().getPluginManager().getPlugin("Vault").isEnabled()) {
            getLogger().severe("Vault not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        }
        //************************
        //*    Load dtlTraders   *
        //************************
        Plugin traders = getServer().getPluginManager().getPlugin("dtlTraders");
        if (!(traders instanceof Main)){
            getLogger().severe("dtlTraders not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        dtlTradersPlugin = (Main) traders;
        economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();

        getServer().getPluginManager().registerEvents(new SignClickListener(), this);
        getServer().getPluginManager().registerEvents(new SignPlaceListener(), this);

        getCommand("cargo").setExecutor(new CargoCommand());
        getCommand("load").setExecutor(new LoadCommand());
        getCommand("unload").setExecutor(new UnloadCommand());
    }

    public void onDisable() {
        economy = null;
        instance = null;
    }

    public static Economy getEconomy(){
        return economy;
    }

    public static List<Player> getQue(){
        return playersInQue;
    }

    public static CargoMain getInstance(){
        return instance;
    }

    public void unload(@NotNull Player player){
        if (!player.hasPermission("Cargo.unload")) {
            player.sendMessage(Config.ERROR_TAG + "You don't have permission to do that!");
            return;
        }
        if (playersInQue.contains(player)) {
            player.sendMessage(Config.ERROR_TAG + "You're already moving cargo!");
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            player.sendMessage(Config.ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }

        PlayerCraft playerCraft = CraftManager.getInstance().getCraftByPlayer(player);
        if(playerCraft == null) {
            player.sendMessage(Config.ERROR_TAG + "You need to be piloting a craft to do that!");
            return;
        }

        List<NPC> nearbyMerchants = NPCUtil.getNPCsInRange(playerCraft.getHitBox().getMidPoint());
        if (nearbyMerchants.isEmpty()) {
            player.sendMessage(Config.ERROR_TAG + "You need to be within " + Config.scanRange + " blocks of a merchant to use that command!");
            return;
        }

        Set<TradableGUIItem> items = NPCUtil.getItems(nearbyMerchants, player.getInventory().getItemInMainHand(), dtlTradersPlugin, "sell");
        TradableGUIItem finalItem = null;
        for (TradableGUIItem item : items) {
            if (finalItem == null) {
                finalItem = item;
                continue;
            }

            if (item.getTradePrice() > finalItem.getTradePrice())
                finalItem = item;
        }
        if (finalItem == null || finalItem.getTradePrice() <= 0.0) {
            player.sendMessage(Config.ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }

        String itemName = finalItem.getMainItem().getItemMeta().getDisplayName() != null && finalItem.getMainItem().getItemMeta().getDisplayName().length() > 0 ? finalItem.getMainItem().getItemMeta().getDisplayName() : finalItem.getMainItem().getType().name().toLowerCase();
        List<Inventory> inventories = CraftInventoryUtil.getInventories(playerCraft, finalItem.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        if (inventories.isEmpty()) {
            player.sendMessage(Config.ERROR_TAG + "You have no " + itemName + ChatColor.RESET + " on this craft!");
            return;
        }

        player.sendMessage(Config.SUCCESS_TAG + "Started unloading cargo");
        playersInQue.add(player);
        new UnloadTask(playerCraft, finalItem).runTaskTimer(this, Config.delay, Config.delay);
        new ProcessingTask(player, finalItem, inventories.size()).runTaskTimer(this, 0, 20);
    }

    public void load(@NotNull Player player){
        if (!player.hasPermission("Cargo.load")) {
            player.sendMessage(Config.ERROR_TAG + "You don't have permission to do that!");
            return;
        }
        if (playersInQue.contains(player)) {
            player.sendMessage(Config.ERROR_TAG + "You're already moving cargo!");
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            getLogger().info(player.getInventory().getItemInMainHand().getType().name());
            player.sendMessage(Config.ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }

        PlayerCraft playerCraft = CraftManager.getInstance().getCraftByPlayer(player);
        if (playerCraft == null) {
            player.sendMessage(Config.ERROR_TAG + "You need to be piloting a craft to do that!");
            return;
        }

        List<NPC> nearbyMerchants = NPCUtil.getNPCsInRange(playerCraft.getHitBox().getMidPoint());
        if (nearbyMerchants.isEmpty()) {
            player.sendMessage(Config.ERROR_TAG + "You need to be within " + Config.scanRange + " blocks of a merchant to use that command!");
            return;
        }

        Set<TradableGUIItem> items = NPCUtil.getItems(nearbyMerchants, player.getInventory().getItemInMainHand(), dtlTradersPlugin, "buy");
        TradableGUIItem finalItem = null;
        for (TradableGUIItem item : items) {
            if (finalItem == null) {
                finalItem = item;
                continue;
            }

            if (item.getTradePrice() < finalItem.getTradePrice())
                finalItem = item;
        }
        if (finalItem == null || finalItem.getTradePrice() <= 0.0) {
            player.sendMessage(Config.ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }

        String itemName = finalItem.getMainItem().getItemMeta().getDisplayName() != null && finalItem.getMainItem().getItemMeta().getDisplayName().length() > 0 ? finalItem.getMainItem().getItemMeta().getDisplayName() : finalItem.getMainItem().getType().name().toLowerCase();
        if(!economy.has(player,finalItem.getTradePrice()*(1+Config.loadTax))){
            player.sendMessage(Config.ERROR_TAG + "You don't have enough money to buy any " + itemName + ChatColor.RESET + "!");
            return;
        }

        List<Inventory> invs = CraftInventoryUtil.getInventoriesWithSpace(playerCraft, finalItem.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        int size = invs.size();
        if(size <=0 ){
            player.sendMessage(Config.ERROR_TAG + "You don't have any space for " + itemName + ChatColor.RESET + " on this craft!");
            return;
        }

        playersInQue.add(player);
        new LoadTask(playerCraft,finalItem).runTaskTimer(this,Config.delay,Config.delay);
        new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
        player.sendMessage(Config.SUCCESS_TAG + "Started loading cargo");
    }
}
