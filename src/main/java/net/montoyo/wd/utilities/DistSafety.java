package net.montoyo.wd.utilities;

import net.minecraft.client.Minecraft;
import net.montoyo.wd.client.ClientProxy;

public class DistSafety {
    public static ClientProxy createProxy() {
        return new ClientProxy();
    }

    public static boolean isConnected() {
        if (Minecraft.getInstance().getConnection() == null) return false;
        if (Minecraft.getInstance().getConnection().getConnection().isConnecting()) return false;
        return Minecraft.getInstance().getConnection().getConnection().isConnected();
    }
}
