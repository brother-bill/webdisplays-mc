package net.montoyo.wd.config.annoconfg.annotation.format;

import net.neoforged.fml.config.ModConfig;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
	ModConfig.Type type();
	String path() default "";
}
