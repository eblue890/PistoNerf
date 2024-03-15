package com.bluah.pistonerf;

import org.bukkit.Material;

public enum CropBlocks {
    PUMPKIN(Material.PUMPKIN),
    MELON(Material.MELON),
    BAMBOO(Material.BAMBOO),
    WHEAT(Material.WHEAT),
    SUGAR_CANE(Material.SUGAR_CANE),
    BEETROOTS(Material.BEETROOTS),
    POTATOES(Material.POTATOES),
    BAMBOO_SAPLING(Material.BAMBOO_SAPLING);

    private final Material material;

    CropBlocks(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return this.material;
    }

    public static boolean isCrop(Material material) {
        for (CropBlocks crop : CropBlocks.values()) {
            if (crop.getMaterial().equals(material)) {
                return true;
            }
        }
        return false;
    }
}
