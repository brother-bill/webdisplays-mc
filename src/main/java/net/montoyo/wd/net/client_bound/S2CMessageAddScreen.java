/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

import java.io.IOException;
import java.util.ArrayList;

import static net.montoyo.wd.block.ScreenBlock.hasTE;

public class S2CMessageAddScreen extends Packet {
    public static final CustomPacketPayload.Type<S2CMessageAddScreen> TYPE =
            new CustomPacketPayload.Type<>(id("add_screen"));

    public static final StreamCodec<FriendlyByteBuf, S2CMessageAddScreen> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    S2CMessageAddScreen::new
            );

    private boolean clear;
    private Vector3i pos;
    private ScreenData[] screens;

    public S2CMessageAddScreen(ScreenBlockEntity tes) {
        clear = true;
        pos = new Vector3i(tes.getBlockPos());
        screens = new ScreenData[tes.screenCount()];

        for (int i = 0; i < tes.screenCount(); i++)
            screens[i] = tes.getScreen(i);
    }

    public S2CMessageAddScreen(ScreenBlockEntity tes, ScreenData... toSend) {
        clear = false;
        pos = new Vector3i(tes.getBlockPos());
        screens = toSend;
    }

    public S2CMessageAddScreen(boolean clear, Vector3i pos, ScreenData[] screens) {
        this.clear = clear;
        this.pos = pos;
        this.screens = screens;
    }

    public S2CMessageAddScreen(FriendlyByteBuf buf) {
        super(buf);

        clear = buf.readBoolean();
        pos = new Vector3i(buf);

        int cnt = buf.readByte() & 7;

        screens = new ScreenData[cnt];
        for (int i = 0; i < cnt; i++) {
            screens[i] = new ScreenData();
            screens[i].side = BlockSide.values()[buf.readByte()];
            screens[i].size = new Vector2i(buf);
            screens[i].url = buf.readUtf();
            screens[i].resolution = new Vector2i(buf);
            screens[i].rotation = Rotation.values()[buf.readByte() & 3];
            screens[i].owner = new NameUUIDPair(buf);
            screens[i].upgrades = new ArrayList<>();

            int numUpgrades = buf.readByte();
            for (int j = 0; j < numUpgrades; j++)
                screens[i].upgrades.add(net.montoyo.wd.utilities.serialization.Util.readItemStack(buf));
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(clear);
        pos.writeTo(buf);
        buf.writeByte(screens.length);

        for (ScreenData scr : screens) {
            buf.writeByte(scr.side.ordinal());
            scr.size.writeTo(buf);
            buf.writeUtf(scr.url);
            scr.resolution.writeTo(buf);
            buf.writeByte(scr.rotation.ordinal());
            scr.owner.writeTo(buf);
            buf.writeByte(scr.upgrades.size());

            for (ItemStack is : scr.upgrades)
                net.montoyo.wd.utilities.serialization.Util.writeItemStack(buf, is);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext ctx) {
        if (isClient(ctx)) {
            ctx.enqueueWork(() -> {
                Level lvl = ctx.player().level();
                BlockEntity te = lvl.getBlockEntity(pos.toBlock());
                if (!(te instanceof ScreenBlockEntity)) {
                    lvl.setBlockAndUpdate(pos.toBlock(), lvl.getBlockState(pos.toBlock()).setValue(hasTE, true));
                    te = lvl.getBlockEntity(pos.toBlock());

                    if (!(te instanceof ScreenBlockEntity)) {
                        if (clear)
                            Log.error("CMessageAddScreen: Can't add screen to invalid tile entity at %s", pos.toString());

                        return;
                    }
                }

                ScreenBlockEntity tes = (ScreenBlockEntity) te;
                if (clear)
                    tes.clear();

                for (ScreenData entry : screens) {
                    ScreenData scr = tes.addScreen(entry.side, entry.size, entry.resolution, null, false);
                    scr.rotation = entry.rotation;
                    String webUrl;

                    try {
                        webUrl = ScreenBlockEntity.url(entry.url);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    scr.url = webUrl;
                    scr.owner = entry.owner;
                    scr.upgrades = entry.upgrades;

                    if (scr.browser != null)
                        scr.browser.loadURL(webUrl);
                }
            });
        }
    }
}
