package io.github.angelclient.client.mod.impl;

import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class FastRecraftMod extends Mod {

    public static final FastRecraftMod INSTANCE = new FastRecraftMod();

    private ItemStack[] lastCraftingGrid = new ItemStack[9];
    private boolean hasSavedRecipe = false;

    @Override
    public String getId() {
        return "fast_recraft";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.UTILITY;
    }

    public void onCraftingGuiOpen(GuiCrafting gui) {
        if (!isEnabled()) return;
        if (!hasSavedRecipe) return;
        if (!(gui.inventorySlots instanceof ContainerWorkbench)) return;
        applyLastRecipe((ContainerWorkbench) gui.inventorySlots);
    }

    public void onCraftingGuiClose(GuiCrafting gui) {
        if (!isEnabled()) return;
        if (!(gui.inventorySlots instanceof ContainerWorkbench)) return;
        saveCurrentGrid((ContainerWorkbench) gui.inventorySlots);
    }

    private void saveCurrentGrid(ContainerWorkbench container) {
        IInventory craftMatrix = container.craftMatrix;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftMatrix.getStackInSlot(i);
            lastCraftingGrid[i] = stack != null ? stack.copy() : null;
        }
        hasSavedRecipe = true;
    }

    private void applyLastRecipe(ContainerWorkbench container) {
        for (int i = 0; i < 9; i++) {
            if (lastCraftingGrid[i] != null) {
                container.craftMatrix.setInventorySlotContents(i, lastCraftingGrid[i].copy());
            }
        }
        container.onCraftMatrixChanged(container.craftMatrix);
    }
}