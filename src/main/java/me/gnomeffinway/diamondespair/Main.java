package me.gnomeffinway.diamondespair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DiamonDespair plugin for Bukkit
 * 
 * Replaces a block with another upon being broken, as specified in the config.yml file.
 * 
 * @author GnomeffinWay
 */

public class Main extends JavaPlugin implements Listener{
	
	public static Main plugin;
	public final Logger logger=Logger.getLogger("Minecraft");
	private FileConfiguration customConfig = null;
	private File customConfigFile = null;
	
	// Plugin enable operations
	public void onEnable(){
		getServer().getPluginManager().registerEvents(this,this);
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	// Plugin disable operations
	public void onDisable(){
	}
		
	
	// Listens for block breaks. Replaces drop if block is equal to activator.
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreak(BlockBreakEvent event){
        Block block=event.getBlock();     
		Material mat=event.getBlock().getType();
		Player plr=event.getPlayer();
		int actID=(Integer) getConfig().get("activator");
		int repID=(Integer) getConfig().get("replacement");
		Material activator;
		Material replacement;
		
		//Seeing if activator or replacement values are items instead of blocks
		if(actID>=255){
			this.logger.info("[DiaD] Error: Activator field not valid [not a block ID]");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		if(repID>=255){
			this.logger.info("[DiaD] Error: Replacement field not valid [not a block ID]");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		// Determining if the activator in the config is a valid material ID
		try{
			activator=Material.getMaterial(actID);
		}
		catch(Exception e){
			this.logger.info("[DiaD] Error: Activator field not valid");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		// Determining if replacement in the config is a valid material ID
		try{
			replacement=Material.getMaterial(repID);
		}
		catch(Exception e){
			this.logger.info("[DiaD] Error: Replacement field not valid");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		// Checking if block is equal to activator; replacing block and sending player a message
		if(mat.equals(activator) && (!(Boolean)getConfig().get("individual") || playerSearch(plr.getName()))){		
			if(!(Boolean)getConfig().get("exp-drop"))
				event.setExpToDrop(0);
			block.setType(replacement);
			if(!plr.hasMetadata("diad-warned"))
				plr.sendMessage(ChatColor.RED+"That material is no longer mineable");
			plr.setMetadata("diad-warned", new FixedMetadataValue(this,true));
		}
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(cmd.getName().equalsIgnoreCase("diamondespair")){
			sender.sendMessage(ChatColor.GOLD+"[DiaD] "+ChatColor.YELLOW+"Activator ID is "+getConfig().get("activator")+" and Replacement ID is "+getConfig().get("replacement"));
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("diadactivator")){
			if(args.length < 1){
				sender.sendMessage(ChatColor.RED+"[DiaD] ERROR: /diadactivator <block ID>");
				return true;
			}
			
			int activatorID;
			
			try{
				activatorID=Integer.parseInt(args[0]);
			}
			catch(NumberFormatException e){
				sender.sendMessage(ChatColor.RED+"[DiaD] ERROR: Activator field not valid [not a number]");
				return true;
			}
			
			if(activatorID>=255){
				this.logger.info("[DiaD] Error: Activator field not valid [not a block ID]");
				return true;
			}
			
			getConfig().set("activator", activatorID);
			saveConfig();
			sender.sendMessage(ChatColor.GOLD+"[DiaD] "+ChatColor.YELLOW+"Activator block is now"+Material.getMaterial(getConfig().getInt("activator")));
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("diadreplacement")){
			if(args.length < 1){
				sender.sendMessage(ChatColor.RED+"[DiaD] ERROR: /diadreplacement <block ID>");
				return true;
			}
			
			int replacementID;
			
			try{
				replacementID=Integer.parseInt(args[0]);
			}
			catch(NumberFormatException e){
				sender.sendMessage(ChatColor.RED+"[DiaD] ERROR: Replacment field not valid [not a number]");
				getServer().getPluginManager().disablePlugin(this);
				return true;
			}
			
			if(replacementID>=255){
				this.logger.info("[DiaD] Error: Replacement field not valid [not a block ID]");
				getServer().getPluginManager().disablePlugin(this);
				return true;
			}
			
			getConfig().set("replacement", replacementID);
			saveConfig();
			sender.sendMessage(ChatColor.GOLD+"[DiaD] "+ChatColor.YELLOW+"Replacement block is now"+Material.getMaterial(getConfig().getInt("replacement")));
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("diadpunish")){
			if(!getConfig().getBoolean("individual")){
				sender.sendMessage(ChatColor.RED+"[DiaD] ERROR: 'individual' must be set to true in config.yml");
				return true;
			}
			if(args.length < 1){
				sender.sendMessage(ChatColor.RED+"[DiaD] ERROR: /diadpunish <player>");
				return true;
			}
			Player target=getServer().getPlayerExact(args[0]);
			getCustomConfig().set(target.getName(),true);
			saveCustomConfig();
			sender.sendMessage(ChatColor.GOLD+"[DiaD] "+ChatColor.YELLOW+target.getName()+(" has been punished"));
			if(target.isOnline())
				target.sendMessage(ChatColor.RED+"Your pickaxe vibrates strangely.");
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("diadforgive")){
			if(!getConfig().getBoolean("individual")){
				sender.sendMessage(ChatColor.RED+"[DiaD] ERROR: 'individual' must be set to true in config.yml");
				return true;
			}
			if(args.length < 1){
				sender.sendMessage(ChatColor.RED+"[DiaD] ERROR: /diadforgive <player>");
				return true;
			}
			Player target=getServer().getPlayerExact(args[0]);
			getCustomConfig().set(target.getName(),false);
			saveCustomConfig();
			sender.sendMessage(ChatColor.GOLD+"[DiaD] "+ChatColor.YELLOW+target.getName()+(" has been forgiven"));
			if(target.isOnline())
				target.sendMessage(ChatColor.RED+"Your pickaxe feels normal again.");
			return true;
		}
		return false;
	}
	
	public void reloadCustomConfig() {
	    if (customConfigFile == null) {
	    customConfigFile = new File(getDataFolder(), "players.yml");
	    }
	    customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
	 
	    // Look for defaults in the jar
	    InputStream defConfigStream = this.getResource("players.yml");
	    if (defConfigStream != null) {
	        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        customConfig.setDefaults(defConfig);
	    }
	}
	
	private boolean playerSearch(String name){
		if(getCustomConfig().get(name)==null)
			return false;
		if(getCustomConfig().getBoolean(name))
			return true;
		return false;
	}
	
	public FileConfiguration getCustomConfig() {
	    if (customConfig == null) {
	        this.reloadCustomConfig();
	    }
	    return customConfig;
	}
	
	public void saveCustomConfig() {
	    if (customConfig == null || customConfigFile == null) {
	    return;
	    }
	    try {
	        getCustomConfig().save(customConfigFile);
	    } catch (IOException ex) {
	        this.getLogger().log(Level.SEVERE, "Could not save config to " + customConfigFile, ex);
	    }
	}

}

// TODO:
// Prevent drops from creative-placed blocks
// Fix config spacing
// Sort players.yml
// Set individual in-game
// Class separation/clean up