package net.montoyo.wd.controls;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.builtin.*;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.Log;

import java.lang.reflect.Method;
import java.util.HashMap;

// TODO: enable deferred registry of these
public class ScreenControlRegistry {
    private static final HashMap<ResourceLocation, ScreenControlType<?>> CONTROL_TYPES = new HashMap<>();

    public static void register(ResourceLocation name, ScreenControlType<?> type) {
        if (CONTROL_TYPES.containsKey(name)) {
            Log.warning("ScreenControlRegistry#CONTROL_TYPES already contains an entry with name " + name);
            throw new IllegalArgumentException("Cannot have two entries with the same name.");
        }
        CONTROL_TYPES.put(name, type);

        // lil thing for sanity
        // avoids the pain the dist cleaner causes, hopefully
        if (!FMLEnvironment.production) {
            if (FMLEnvironment.dist.isClient()) {
                boolean shouldThrow = false;
                try {
                    Method m = type.clazz.getMethod("handleClient", BlockPos.class, BlockSide.class, ScreenBlockEntity.class, IPayloadContext.class);
                    OnlyIn onlyIn = m.getAnnotation(OnlyIn.class);
                    if (onlyIn == null) shouldThrow = true;
                    Dist d = onlyIn.value(); // idc if this throws, lol
                    if (d != Dist.CLIENT) shouldThrow = true;
                } catch (Throwable ignored) {
                }
                if (shouldThrow) {
                    Log.warning("handleClient on ScreenControl classes MUST be marked with `@OnlyIn(Dist.CLIENT)`, but it is not on " + type.clazz);
                    throw new IllegalStateException(
                            "handleClient on ScreenControl classes MUST be marked with `@OnlyIn(Dist.CLIENT)`, but it is not on " + type.clazz
                    );
                }
            }
        }
    }

    // if needed, the old code
    // https://github.com/Mysticpasta1/webdisplays/blob/ff55cbf1b27773c15f44f17ad3364da3a16b6ed9/src/main/java/net/montoyo/wd/net/server/SMessageScreenCtrl.java#L281-L364
    // https://github.com/Mysticpasta1/webdisplays/blob/5ce9e4574df356910645b0382628f74d1401e26d/src/main/java/net/montoyo/wd/net/client_bound/S2CMessageScreenUpdate.java#L261-L284
    static {
        register(SetURLControl.id, new ScreenControlType<>(SetURLControl.class, SetURLControl::new));
        register(KeyTypedControl.id, new ScreenControlType<>(KeyTypedControl.class, KeyTypedControl::new));
        register(AutoVolumeControl.id, new ScreenControlType<>(AutoVolumeControl.class, AutoVolumeControl::new));
        register(JSRequestControl.id, new ScreenControlType<>(JSRequestControl.class, JSRequestControl::new));
        register(LaserControl.id, new ScreenControlType<>(LaserControl.class, LaserControl::new));
        register(ScreenModifyControl.id, new ScreenControlType<>(ScreenModifyControl.class, ScreenModifyControl::new));
        register(ModifyFriendListControl.id, new ScreenControlType<>(ModifyFriendListControl.class, ModifyFriendListControl::new));
        register(ManageRightsAndUpdgradesControl.id, new ScreenControlType<>(ManageRightsAndUpdgradesControl.class, ManageRightsAndUpdgradesControl::new));
        register(ClickControl.id, new ScreenControlType<>(ClickControl.class, ClickControl::new));
        register(OwnerControl.id, new ScreenControlType<>(OwnerControl.class, OwnerControl::new));
        register(TurnOffControl.id, new ScreenControlType<>(TurnOffControl.class, (buf) -> TurnOffControl.INSTANCE));
    }

    public static ScreenControl parse(FriendlyByteBuf buf) {
        return CONTROL_TYPES.get(ResourceLocation.parse(buf.readUtf()))
                .deserializer.apply(buf);
    }

    public static void init() {
        /* NO-OP: allows static init to run during mod init in dev env */
    }
}
