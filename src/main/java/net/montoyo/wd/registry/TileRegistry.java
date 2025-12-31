package net.montoyo.wd.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.montoyo.wd.entity.*;

public class TileRegistry {
    public static final DeferredRegister<BlockEntityType<?>> TILE_TYPES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, "webdisplays");

    //Register tile entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScreenBlockEntity>> SCREEN_BLOCK_ENTITY = TILE_TYPES
            .register("screen", () -> BlockEntityType.Builder
                    .of(ScreenBlockEntity::new, BlockRegistry.SCREEN_BLOCk.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KeyboardBlockEntity>> KEYBOARD = TILE_TYPES.register("kb_left", () -> BlockEntityType.Builder
            .of(KeyboardBlockEntity::new, BlockRegistry.KEYBOARD_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RemoteControlBlockEntity>> REMOTE_CONTROLLER = TILE_TYPES.register("rctrl",
            () -> BlockEntityType.Builder.of(RemoteControlBlockEntity::new, BlockRegistry.REMOTE_CONTROLLER_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneControlBlockEntity>> REDSTONE_CONTROLLER = TILE_TYPES.register("redctrl",
            () -> BlockEntityType.Builder.of(RedstoneControlBlockEntity::new, BlockRegistry.REDSTONE_CONTROL_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerBlockEntity>> SERVER = TILE_TYPES.register("server",
            () -> BlockEntityType.Builder.of(ServerBlockEntity::new, BlockRegistry.SERVER_BLOCK.get()).build(null));

    public static void init(IEventBus bus) {
        TILE_TYPES.register(bus);
    }
}
