package net.montoyo.wd.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.config.annoconfg.AnnoCFG;
import net.montoyo.wd.config.annoconfg.annotation.format.*;
import net.montoyo.wd.config.annoconfg.annotation.value.Default;
import net.montoyo.wd.config.annoconfg.annotation.value.DoubleRange;
import net.montoyo.wd.config.annoconfg.annotation.value.IntRange;

@Config(type = ModConfig.Type.CLIENT)
public class ClientConfig {
	@SuppressWarnings("unused")
	private static AnnoCFG CFG;

	public static void init(IEventBus bus) {
		CFG = new AnnoCFG(bus, ClientConfig.class);
	}

	@Name("load_distance")
	@Comment("How far (in blocks) you can be before a screen starts rendering")
	@Translation("config.webdisplays.load_distance")
	@DoubleRange(minV = 0, maxV = Double.MAX_VALUE)
	@Default(valueD = 30)
	public static double loadDistance = 30.0;

	@Name("unload_distance")
	@Comment("How far you can be before a screen stops rendering")
	@Translation("config.webdisplays.unload_distance")
	@DoubleRange(minV = 0, maxV = Double.MAX_VALUE)
	@Default(valueD = 32)
	public static double unloadDistance = 32.0;

	@Name("pad_resolution")
	@Comment({
			"The resolution that minePads should use",
			"Smaller values produce lower qualities, higher values produce higher qualities",
			"Due to how web browsers work however, the larger this value is, the smaller text is",
			"Also, higher values will invariably lag more",
			"A good goto value for this would be the height of your monitor, in pixels",
			"A standard monitor is (at least currently) 1080",
	})
	@Translation("config.webdisplays.pad_res")
	@IntRange(minV = 0, maxV = Integer.MAX_VALUE)
	@Default(valueI = 720)
	public static int padResolution = 720;

	@Name("screen_brightness")
	@Comment({
			"Controls the brightness of WebDisplays screens (0.0 to 1.0)",
			"0.0 = completely dark, 0.5 = 50% brightness, 1.0 = full brightness",
			"Lower values help when using shaders that make screens too bright",
	})
	@Translation("config.webdisplays.screen_brightness")
	@DoubleRange(minV = 0.0, maxV = 1.0)
	@Default(valueD = 1.0)
	public static double screenBrightness = 1.0;

	@Name("side_pad")
	@Comment({
			"When this is true, the minePad is placed off to the side of the screen when held, so it's visible but doesn't take up too much of the screen",
			"When this is false, the minePad is placed closer to the center of the screen, allow it to be seen better, but taking up more of your view",
	})
	@Translation("config.webdisplays.side_pad")
	@Default(valueBoolean = true)
	public static boolean sidePad = true;

	@Comment({
			"Options relating to input handling"
	})
	@CFGSegment("input")
	public static class Input {
		@Name("keyboard_camera")
		@Comment({
				"If this is on, then the camera will try to focus on the selected element while a keyboard is in use",
				"Elsewise, it'll try to focus on the center of the screen",
		})
		@Translation("config.webdisplays.keyboard_camera")
		@Default(valueBoolean = true)
		public static boolean keyboardCamera = true;

		@Name("switch_buttons")
		@Comment("If the left and right buttons should be swapped when using a laser")
		@Translation("config.webdisplays.switch_buttons")
		@DoubleRange(minV = 0, maxV = Double.MAX_VALUE)
		@Default(valueD = 30)
		public static boolean switchButtons = true;
	}

	@SuppressWarnings("unused")
	public static void postLoad() {
		if (unloadDistance < loadDistance + 2.0)
			unloadDistance = loadDistance + 2.0;

		// cache pad resolution
		WebDisplays.INSTANCE.padResY = padResolution;
		WebDisplays.INSTANCE.padResX = WebDisplays.INSTANCE.padResY * WebDisplays.PAD_RATIO;

		// cache unload/load distances
		WebDisplays.INSTANCE.unloadDistance2 = unloadDistance * unloadDistance;
		WebDisplays.INSTANCE.loadDistance2 = loadDistance * loadDistance;
	}
}
