package net.montoyo.wd.config.annoconfg;

import net.montoyo.wd.config.annoconfg.handle.UnsafeHandle;

import java.util.function.Supplier;

public class ConfigEntry {
	UnsafeHandle handle;
	Supplier<?> supplier;
	
	public ConfigEntry(UnsafeHandle handle, Supplier<?> supplier) {
		this.handle = handle;
		this.supplier = supplier;
	}
}
