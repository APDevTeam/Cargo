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
import io.github.cccm5.config.Config;
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

import java.util.ArrayList;
import java.util.List;

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
        getServer().getPluginManager().registerEvents(this, this);
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
    }

    public void onDisable() {
        economy = null;
        instance = null;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { // Plugin
        if (command.getName().equalsIgnoreCase("unload")) {
            if(!(sender instanceof Player)){
                sender.sendMessage(Config.ERROR_TAG + "You need to be a player to execute that command!");
                return true;
            }
            unload((Player) sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("load")) {
            if(!(sender instanceof Player)){
                sender.sendMessage(Config.ERROR_TAG + "You need to be a player to execute that command!");
                return true;
            }
            load((Player) sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("cargo")) {
            if(!sender.hasPermission("Cargo.cargo")){
                sender.sendMessage(Config.ERROR_TAG + "You don't have permission to do that!");
                return true;
            }
            sender.sendMessage(ChatColor.WHITE + "--[ " + ChatColor.DARK_AQUA + "  Movecraft Cargo " + ChatColor.WHITE + " ]--");
            sender.sendMessage(ChatColor.DARK_AQUA + "Scan Range: " + ChatColor.WHITE + Config.scanRange + " Blocks");
            sender.sendMessage(ChatColor.DARK_AQUA + "Transfer Delay: " + ChatColor.WHITE + Config.delay + " ticks");
            sender.sendMessage(ChatColor.DARK_AQUA + "Unload Tax: " + ChatColor.WHITE + String.format("%.2f",100*Config.unloadTax) + "%");
            sender.sendMessage(ChatColor.DARK_AQUA + "Load Tax: " + ChatColor.WHITE + String.format("%.2f",100*Config.loadTax) + "%");
            sender.sendMessage(ChatColor.DARK_AQUA + "Distance Type: " + ChatColor.WHITE + (Config.cardinalDistance ? "Cardinal" : "Direct"));
            return true;
        }
        return false;

    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!e.getClickedBlock().getType().name().equals("SIGN_POST") && !e.getClickedBlock().getType().name().endsWith("SIGN") && !e.getClickedBlock().getType().name().endsWith("WALL_SIGN")) {
            return;
        }
        Sign sign = (Sign) e.getClickedBlock().getState();
        if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[UnLoad]")) {
            unload(e.getPlayer());
            return;
        }
        if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[Load]")) {
            load(e.getPlayer());
        }

    }

    @EventHandler
    public void onSignPlace(SignChangeEvent e){
        if(!e.getBlock().getType().name().equals("SIGN_POST") && !e.getBlock().getType().name().endsWith("SIGN") && !e.getBlock().getType().name().endsWith("WALL_SIGN")){
            return;
        }
        if(ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[Load]") || ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[UnLoad]")){
            e.setLine(0,ChatColor.DARK_AQUA + (ChatColor.stripColor(e.getLine(0))).replaceAll("u","U").replaceAll("l","L"));
        }

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

    private void unload(Player player){
        if(!player.hasPermission("Cargo.unload")){
            player.sendMessage(Config.ERROR_TAG + "You don't have permission to do that!");
            return;
        }
        PlayerCraft playerCraft = CraftManager.getInstance().getCraftByPlayer(player);
        if(playersInQue.contains(player)){
            player.sendMessage(Config.ERROR_TAG + "You're already moving cargo!");
            return;
        }

        if(playerCraft == null){
            player.sendMessage(Config.ERROR_TAG + "You need to be piloting a craft to do that!");
            return;
        }
        //NPC cargoMerchant=null;
        List<NPC> nearbyMerchants = new ArrayList<>();
        double distance;//, lastScan = scanRange;
        MovecraftLocation loc = playerCraft.getHitBox().getMidPoint();
        for(NPC npc :NPCUtil.getNPCsWithTrait(CargoTrait.class)){
            if(!npc.isSpawned())
                continue;
            distance = Config.cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getZ()-npc.getEntity().getLocation().getZ()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getZ()-npc.getEntity().getLocation().getZ(),2));
            if( distance <= Config.scanRange){
                nearbyMerchants.add(npc);
            }
        }
        if(nearbyMerchants.size()==0){
            player.sendMessage(Config.ERROR_TAG + "You need to be within " + Config.scanRange + " blocks of a merchant to use that command!");
            return;
        }

        if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR){
            player.sendMessage(Config.ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }
        String guiName;
        TradableGUIItem finalItem = null;
        for(NPC cargoMerchant : nearbyMerchants) {
            if(finalItem!=null)
                break;
            guiName = cargoMerchant.getTrait(TraderTrait.class).getGUIName();
            AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
            TradeGUI tradeGUI = (TradeGUI) gui;
            ItemStack compareItem = player.getInventory().getItemInMainHand().clone();
            finalItem = null;
            for (TradeGUIPage page : tradeGUI.getPages()) {
                if (page == null) continue;
                for (AGUIItem tempItem : page.getItems("sell")) {
                    if (!(tempItem instanceof TradableGUIItem)) continue;
                    if (tempItem.getMainItem().isSimilar(compareItem)) {
                        if (tempItem.getMainItem().getAmount() > 1)
                            continue;
                        finalItem = (TradableGUIItem) tempItem;
                        break;
                    }
                }
                    if (finalItem == null || finalItem.getTradePrice() == 0.0) {
                        player.sendMessage(Config.ERROR_TAG + "You need to be holding a cargo item to do that!");
                        return;
                    }

            }
        }
        assert finalItem!=null;
        String itemName = finalItem.getMainItem().getItemMeta().getDisplayName() != null && finalItem.getMainItem().getItemMeta().getDisplayName().length() > 0 ? finalItem.getMainItem().getItemMeta().getDisplayName() : finalItem.getMainItem().getType().name().toLowerCase();

        List<Inventory> invs = CraftInventoryUtil.getInventories(playerCraft, finalItem.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        int size = invs.size();
        if(size <=0 ){
            player.sendMessage(Config.ERROR_TAG + "You have no " + itemName + " on this craft!");
            return;
        }

        player.sendMessage(Config.SUCCESS_TAG + "Started unloading cargo");
        playersInQue.add(player);
        new UnloadTask(playerCraft,finalItem ).runTaskTimer(this,Config.delay,Config.delay);
        new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
    }

    private void load(Player player){
        if(!player.hasPermission("Cargo.load")){
            player.sendMessage(Config.ERROR_TAG + "You don't have permission to do that!");
            return;
        }
        PlayerCraft playerCraft = CraftManager.getInstance().getCraftByPlayer(player);
        if(playersInQue.contains(player)){
            player.sendMessage(Config.ERROR_TAG + "You're already moving cargo!");
            return;
        }

        if(playerCraft == null){
            player.sendMessage(Config.ERROR_TAG + "You need to be piloting a craft to do that!");
            return;
        }
        //NPC cargoMerchant=null;
        List<NPC> nearbyMerchants = new ArrayList<>();
        double distance;//, lastScan = scanRange;
        MovecraftLocation loc = playerCraft.getHitBox().getMidPoint();
        for(NPC npc :NPCUtil.getNPCsWithTrait(CargoTrait.class)){
            if(!npc.isSpawned())
                continue;
            distance = Config.cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getZ()-npc.getEntity().getLocation().getZ()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getZ()-npc.getEntity().getLocation().getZ(),2));
            if( distance <= Config.scanRange){
                nearbyMerchants.add(npc);
            }
        }
        if(nearbyMerchants.size()==0){
            player.sendMessage(Config.ERROR_TAG + "You need to be within " + Config.scanRange + " blocks of a merchant to use that command!");
            return;
        }

        if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR){
            getLogger().info(player.getInventory().getItemInMainHand().getType().name());
            player.sendMessage(Config.ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }
        String guiName;
        TradableGUIItem finalItem = null;
        for(NPC cargoMerchant : nearbyMerchants) {
            guiName = cargoMerchant.getTrait(TraderTrait.class).getGUIName();
            AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
            TradeGUI tradeGUI = (TradeGUI) gui;
            ItemStack compareItem = player.getInventory().getItemInMainHand().clone();
            for (TradeGUIPage page : tradeGUI.getPages()) {
                if (page == null) 
                    continue;

                for (AGUIItem tempItem : page.getItems("buy")) {
                    if (!(tempItem instanceof TradableGUIItem))
                        continue;

                    TradableGUIItem tradeItem = (TradableGUIItem) tempItem;
                    if (tradeItem.getMainItem().isSimilar(compareItem)) {
                        if (tempItem.getMainItem().getAmount() > 1)
                            continue;

                        finalItem = tradeItem;
                        break;
                    }
                }
                if (finalItem != null)
                    break;
            }
            if (finalItem != null)
                break;
        }
        if (finalItem == null || finalItem.getTradePrice() == 0.0) {
            player.sendMessage(Config.ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }

        final ItemMeta meta = finalItem.getMainItem().getItemMeta();
        String itemName = meta.getDisplayName() != null && meta.getDisplayName().length() > 0 ? meta.getDisplayName() : finalItem.getMainItem().getType().name().toLowerCase();
        if(!economy.has(player,finalItem.getTradePrice()*(1+Config.loadTax))){
            player.sendMessage(Config.ERROR_TAG + "You don't have enough money to buy any " + itemName + "!");
            return;
        }

        List<Inventory> invs = CraftInventoryUtil.getInventoriesWithSpace(playerCraft, finalItem.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        int size = invs.size();
        if(size <=0 ){
            player.sendMessage(Config.ERROR_TAG + "You don't have any space for " + itemName + " on this craft!");
            return;
        }

        playersInQue.add(player);
        new LoadTask(playerCraft,finalItem).runTaskTimer(this,Config.delay,Config.delay);
        new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
        player.sendMessage(Config.SUCCESS_TAG + "Started loading cargo");
    }
}
