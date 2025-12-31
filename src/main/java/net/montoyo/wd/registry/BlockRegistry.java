package net.montoyo.wd.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.montoyo.wd.block.KeyboardBlockLeft;
import net.montoyo.wd.block.KeyboardBlockRight;
import net.montoyo.wd.block.PeripheralBlock;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.DefaultPeripheral;

public class BlockRegistry {
    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "webdisplays");

    public static final DeferredHolder<Block, ScreenBlock> SCREEN_BLOCk = BLOCKS.register("screen", () -> new ScreenBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

    public static final DeferredHolder<Block, KeyboardBlockLeft> KEYBOARD_BLOCK = BlockRegistry.BLOCKS.register("kb_left", KeyboardBlockLeft::new);
    public static final DeferredHolder<Block, KeyboardBlockRight> blockKbRight = BLOCKS.register("kb_right", KeyboardBlockRight::new);

    public static final DeferredHolder<Block, PeripheralBlock> REDSTONE_CONTROL_BLOCK = BlockRegistry.BLOCKS.register("redctrl", () -> new PeripheralBlock(DefaultPeripheral.REDSTONE_CONTROLLER));
    public static final DeferredHolder<Block, PeripheralBlock> REMOTE_CONTROLLER_BLOCK = BlockRegistry.BLOCKS.register("rctrl", () -> new PeripheralBlock(DefaultPeripheral.REMOTE_CONTROLLER));
    public static final DeferredHolder<Block, PeripheralBlock> SERVER_BLOCK = BlockRegistry.BLOCKS.register("server", () -> new PeripheralBlock(DefaultPeripheral.SERVER));
}
