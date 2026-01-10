/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WDTabs {
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "webdisplays");

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = TABS.register("main", () -> CreativeModeTab.builder()
			// Set name of tab to display
			.title(Component.translatable("itemGroup.webdisplays"))
			// Set icon of creative tab
			.icon(() -> new ItemStack(ItemRegistry.SCREEN.get()))
			// Add default items to tab
			.displayItems((params, output) -> {
				// core items
				output.accept(ItemRegistry.SCREEN.get());
				output.accept(ItemRegistry.KEYBOARD.get());
				output.accept(ItemRegistry.LINKER.get());
				// remote control
				output.accept(ItemRegistry.REMOTE_CONTROLLER.get());
				// redstone stuff
				output.accept(ItemRegistry.REDSTONE_CONTROLLER.get());
				// admin tools
				output.accept(ItemRegistry.OWNERSHIP_THEIF.get());
				// tool items
				output.accept(ItemRegistry.SERVER.get());
				output.accept(ItemRegistry.CONFIGURATOR.get());
				output.accept(ItemRegistry.MINEPAD.get());
				output.accept(ItemRegistry.MINEPAD2.get());
				output.accept(ItemRegistry.LASER_POINTER.get());

				// upgrades
				for (int i = 0; i < ItemRegistry.countUpgrades(); i++) output.accept(ItemRegistry.getUpgradeItem(i).get());
				// cc
				for (int i = 0; i < ItemRegistry.countCompCraftItems(); i++) output.accept(ItemRegistry.getComputerCraftItem(i).get());
			})
			.build()
	);

	public static void init(IEventBus bus) {
		TABS.register(bus);
	}
}
