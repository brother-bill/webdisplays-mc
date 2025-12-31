/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.client.gui.controls.Container;
import net.montoyo.wd.client.gui.controls.Control;
import net.montoyo.wd.client.gui.controls.Event;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.client.gui.loading.GuiLoader;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageACQuery;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.data.Bounds;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class WDScreen extends Screen {

    public static WDScreen CURRENT_SCREEN = null;

    protected final ArrayList<Control> controls = new ArrayList<>();
    protected final ArrayList<Control> postDrawList = new ArrayList<>();
    private final HashMap<Class<? extends Event>, Method> eventMap = new HashMap<>();
    protected boolean quitOnEscape = true;
    protected boolean defaultBackground = true;
    protected int syncTicks = 40;
    private int syncTicksLeft = -1;

    public WDScreen(Component component) {
        super(component);
        Method[] methods = getClass().getMethods();

        for(Method m : methods) {
            if(m.getAnnotation(GuiSubscribe.class) != null) {
                if(!Modifier.isPublic(m.getModifiers()))
                    throw new RuntimeException("Found non public @GuiSubscribe");

                Class<?> params[] = m.getParameterTypes();
                if(params.length != 1 || !Event.class.isAssignableFrom(params[0]))
                    throw new RuntimeException("Invalid parameters for @GuiSubscribe");

                eventMap.put((Class<? extends Event>) params[0], m);
            }
        }
    }

    protected <T extends Control> T addControl(T ctrl) {
        controls.add(ctrl);
        return ctrl;
    }

    public int screen2DisplayX(int x) {
        double ret = ((double) x) / ((double) width) * ((double) minecraft.getWindow().getWidth());
        return (int) ret;
    }

    public int screen2DisplayY(int y) {
        double ret = ((double) y) / ((double) height) * ((double) minecraft.getWindow().getHeight());
        return (int) ret;
    }

    public int display2ScreenX(int x) {
        double ret = ((double) x) / ((double) minecraft.getWindow().getWidth()) * ((double) width);
        return (int) ret;
    }

    public int display2ScreenY(int y) {
        double ret = ((double) y) / ((double) minecraft.getWindow().getHeight()) * ((double) height);
        return (int) ret;
    }

    protected void centerControls() {
        //Determine bounding box
        Bounds bounds = Control.findBounds(controls);

        //Translation vector
        int diffX = (width - bounds.maxX - bounds.minX) / 2;
        int diffY = (height - bounds.maxY - bounds.minY) / 2;

        //Translate controls
        for(Control ctrl : controls) {
            int x = ctrl.getX();
            int y = ctrl.getY();

            ctrl.setPos(x + diffX, y + diffY);
        }
    }

    @Override
    public void render(GuiGraphics poseStack, int mouseX, int mouseY, float ptt) {
        if(defaultBackground)
            renderBackground(poseStack, mouseX, mouseY, ptt);

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        for(Control ctrl: controls)
            ctrl.draw(poseStack, mouseX, mouseY, ptt);

        for(Control ctrl: postDrawList)
            ctrl.postDraw(poseStack, mouseX, mouseY, ptt);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean typed = false;

        for(Control ctrl: controls)
            typed = typed || ctrl.keyTyped(codePoint, modifiers);

        return typed;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = false;

        Control clickedEl = null;
        for(Control ctrl: controls) {
            clicked = ctrl.mouseClicked(mouseX, mouseY, button);
            if (clicked) {
                clickedEl = ctrl;
                break; // don't assume the compiler will optimize stuff
            }
        }

        if (clicked) {
            for (Control control : controls) {
                if (control != clickedEl)
                    control.unfocus();
            }
        }

        return clicked;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean mouseReleased = false;

        for(Control ctrl: controls)
            mouseReleased = mouseReleased || ctrl.mouseReleased(mouseX, mouseY, button);

        return mouseReleased;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean dragged = false;

        for(Control ctrl: controls)
            dragged = dragged || ctrl.mouseClickMove(mouseX, mouseY, button, dragX, dragX);

        return dragged;
    }

    @Override
    protected void init() {
        CURRENT_SCREEN = this;
//        minecraft.keyboardHandler.setSendRepeatsToGui(true);
    }

    @Override
    public void onClose() {
        if(syncTicksLeft >= 0) {
            sync();
            syncTicksLeft = -1;
        }

        for(Control ctrl : controls)
            ctrl.destroy();

//        Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(false);
        CURRENT_SCREEN = null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean scrolled = false;

        for(Control ctrl : controls)
            scrolled = scrolled || ctrl.mouseScroll(mouseX, mouseY, scrollY);

        return scrolled;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        boolean moved = false;

        for(Control ctrl : controls)
            moved = moved || ctrl.mouseMove(mouseX, mouseY);

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean down = false;

        for (Control ctrl : controls)
            down = down || ctrl.keyDown(keyCode, scanCode, modifiers);

        if (this instanceof GuiKeyboard) {
            return down;
        } else {
            return new GuiServer(new Vector3i(), new NameUUIDPair()).keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        boolean up = false;

        for(Control ctrl : controls)
            up = up || ctrl.keyUp(keyCode, scanCode, modifiers);

        return up || super.keyReleased(keyCode, scanCode, modifiers);
    }

    public Object actionPerformed(Event ev) {
        Method m = eventMap.get(ev.getClass());

        if(m != null) {
            try {
                return m.invoke(this, ev);
            } catch(IllegalAccessException e) {
                Log.errorEx("Access to event %s of screen %s is denied", e, ev.getClass().getSimpleName(), getClass().getSimpleName());
            } catch(InvocationTargetException e) {
                Log.errorEx("Event %s of screen %s failed", e, ev.getClass().getSimpleName(), getClass().getSimpleName());
            }
        }

        return null;
    }

    public <T extends Control> T getControlByName(String name) {
        for(Control ctrl : controls) {
            if(name.equals(ctrl.getName()))
                return (T) ctrl;

            if(ctrl instanceof Container) {
                Control ret = ((Container) ctrl).getByName(name);

                if(ret != null)
                    return (T) ret;
            }
        }

        return null;
    }

    protected void addLoadCustomVariables(Map<String, Double> vars) {
    }

    public void loadFrom(ResourceLocation resLoc) {
        try {
            JsonObject root = GuiLoader.getJson(resLoc);

        if(root == null)
            throw new RuntimeException("Could not load GUI file " + resLoc.toString());

        if(!root.has("controls") || !root.get("controls").isJsonArray())
            throw new RuntimeException("In GUI file " + resLoc.toString() + ": missing root 'controls' object.");

        HashMap<String, Double> vars = new HashMap<>();
        vars.put("width", (double) width);
        vars.put("height", (double) height);
        vars.put("displayWidth", (double) minecraft.getWindow().getWidth());
        vars.put("displayHeight", (double) minecraft.getWindow().getHeight());
        addLoadCustomVariables(vars);

        JsonArray content = root.get("controls").getAsJsonArray();
        for(JsonElement elem: content)
            controls.add(GuiLoader.create(new JsonOWrapper(elem.getAsJsonObject(), vars)));

        Field[] fields = getClass().getDeclaredFields();
        for(Field f: fields) {
            f.setAccessible(true);
            FillControl fc = f.getAnnotation(FillControl.class);

            if(fc != null) {
                String name = fc.name().isEmpty() ? f.getName() : fc.name();
                Control ctrl = getControlByName(name);

                if(ctrl == null) {
                    if(fc.required())
                        throw new RuntimeException("In GUI file " + resLoc.toString() + ": missing required control " + name);

                    continue;
                }

                if(!f.getType().isAssignableFrom(ctrl.getClass()))
                    throw new RuntimeException("In GUI file " + resLoc.toString() + ": invalid type for control " + name);

                try {
                    f.set(this, ctrl);
                } catch(IllegalAccessException e) {
                    if(fc.required())
                        throw new RuntimeException(e);
                }
            }
        }

        if(root.has("center") && root.get("center").getAsBoolean())
            centerControls();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        for(Control ctrl : controls)
            ctrl.destroy();

        controls.clear();
        super.resize(minecraft, width, height);
    }

    protected void requestAutocomplete(String beginning, boolean matchExact) {
        WDNetworkRegistry.sendToServer(new C2SMessageACQuery(beginning, matchExact));
    }

    public void onAutocompleteResult(NameUUIDPair pairs[]) {
    }

    public void onAutocompleteFailure() {
    }

    protected void requestSync() {
        syncTicksLeft = syncTicks - 1;
    }

    protected boolean syncRequested() {
        return syncTicksLeft >= 0;
    }

    protected void abortSync() {
        syncTicksLeft = -1;
    }

    protected void sync() {
    }

    @Override
    public void tick() {
        if(syncTicksLeft >= 0) {
            if(--syncTicksLeft < 0)
                sync();
        }
    }

    public void drawItemStackTooltip(GuiGraphics poseStack, ItemStack is, int x, int y) {
        poseStack.renderTooltip(Minecraft.getInstance().font, is, x, y); //Since it's protected...
    }

    public void drawTooltip(GuiGraphics poseStack, List<String> lines, int x, int y) {
        poseStack.renderTooltip(Minecraft.getInstance().font, lines.stream().map(a -> FormattedCharSequence.forward(a, Style.EMPTY)).collect(Collectors.toList()), x, y); //This is also protected...
    }

    public void requirePostDraw(Control ctrl) {
        if(!postDrawList.contains(ctrl))
            postDrawList.add(ctrl);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public abstract boolean isForBlock(BlockPos bp, BlockSide side);

    @Nullable
    public String getWikiPageName() {
        return null;
    }

    //Bypass for needing to use Components

}
