package com.github.hoqhuuep.islandcraft.bukkit;

import java.util.List;

import javax.persistence.PersistenceException;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.hoqhuuep.islandcraft.bukkit.command.LocalChatCommandExecutor;
import com.github.hoqhuuep.islandcraft.bukkit.command.PartyChatCommandExecutor;
import com.github.hoqhuuep.islandcraft.bukkit.command.PrivateMessageCommandExecutor;
import com.github.hoqhuuep.islandcraft.bukkit.command.PurchasingCommandExecutor;
import com.github.hoqhuuep.islandcraft.bukkit.command.SuicideCommandExecutor;
import com.github.hoqhuuep.islandcraft.bukkit.ebeanserver.CompassTargetBean;
import com.github.hoqhuuep.islandcraft.bukkit.ebeanserver.EbeanServerDatabase;
import com.github.hoqhuuep.islandcraft.bukkit.event.BetterClockListener;
import com.github.hoqhuuep.islandcraft.bukkit.event.BetterCompassListener;
import com.github.hoqhuuep.islandcraft.bukkit.fileconfiguration.FileConfigurationConfig;
import com.github.hoqhuuep.islandcraft.bukkit.terraincontrol.IslandCraftBiomeGenerator;
import com.github.hoqhuuep.islandcraft.bukkit.worldguard.WorldGuardProtection;
import com.github.hoqhuuep.islandcraft.common.IslandMath;
import com.github.hoqhuuep.islandcraft.common.api.ICConfig;
import com.github.hoqhuuep.islandcraft.common.api.ICDatabase;
import com.github.hoqhuuep.islandcraft.common.api.ICProtection;
import com.github.hoqhuuep.islandcraft.common.api.ICServer;
import com.github.hoqhuuep.islandcraft.common.chat.LocalChat;
import com.github.hoqhuuep.islandcraft.common.chat.PartyChat;
import com.github.hoqhuuep.islandcraft.common.chat.PrivateMessage;
import com.github.hoqhuuep.islandcraft.common.extras.BetterClock;
import com.github.hoqhuuep.islandcraft.common.extras.BetterCompass;
import com.github.hoqhuuep.islandcraft.common.extras.Suicide;
import com.github.hoqhuuep.islandcraft.common.generator.IslandGenerator;
import com.github.hoqhuuep.islandcraft.common.purchasing.Purchasing;
import com.khorn.terraincontrol.TerrainControl;
import com.khorn.terraincontrol.biomegenerators.BiomeModeManager;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public final class IslandCraftPlugin extends JavaPlugin {
	private BiomeModeManager getBiomeModeManager() {
		return TerrainControl.getBiomeModeManager();
	}

	@Override
	public List<Class<?>> getDatabaseClasses() {
		return EbeanServerDatabase.getDatabaseClasses();
	}

	@Override
	public void onEnable() {
		getConfig().options().copyDefaults(true);
		saveConfig();
		try {
			getDatabase().find(CompassTargetBean.class).findRowCount();
		} catch (PersistenceException e) {
			installDDL();
		}

		final ICServer server = new BukkitServer(getServer());
		final ICConfig config = new FileConfigurationConfig(getConfig());
		final ICDatabase database = new EbeanServerDatabase(getDatabase());
		final ICProtection protection = new WorldGuardProtection(
				getWorldGuard());

		// Island Math
		final IslandMath islandMath = new IslandMath(config, server);

		// Generator
		IslandCraftBiomeGenerator.setGenerator(new IslandGenerator(config));
		BiomeModeManager biomeModeManager = getBiomeModeManager();
		biomeModeManager.register("IslandCraft",
				IslandCraftBiomeGenerator.class);

		// Purchasing
		PurchasingCommandExecutor purchasing = new PurchasingCommandExecutor(
				new Purchasing(database, config, protection, islandMath),
				server);
		getCommand("purchase").setExecutor(purchasing);
		getCommand("abandon").setExecutor(purchasing);
		getCommand("examine").setExecutor(purchasing);
		getCommand("rename").setExecutor(purchasing);

		// Chat
		LocalChatCommandExecutor localChat = new LocalChatCommandExecutor(
				new LocalChat(config), server);
		getCommand("l").setExecutor(localChat);
		PartyChatCommandExecutor partyChat = new PartyChatCommandExecutor(
				new PartyChat(database), server);
		getCommand("p").setExecutor(partyChat);
		getCommand("join").setExecutor(partyChat);
		getCommand("leave").setExecutor(partyChat);
		getCommand("members").setExecutor(partyChat);
		PrivateMessageCommandExecutor privateMessage = new PrivateMessageCommandExecutor(
				new PrivateMessage(), server);
		getCommand("m").setExecutor(privateMessage);

		// UsefulExtras
		register(new BetterClockListener(new BetterClock(), server));
		register(new BetterCompassListener(new BetterCompass(database), server));
		getCommand("suicide").setExecutor(
				new SuicideCommandExecutor(new Suicide(), server));
	}

	private void register(final Listener listener) {
		final PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(listener, this);
	}

	private WorldGuardPlugin getWorldGuard() {
		PluginManager pluginManager = getServer().getPluginManager();
		Plugin plugin = pluginManager.getPlugin("WorldGuard");

		// WorldGuard may not be loaded
		if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
			return null;
		}

		return (WorldGuardPlugin) plugin;
	}
}