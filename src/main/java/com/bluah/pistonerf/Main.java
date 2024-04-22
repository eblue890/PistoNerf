package com.bluah.pistonerf;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
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

        // Check configuration for minecart interaction allowance
        boolean allowMinecartInteractions = getConfig().getBoolean("allow_minecart_interactions");

        if (!allowMinecartInteractions || !containsSlimeOrHoney) {
            // Apply original logic if minecart interactions aren't allowed or if slime/honey isn't involved
            if ((containsSlimeOrHoney && getConfig().getBoolean("disable_slime_honey_interaction_by_block") && movingMaterials.stream().anyMatch(disabledSlimeHoneyInteractions::contains)) ||
                    (!containsSlimeOrHoney && getConfig().getBoolean("disable_piston_by_block") && movingMaterials.stream().anyMatch(disabledBlocks::contains))) {
                setEventCancelled(event);
                handlePistonBreaking(piston);
            }
        } else {
            // Here, allowMinecartInteractions is true and containsSlimeOrHoney is also true
            // Logic to not cancel the event based on additional conditions
            // Assuming a simplistic check for a minecart being near the piston or slime/honey block
            boolean minecartNearby = checkForMinecartNearby(piston.getLocation()); // This method needs to be implemented

            if (!minecartNearby) {
                // If no minecart is detected nearby, apply the original logic
                if (movingMaterials.stream().anyMatch(disabledSlimeHoneyInteractions::contains)) {
                    setEventCancelled(event);
                    handlePistonBreaking(piston);
                }
            }
            // If a minecart is detected, the event won't be cancelled, allowing for natural game interactions
        }
    }

    private boolean checkForMinecartNearby(Location location) {
        // Define the radius within which to look for minecarts. You may adjust this value.
        double radius = 2.0;

        // Get a list of entities within the specified radius of the location
        List<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
                .filter(e -> e instanceof Minecart) // Filter to only include minecarts
                .collect(Collectors.toList());

        // If the list is not empty, there are minecarts nearby
        return !nearbyEntities.isEmpty();
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
            Material pistonMaterial = piston.getType(); // Determine the type of piston
            piston.setType(Material.AIR); // Remove the piston block

            if (getConfig().getBoolean("drop_piston_on_break")) {
                // Ensure the correct item is dropped based on the piston type
                piston.getWorld().dropItemNaturally(piston.getLocation(), new ItemStack(pistonMaterial, 1));
            }
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
