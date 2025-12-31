/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;

import java.util.ArrayList;

public class UpgradeGroup extends BasicControl {

    private int width;
    private int height;
    private ArrayList<ItemStack> upgrades;
    private ItemStack overStack;
    private ItemStack clickStack;
    private final ItemRenderer renderItem = Minecraft.getInstance().getItemRenderer();

    public UpgradeGroup() {
        parent.requirePostDraw(this);
    }

    @Override
    public void draw(GuiGraphics poseStack, int mouseX, int mouseY, float ptt) {
        if(upgrades != null) {
            int x = this.x;

            for(ItemStack is: upgrades) {
                if(is == overStack && !disabled)
                    fillRect(poseStack.bufferSource(), x, y, 16, 16, 0x80FF0000);

                poseStack.renderItem(is, x, y);
                poseStack.renderItemDecorations(font, is, x, y);
                x += 18;
            }
        }
    }

    @Override
    public void postDraw(GuiGraphics poseStack, int mouseX, int mouseY, float ptt) {
        if(overStack != null)
            parent.drawItemStackTooltip(poseStack, overStack, mouseX, mouseY);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setWidth(int w) {
        width = w;
    }

    public void setHeight(int h) {
        height = h;
    }

    public void setUpgrades(ArrayList<ItemStack> upgrades) {
        this.upgrades = upgrades;
    }

    public ArrayList<ItemStack> getUpgrades() {
        return upgrades;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        width = json.getInt("width", 0);
        height = json.getInt("height", 16);
    }

    @Override
    public boolean mouseMove(double mouseX, double mouseY) {
        if(upgrades != null) {
            overStack = null;

            if(mouseY >= y && mouseY <= y + 16 && mouseX >= x) {
                mouseX -= x;
                int sel = (int) (mouseX / 18);

                if(sel < upgrades.size() && mouseX % 18 <= 16)
                    overStack = upgrades.get(sel);

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if(mouseButton == 0) {
            // don't process the click if it's not inbounds, lol
            if (
                    mouseX >= x && mouseX <= x + width &&
                    mouseY >= y && mouseX <= y + height
            ) {
                    clickStack = overStack;
        
                    return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if(state == 0 && clickStack != null) {
            if(clickStack == overStack && !disabled && upgrades.contains(clickStack)) //HOTFIX: Make sure it's actually in the list :p
                parent.actionPerformed(new ClickEvent(this));

            clickStack = null;

            return true;
        }

        return false;
    }

    public ItemStack getMouseOverUpgrade() {
        return overStack;
    }

    public static class ClickEvent extends Event<UpgradeGroup> {

        private final ItemStack clickStack;

        public ClickEvent(UpgradeGroup src) {
            source = src;
            clickStack = src.clickStack;
        }

        public ItemStack getMouseOverStack() {
            return clickStack;
        }

    }

}
