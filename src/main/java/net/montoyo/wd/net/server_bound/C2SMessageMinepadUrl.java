package net.montoyo.wd.net.server_bound;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.item.ItemMinePad2;
import net.montoyo.wd.net.Packet;

import java.util.UUID;

public class C2SMessageMinepadUrl extends Packet {
    public static final CustomPacketPayload.Type<C2SMessageMinepadUrl> TYPE =
            new CustomPacketPayload.Type<>(id("minepad_url"));

    public static final StreamCodec<FriendlyByteBuf, C2SMessageMinepadUrl> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    C2SMessageMinepadUrl::new
            );

    UUID id;
    String url;

    public C2SMessageMinepadUrl(UUID id, String url) {
        this.id = id;
        this.url = url;
    }

    public C2SMessageMinepadUrl(FriendlyByteBuf buf) {
        super(buf);
        this.id = buf.readUUID();
        this.url = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(url);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    protected void merge(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (url.equals("")) {
            tag.remove("PadID");
        } else {
            tag.putUUID("PadID", id);
            tag.putString("PadURL", url);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void handle(IPayloadContext ctx) {
        if (isServer(ctx)) {
            ServerPlayer sender = getSender(ctx);
            if (sender == null) return;

            ctx.enqueueWork(() -> {
                // check if the player is holding a minePad with the requested id
                // if the player is, then update that pad
                for (InteractionHand value : InteractionHand.values()) {
                    ItemStack stack = sender.getItemInHand(value);
                    if (stack.getItem() instanceof ItemMinePad2) {
                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        if (tag.contains("PadID")) {
                            UUID padId = tag.getUUID("PadID");
                            if (padId.equals(id)) {
                                merge(stack);
                                return;
                            }
                        }
                    }
                }

                // if the player is not holding the requested minePad, update the first one that does not already have an ID
                for (InteractionHand value : InteractionHand.values()) {
                    ItemStack stack = sender.getItemInHand(value);
                    if (stack.getItem() instanceof ItemMinePad2) {
                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        if (!tag.contains("PadID")) {
                            merge(stack);
                            return;
                        }
                    }
                }
            });
        }
    }
}
