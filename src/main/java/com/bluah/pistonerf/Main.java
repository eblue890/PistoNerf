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

    private final Set<Material> disabledBlocks = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PluginCommand pistonerfCommand = getCommand("pistonerf");
        if (pistonerfCommand != null) {
            ConfigCommand configCommand = new ConfigCommand(this);
            pistonerfCommand.setExecutor(configCommand);
            pistonerfCommand.setTabCompleter(configCommand); // Set the TabCompleter
        } else {
            getLogger().severe("Failed to register command 'pistonerf'. Check your plugin.yml file.");
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        updateDisabledBlocks();
    }

    @EventHandler(ignoreCancelled = true)
    private void onPistonExtend(BlockPistonExtendEvent e) {
        boolean restrictCropInteraction = getConfig().getBoolean("slime_honey_crop_interaction");
        if (restrictCropInteraction && affectsRestrictedBlocks(e.getBlocks())) {
            e.setCancelled(true);
        }

        if (getConfig().getBoolean("disable_piston_by_block")) {
            for (Block block : e.getBlocks()) {
                if (disabledBlocks.contains(block.getType())) {
                    e.setCancelled(true);
                    if (getConfig().getBoolean("break_piston_on_disable")) {
                        Block piston = e.getBlock();
                        if (getConfig().getBoolean("drop_piston_on_break")) {
                            piston.getWorld().dropItemNaturally(piston.getLocation(), new ItemStack(Material.PISTON));
                        }
                        piston.setType(Material.AIR);
                    }
                    break;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPistonRetract(BlockPistonRetractEvent e) {
        if (getConfig().getBoolean("slime_honey_crop_interaction") && affectsRestrictedBlocks(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    private boolean affectsRestrictedBlocks(List<Block> blocks) {
        boolean containsSlimeOrHoney = blocks.stream()
                .map(Block::getType)
                .anyMatch(type -> type == Material.SLIME_BLOCK || type == Material.HONEY_BLOCK);

        boolean affectsCropBlocks = blocks.stream()
                .map(Block::getType)
                .anyMatch(CropBlocks::isCrop); // Make sure CropBlocks.isCrop() is correctly implemented

        return containsSlimeOrHoney && affectsCropBlocks;
    }

    public void updateDisabledBlocks() {
        disabledBlocks.clear();
        List<String> disabledBlocksList = getConfig().getStringList("disabled_blocks");
        for (String blockName : disabledBlocksList) {
            Material material = Material.matchMaterial(blockName);
            if (material != null) {
                disabledBlocks.add(material);
            } else {
                getLogger().warning("Invalid block name in config: " + blockName);
            }
        }
    }
}
