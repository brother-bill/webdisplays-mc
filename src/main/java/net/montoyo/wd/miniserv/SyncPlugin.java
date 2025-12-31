package net.montoyo.wd.miniserv;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncPlugin {
    private static final Map<ServerPlayer, List<ServerPlayer>> syncedPlayers = new HashMap<>();
    private static final Map<ServerPlayer, String> URL = new HashMap<>();

    public static void syncPlayers(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            List<ServerPlayer> syncedPlayersList = new ArrayList<>(players);
            syncedPlayersList.remove(player);
            syncedPlayers.put(player, syncedPlayersList);
        }
    }

    public static String getPlayerString(ServerPlayer player) {
//        if(URL.get(player) == null)
//            setPlayerString(player, new URLMessage().getURL());
        return URL.get(player);
    }

    public static void setPlayerString(ServerPlayer player, String value) {
        URL.put(player, value);
        List<ServerPlayer> syncedPlayersList = syncedPlayers.get(player);
        if (syncedPlayersList != null) {
            for (ServerPlayer syncedPlayer : syncedPlayersList) {
                if(!syncedPlayer.hasDisconnected()) {
//                    WDNetworkRegistry.INSTANCE.sendToServer(new SMessageGetUrl(URL.get(syncedPlayer)));
                }
            }
        }
    }
}