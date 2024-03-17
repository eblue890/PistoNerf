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
    private Set<Material> disabledSlimeHoneyInteractions = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDisabledBlocks();
        loadDisabledSlimeHoneyInteractions();

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

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        handlePistonMovement(e.getBlocks(), e.getBlock(), e);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        handlePistonMovement(e.getBlocks(), e.getBlock(), e);
    }

    private void handlePistonMovement(List<Block> blocks, Block piston, Object event) {
        List<Material> movingMaterials = blocks.stream().map(Block::getType).collect(Collectors.toList());
        boolean containsSlimeOrHoney = movingMaterials.contains(Material.SLIME_BLOCK) || movingMaterials.contains(Material.HONEY_BLOCK);

        if (containsSlimeOrHoney && getConfig().getBoolean("disable_slime_honey_interaction_by_block")) {
            if (movingMaterials.stream().anyMatch(disabledSlimeHoneyInteractions::contains)) {
                setEventCancelled(event);
                handlePistonBreaking(piston);
            }
        } else if (getConfig().getBoolean("disable_piston_by_block")) {
            if (movingMaterials.stream().anyMatch(disabledBlocks::contains)) {
                setEventCancelled(event);
                handlePistonBreaking(piston);
            }
        }
    }

    private void setEventCancelled(Object event) {
        if (event instanceof BlockPistonExtendEvent) {
            ((BlockPistonExtendEvent) event).setCancelled(true);
        } else if (event instanceof BlockPistonRetractEvent) {
            ((BlockPistonRetractEvent) event).setCancelled(true);
        }
    }

    private void handlePistonBreaking(Block piston) {
        if (getConfig().getBoolean("break_piston_on_disable")) {
            if (getConfig().getBoolean("drop_piston_on_break")) {
                piston.getWorld().dropItemNaturally(piston.getLocation(), new ItemStack(Material.PISTON));
            }
            piston.setType(Material.AIR);
        }
    }

    public void loadDisabledBlocks() {
        disabledBlocks.clear();
        List<String> blockNames = getConfig().getStringList("disabled_blocks");
        for (String name : blockNames) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                disabledBlocks.add(material);
            }
        }
    }

    public void loadDisabledSlimeHoneyInteractions() {
        disabledSlimeHoneyInteractions.clear();
        List<String> blockNames = getConfig().getStringList("disabled_slime_honey_interactions");
        for (String name : blockNames) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                disabledSlimeHoneyInteractions.add(material);
            }
        }
    }
}
