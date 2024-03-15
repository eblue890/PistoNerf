package com.bluah.pistonerf;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Main extends JavaPlugin implements Listener {

    private Set<Material> disabledBlocks = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDisabledBlocks();

        PluginCommand pistonerfCommand = getCommand("pistonerf");
        if (pistonerfCommand != null) {
            ConfigCommand configCommand = new ConfigCommand(this);
            pistonerfCommand.setExecutor(configCommand);
            pistonerfCommand.setTabCompleter(configCommand);
        } else {
            getLogger().severe("Failed to register command 'pistonerf'. Check your plugin.yml file.");
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (containsDisabledBlock(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (containsDisabledBlock(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    private boolean containsDisabledBlock(List<Block> blocks) {
        return blocks.stream().map(Block::getType).anyMatch(disabledBlocks::contains);
    }

    public void loadDisabledBlocks() {
        disabledBlocks.clear();
        getConfig().getStringList("disabled_blocks").forEach(name -> {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                disabledBlocks.add(material);
            } else {
                getLogger().warning("Invalid block name in config: " + name);
            }
        });
    }
}
