/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.montoyo.wd.client.gui.loading.GuiLoader;
import net.montoyo.wd.client.gui.loading.JsonAWrapper;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;

import java.util.ArrayList;

public abstract class Container extends BasicControl {

    protected int paddingX = 0;
    protected int paddingY = 0;
    protected final ArrayList<Control> childs = new ArrayList<>();

    public <T extends Control> T addControl(T ctrl) {
        childs.add(ctrl);
        return ctrl;
    }

    @Override
    public boolean keyTyped(int keyCode, int modifiers) {
        boolean typed = false;

        if(!disabled) {
            for(Control ctrl : childs)
                typed = typed || ctrl.keyTyped(keyCode, modifiers);
        }

        return typed;
    }

    @Override
    public boolean keyUp(int key, int scanCode, int modifiers) {
        boolean up = false;

        if(!disabled) {
            for(Control ctrl : childs)
                up = up || ctrl.keyUp(key, scanCode, modifiers);
        }

        return up;
    }

    @Override
    public boolean keyDown(int key, int scanCode, int modifiers) {
        boolean down = false;

        if(!disabled) {
            for(Control ctrl : childs)
                down = down || ctrl.keyDown(key, scanCode, modifiers);
        }

        return down;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean clicked = false;

        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs) {
                clicked = ctrl.mouseClicked(mouseX, mouseY, mouseButton);
                if (clicked) break; // don't assume compiler optimizations
            }
        }

        return clicked;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        boolean released = false;

        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs)
                released = released || ctrl.mouseReleased(mouseX, mouseY, state);
        }

        return released;
    }

    @Override
    public boolean mouseClickMove(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean clicked = false;

        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs)
                clicked = clicked || ctrl.mouseClickMove(mouseX, mouseY, button, dragX, dragY);
        }

        return clicked;
    }

    @Override
    public boolean mouseMove(double mouseX, double mouseY) {
        boolean clicked = false;

        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;


            for(Control ctrl : childs)
                clicked = clicked || ctrl.mouseMove(mouseX, mouseY);
        }

        return clicked;
    }

    @Override
    public boolean mouseScroll(double mouseX, double mouseY, double amount) {
        boolean scrolled = false;

        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs)
                scrolled = scrolled || ctrl.mouseScroll(mouseX, mouseY, amount);
        }

        return scrolled;
    }

    @Override
    public void draw(GuiGraphics poseStack, int mouseX, int mouseY, float ptt) {
        if(visible) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            poseStack.pose().pushPose();
            poseStack.pose().translate(x + paddingX, y + paddingY, 0.0);

            if(disabled) {
                for(Control ctrl : childs)
                    ctrl.draw(poseStack, -1, -1, ptt);
            } else {
                for(Control ctrl : childs)
                    ctrl.draw(poseStack, mouseX, mouseY, ptt);
            }

            poseStack.pose().popPose();
        }
    }

    @Override
    public void destroy() {
        for(Control ctrl : childs)
            ctrl.destroy();
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);

        JsonAWrapper objs = json.getArray("childs");
        for(int i = 0; i < objs.size(); i++)
            childs.add(GuiLoader.create(objs.getObject(i)));
    }

    public Control getByName(String name) {
        for(Control ctrl : childs) {
            if(name.equals(ctrl.name))
                return ctrl;

            if(ctrl instanceof Container) {
                Control ret = ((Container) ctrl).getByName(name);

                if(ret != null)
                    return ret;
            }
        }

        return null;
    }

    @Override
    public void unfocus() {
        for (Control control : childs) {
            control.unfocus();
        }
    }
}
