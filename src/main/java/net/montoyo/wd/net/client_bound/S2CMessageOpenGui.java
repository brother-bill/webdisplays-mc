/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.data.GuiData;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.utilities.Log;

public class S2CMessageOpenGui extends Packet {
    public static final CustomPacketPayload.Type<S2CMessageOpenGui> TYPE =
            new CustomPacketPayload.Type<>(id("open_gui"));

    public static final StreamCodec<FriendlyByteBuf, S2CMessageOpenGui> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    S2CMessageOpenGui::new
            );

    private GuiData data;

    public S2CMessageOpenGui(GuiData data) {
        this.data = data;
    }

    public S2CMessageOpenGui(FriendlyByteBuf buf) {
        super(buf);

        String name = buf.readUtf();
        data = GuiData.read(name, buf);
        Class<? extends GuiData> cls = GuiData.classOf(name);

        if (cls == null) {
            Log.error("Could not create GuiData of type %s because it doesn't exist!", name);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(data.getName());
        data.serialize(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext ctx) {
        if (isClient(ctx)) {
            ctx.enqueueWork(() -> WebDisplays.PROXY.displayGui(data));
        }
    }
}
