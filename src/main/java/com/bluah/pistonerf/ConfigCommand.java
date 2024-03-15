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
            sender.sendMessage(ChatColor.RED + "Usage: /pistonerf add|remove|list|reload <material>");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "reload":
                plugin.reloadConfig();
                plugin.loadDisabledBlocks(); // Reload the blocks disabled for interaction
                sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                return true;
            case "list":
                List<String> disabledBlocks = plugin.getConfig().getStringList("disabled_blocks");
                if (disabledBlocks.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "No blocks are currently disabled.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Disabled blocks:");
                    disabledBlocks.forEach(blockName -> sender.sendMessage(ChatColor.GREEN + "- " + blockName));
                }
                return true;
            case "add":
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " " + subcommand + " <material>");
                    return true;
                }
                handleMaterialModification(subcommand, args[1], sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid command. Use add, remove, list, or reload.");
                return true;
        }
    }

    private void handleMaterialModification(String action, String materialNameInput, CommandSender sender) {
        String materialName = materialNameInput.toUpperCase();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            sender.sendMessage(ChatColor.RED + "Invalid material name: " + materialName);
            return;
        }

        FileConfiguration config = plugin.getConfig();
        List<String> disabledBlocks = config.getStringList("disabled_blocks");

        if ("add".equalsIgnoreCase(action)) {
            if (!disabledBlocks.contains(materialName)) {
                disabledBlocks.add(materialName);
                sender.sendMessage(ChatColor.GREEN + materialName + " has been added to the disabled blocks list.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + materialName + " is already in the disabled blocks list.");
            }
        } else if ("remove".equalsIgnoreCase(action)) {
            if (disabledBlocks.remove(materialName)) {
                sender.sendMessage(ChatColor.GREEN + materialName + " has been removed from the disabled blocks list.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + materialName + " is not in the disabled blocks list.");
            }
        }

        config.set("disabled_blocks", disabledBlocks);
        plugin.saveConfig();
        plugin.loadDisabledBlocks(); // Reload the updated list of disabled blocks
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list", "reload");
        } else if (args.length == 2) {
            return getAllMaterialNames().stream().filter(s -> s.startsWith(args[1].toUpperCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> getAllMaterialNames() {
        return java.util.Arrays.stream(Material.values()).map(Enum::name).collect(Collectors.toList());
    }
}
