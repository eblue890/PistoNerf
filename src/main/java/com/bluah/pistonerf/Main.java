package com.bluah.pistonerf;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
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

    @EventHandler
    public void onPiston(BlockPistonExtendEvent e) {
        if (getConfig().getBoolean("disable_piston_by_block")) {
            List<Material> movingMaterials = e.getBlocks().stream().map(Block::getType).collect(Collectors.toList());

            if (movingMaterials.stream().anyMatch(disabledBlocks::contains)) {
                e.setCancelled(true);
                if (getConfig().getBoolean("break_piston_on_disable")) {
                    Block piston = e.getBlock();
                    if (getConfig().getBoolean("drop_piston_on_break")) {
                        piston.getWorld().dropItemNaturally(piston.getLocation(), new ItemStack(Material.PISTON));
                    }
                    piston.setType(Material.AIR);
                }
            }
        }
    }

    public void loadDisabledBlocks() {
        disabledBlocks.clear();
        getConfig().getStringList("disabled_blocks").forEach(name -> {
            Material material = Material.matchMaterial(name);
            if (material != null) disabledBlocks.add(material);
        });
    }
}
