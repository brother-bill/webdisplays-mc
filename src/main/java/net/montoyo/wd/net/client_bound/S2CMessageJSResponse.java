/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.utilities.Log;

public class S2CMessageJSResponse extends Packet {
    public static final CustomPacketPayload.Type<S2CMessageJSResponse> TYPE =
            new CustomPacketPayload.Type<>(id("js_response"));

    public static final StreamCodec<FriendlyByteBuf, S2CMessageJSResponse> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    S2CMessageJSResponse::new
            );

    private int id;
    private JSServerRequest type;
    private boolean success;
    private byte[] data;
    private int errCode;
    private String errString;

    public S2CMessageJSResponse(int id, JSServerRequest t, byte[] d) {
        this.id = id;
        type = t;
        success = true;
        data = d;
    }

    public S2CMessageJSResponse(int id, JSServerRequest t, int code, String err) {
        this.id = id;
        type = t;
        success = false;
        errCode = code;
        errString = err;
    }

    public S2CMessageJSResponse(FriendlyByteBuf buf) {
        super(buf);

        id = buf.readInt();
        type = JSServerRequest.fromID(buf.readByte());
        success = buf.readBoolean();

        if (success) {
            data = new byte[buf.readByte()];
            buf.readBytes(data);
        } else {
            errCode = buf.readInt();
            errString = buf.readUtf();
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeByte(type.ordinal());
        buf.writeBoolean(success);

        if (success) {
            buf.writeByte(data.length);
            buf.writeBytes(data);
        } else {
            buf.writeInt(errCode);
            buf.writeUtf(errString);
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
                try {
                    if (success)
                        WebDisplays.PROXY.handleJSResponseSuccess(id, type, data);
                    else
                        WebDisplays.PROXY.handleJSResponseError(id, type, errCode, errString);
                } catch (Throwable t) {
                    Log.warningEx("Could not handle JS response", t);
                }
            });
        }
    }
}
