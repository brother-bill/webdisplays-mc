/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.loading;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.montoyo.wd.client.gui.controls.*;
import net.montoyo.wd.utilities.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class GuiLoader {

    private static final HashMap<String, Class<? extends Control>> CONTROLS = new HashMap<>();
    private static final HashMap<ResourceLocation, JsonObject> RESOURCES = new HashMap<>();

    public static void register(Class<? extends Control> cls) {
        if(Modifier.isAbstract(cls.getModifiers()))
            throw new RuntimeException("GG retard, you just registered an abstract class...");

        String name = cls.getSimpleName();
        if(CONTROLS.containsKey(name))
            throw new RuntimeException("Control class already registered or name taken!");

        CONTROLS.put(name, cls);
    }

    static {
        register(Button.class);
        register(CheckBox.class);
        register(ControlGroup.class);
        register(Label.class);
        register(List.class);
        register(TextField.class);
        register(Icon.class);
        register(UpgradeGroup.class);
        register(YTButton.class);
    }

    public static Control create(JsonOWrapper json) {
        Control ret;

        try {
            ret = CONTROLS.get(json.getString("type", null)).newInstance();
        } catch(InstantiationException e) {
            Log.errorEx("Could not create control from JSON: instantiation exception", e);
            throw new RuntimeException(e);
        } catch(IllegalAccessException e) {
            Log.errorEx("Could not create control from JSON: access denied", e);
            throw new RuntimeException(e);
        }

        ret.load(json);
        return ret;
    }

    public static JsonObject getJson(ResourceLocation resLoc) throws IOException {
        JsonObject ret = RESOURCES.get(resLoc);
        if(ret == null) {
            Resource resource;

            resource = Minecraft.getInstance().getResourceManager().getResource(resLoc).get();

            JsonParser parser = new JsonParser();
            ret = parser.parse(new InputStreamReader(resource.open())).getAsJsonObject();

            RESOURCES.put(resLoc, ret);
        }

        return ret;
    }

    public static void clearCache() {
        RESOURCES.clear();
    }

}
