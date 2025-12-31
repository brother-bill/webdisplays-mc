/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageOpenGui;

import java.util.HashMap;
import java.util.function.Supplier;

public abstract class GuiData {
    public static GuiData read(String name, FriendlyByteBuf buf) {
        GuiType type = dataTable.get(name);
        GuiData data = type.create();
        data.deserialize(buf);
        return data;
    }

    protected static class GuiType {
        Class<? extends GuiData> clazz;
        Supplier<GuiData> constructor;

        public GuiType(Class<? extends GuiData> clazz, Supplier<GuiData> constructor) {
            this.clazz = clazz;
            this.constructor = constructor;
        }

        public GuiData create() {
            return constructor.get();
        }
    }

    private static final HashMap<String, GuiType> dataTable = new HashMap<>();

    static {
        dataTable.put("SetURL", new GuiType(SetURLData.class, SetURLData::new));
        dataTable.put("ScreenConfig", new GuiType(ScreenConfigData.class, ScreenConfigData::new));
        dataTable.put("Keyboard", new GuiType(KeyboardData.class, KeyboardData::new));
        dataTable.put("RedstoneCtrl", new GuiType(RedstoneCtrlData.class, RedstoneCtrlData::new));
        dataTable.put("Server", new GuiType(ServerData.class, ServerData::new));
    }

    public static Class<? extends GuiData> classOf(String name) {
        return dataTable.get(name).clazz;
    }

    public GuiData() {
    }

    @OnlyIn(Dist.CLIENT)
    public abstract Screen createGui(Screen old, Level world);

    public abstract String getName();

    public void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new S2CMessageOpenGui(this));
    }

    public abstract void serialize(FriendlyByteBuf buf);

    public abstract void deserialize(FriendlyByteBuf buf);
}
