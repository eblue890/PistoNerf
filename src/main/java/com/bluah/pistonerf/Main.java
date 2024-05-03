package com.bluah.pistonerf;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.data.Directional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Main extends JavaPlugin implements Listener, org.bukkit.command.CommandExecutor {

    private Set<Material> disabledBlocks = new HashSet<>();
    private Set<Material> disabledSlimeHoneyInteractions = new HashSet<>();
    private Set<Material> disabledDispenserItems = new HashSet<>();
    private Set<Material> disabledDispenserBlocks = new HashSet<>();

    @Override
    public void onEnable() {
        super.onEnable();
        saveDefaultConfig();
        loadDisabledBlocks();
        loadDisabledSlimeHoneyInteractions();
        loadDispenserConfigs();

        getCommand("pistonerf").setExecutor(this);
        getCommand("pistonerf").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("pistonerf")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadDisabledBlocks();
                loadDisabledSlimeHoneyInteractions();
                loadDispenserConfigs();
                sender.sendMessage(ChatColor.GREEN + "Pistonerf configuration reloaded.");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Usage: /pistonerf reload");
            return false;
        }
        return false;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
        }

        return completions;
    }

    private void loadDispenserConfigs() {
        disabledDispenserItems.clear();
        disabledDispenserBlocks.clear();
        if (getConfig().getBoolean("anti-dispenser-enable")) {
            List<String> items = getConfig().getStringList("disabled-dispenser-items");
            for (String item : items) {
                Material mat = Material.matchMaterial(item);
                if (mat != null) {
                    disabledDispenserItems.add(mat);
                }
            }
            List<String> blocks = getConfig().getStringList("disabled-dispenser-blocks");
            for (String block : blocks) {
                Material mat = Material.matchMaterial(block);
                if (mat != null) {
                    disabledDispenserBlocks.add(mat);
                }
            }
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Dispenser) {
            Dispenser dispenser = (Dispenser) block.getState();
            BlockFace facing = ((Directional) dispenser.getBlockData()).getFacing();
            Block affectedBlock = block.getRelative(facing);

            ItemStack dispensedItem = event.getItem();
            if (disabledDispenserItems.contains(dispensedItem.getType()) && disabledDispenserBlocks.contains(affectedBlock.getType())) {
                event.setCancelled(true);
            }
        }
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
        boolean allowMinecartInteractions = getConfig().getBoolean("allow_minecart_interactions");

        if (!allowMinecartInteractions || !containsSlimeOrHoney) {
            if ((containsSlimeOrHoney && getConfig().getBoolean("disable_slime_honey_interaction_by_block") && movingMaterials.stream().anyMatch(disabledSlimeHoneyInteractions::contains)) ||
                    (!containsSlimeOrHoney && getConfig().getBoolean("disable_piston_by_block") && movingMaterials.stream().anyMatch(disabledBlocks::contains))) {
                setEventCancelled(event);
                handlePistonBreaking(piston);
            }
        } else {
            boolean minecartNearby = checkForMinecartNearby(piston.getLocation());
            if (!minecartNearby && movingMaterials.stream().anyMatch(disabledSlimeHoneyInteractions::contains)) {
                setEventCancelled(event);
                handlePistonBreaking(piston);
            }
        }
    }

    private boolean checkForMinecartNearby(Location location) {
        double radius = 2.0;
        List<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
                .filter(e -> e instanceof Minecart)
                .collect(Collectors.toList());
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
            Material pistonMaterial = piston.getType();
            piston.setType(Material.AIR);
            if (getConfig().getBoolean("drop_piston_on_break")) {
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
