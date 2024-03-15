package com.bluah.pistonerf;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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
                plugin.updateDisabledBlocks();
                sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                return true;
            case "list":
                List<String> disabledBlocks = plugin.getConfig().getStringList("disabled_blocks");
                if (disabledBlocks.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "No blocks are currently disabled.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Disabled blocks:");
                    for (String blockName : disabledBlocks) {
                        sender.sendMessage(ChatColor.GREEN + "- " + blockName);
                    }
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
                sender.sendMessage(ChatColor.RED + "Usage: /pistonerf add|remove|list|reload <material>");
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

        switch (action) {
            case "add":
                if (!disabledBlocks.contains(materialName)) {
                    disabledBlocks.add(materialName);
                    config.set("disabled_blocks", disabledBlocks);
                    plugin.saveConfig();
                    plugin.updateDisabledBlocks();
                    sender.sendMessage(ChatColor.GREEN + materialName + " has been added to the disabled blocks list.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + materialName + " is already in the disabled blocks list.");
                }
                break;
            case "remove":
                if (disabledBlocks.contains(materialName)) {
                    disabledBlocks.remove(materialName);
                    config.set("disabled_blocks", disabledBlocks);
                    plugin.saveConfig();
                    plugin.updateDisabledBlocks();
                    sender.sendMessage(ChatColor.GREEN + materialName + " has been removed from the disabled blocks list.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + materialName + " is not in the disabled blocks list.");
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid action. Use add or remove.");
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("add");
            subCommands.add("remove");
            subCommands.add("reload");
            subCommands.add("list");
            return subCommands.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        } else if (args.length == 2 && ("add".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))) {
            return getAllMaterialNames().stream().filter(s -> s.startsWith(args[1].toUpperCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> getAllMaterialNames() {
        return java.util.Arrays.stream(Material.values()).map(Enum::name).collect(Collectors.toList());
    }
}
