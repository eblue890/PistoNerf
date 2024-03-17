package com.bluah.pistonerf;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
                plugin.reloadConfig(); // Reloads the config from the file
                plugin.loadDisabledBlocks(); // Reload the disabled blocks configuration
                plugin.loadDisabledSlimeHoneyInteractions(); // Reload the disabled slime/honey interactions configuration
                sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                return true;
            case "list":
                // Listing disabled blocks and slime/honey interactions
                displayList(sender, "disabled_blocks", "Disabled blocks:");
                displayList(sender, "disabled_slime_honey_interactions", "Disabled slime/honey interactions:");
                return true;
            case "add":
            case "remove":
                // Handling material addition/removal from the lists
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

    private void displayList(CommandSender sender, String configPath, String message) {
        sender.sendMessage(ChatColor.GREEN + message);
        List<String> list = plugin.getConfig().getStringList(configPath);
        if (list.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No items in the list.");
        } else {
            list.forEach(item -> sender.sendMessage(ChatColor.GREEN + "- " + item));
        }
    }

    private void handleMaterialModification(String action, String listType, String materialNameInput, CommandSender sender) {
        String materialName = materialNameInput.toUpperCase();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            sender.sendMessage(ChatColor.RED + "Invalid material name: " + materialName);
            return;
        }

        List<String> targetList;
        if ("blocks".equalsIgnoreCase(listType)) {
            targetList = plugin.getConfig().getStringList("disabled_blocks");
        } else if ("slime_honey".equalsIgnoreCase(listType)) {
            targetList = plugin.getConfig().getStringList("disabled_slime_honey_interactions");
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid list type. Use 'blocks' or 'slime_honey'.");
            return;
        }

        if ("add".equalsIgnoreCase(action) && !targetList.contains(materialName)) {
            targetList.add(materialName);
            sender.sendMessage(ChatColor.GREEN + materialName + " added to the list.");
        } else if ("remove".equalsIgnoreCase(action) && targetList.remove(materialName)) {
            sender.sendMessage(ChatColor.GREEN + materialName + " removed from the list.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "No changes made. (Item might already be in the list or not found)");
            return;
        }

        // Update the config and save
        if ("blocks".equalsIgnoreCase(listType)) {
            plugin.getConfig().set("disabled_blocks", targetList);
        } else {
            plugin.getConfig().set("disabled_slime_honey_interactions", targetList);
        }
        plugin.saveConfig();
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
