/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

public class S2CMessageACResult extends Packet {
    public static final CustomPacketPayload.Type<S2CMessageACResult> TYPE =
            new CustomPacketPayload.Type<>(id("ac_result"));

    public static final StreamCodec<FriendlyByteBuf, S2CMessageACResult> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    S2CMessageACResult::new
            );

    private NameUUIDPair[] result;

    public S2CMessageACResult(NameUUIDPair[] pairs) {
        result = pairs;
    }

    public S2CMessageACResult(FriendlyByteBuf buf) {
        super(buf);

        int cnt = buf.readByte();
        result = new NameUUIDPair[cnt];

        for (int i = 0; i < cnt; i++)
            result[i] = new NameUUIDPair(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(result.length);

        for (NameUUIDPair pair : result)
            pair.writeTo(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext ctx) {
        if (isClient(ctx)) {
            ctx.enqueueWork(() -> {
                WebDisplays.PROXY.onAutocompleteResult(result);
            });
        }
    }
}
