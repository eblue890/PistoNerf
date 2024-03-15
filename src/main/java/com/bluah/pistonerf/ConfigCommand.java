package com.bluah.pistonerf;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public ConfigCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /pistonerf add|remove|list|reload");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "reload":
                plugin.reloadConfig();
                plugin.loadDisabledSlimeHoneyInteractions(); // Load the new configuration for slime/honey interactions
                sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                return true;
            case "list":
                sender.sendMessage(ChatColor.GREEN + "Disabled blocks:");
                List<String> disabledBlocks = plugin.getConfig().getStringList("disabled_blocks");
                if (disabledBlocks.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "No blocks are currently disabled.");
                } else {
                    disabledBlocks.forEach(blockName -> sender.sendMessage(ChatColor.GREEN + "- " + blockName));
                }
                return true;
            case "add":
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " " + subcommand + " blocks|slime_honey <material>");
                    return true;
                }
                handleMaterialModification(subcommand, args[1], args[2], sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid command. Use add, remove, list, or reload.");
                return true;
        }
    }

    private void handleMaterialModification(String action, String listType, String materialNameInput, CommandSender sender) {
        String materialName = materialNameInput.toUpperCase();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            sender.sendMessage(ChatColor.RED + "Invalid material name: " + materialName);
            return;
        }

        FileConfiguration config = plugin.getConfig();
        List<String> targetList;
        if ("blocks".equalsIgnoreCase(listType)) {
            targetList = config.getStringList("disabled_blocks");
        } else if ("slime_honey".equalsIgnoreCase(listType)) {
            targetList = config.getStringList("disabled_slime_honey_interactions");
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid list type. Use 'blocks' or 'slime_honey'.");
            return;
        }

        boolean modified = false;
        if ("add".equalsIgnoreCase(action)) {
            if (!targetList.contains(materialName)) {
                targetList.add(materialName);
                modified = true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + materialName + " is already in the list.");
            }
        } else if ("remove".equalsIgnoreCase(action)) {
            modified = targetList.remove(materialName);
            if (modified) {
                sender.sendMessage(ChatColor.GREEN + materialName + " has been removed from the list.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + materialName + " was not found in the list.");
            }
        }

        if (modified) {
            if ("blocks".equalsIgnoreCase(listType)) {
                config.set("disabled_blocks", targetList);
            } else {
                config.set("disabled_slime_honey_interactions", targetList);
            }
            plugin.saveConfig();
            plugin.loadDisabledBlocks(); // Reload disabled blocks
            plugin.loadDisabledSlimeHoneyInteractions(); // Reload disabled slime/honey interactions
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list", "reload");
        } else if (args.length == 2) {
            return List.of("blocks", "slime_honey");
        } else if (args.length == 3) {
            return getAllMaterialNames().stream().filter(s -> s.startsWith(args[2].toUpperCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> getAllMaterialNames() {
        return java.util.Arrays.stream(Material.values()).map(Enum::name).collect(Collectors.toList());
    }
}
