package net.montoyo.wd.controls;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

public class ScreenControlType<T extends ScreenControl> {
	Class<T> clazz;
	Function<FriendlyByteBuf, ScreenControl> deserializer;
	
	public ScreenControlType(Class<T> clazz, Function<FriendlyByteBuf, ScreenControl> deserializer) {
		this.clazz = clazz;
		this.deserializer = deserializer;
	}
}