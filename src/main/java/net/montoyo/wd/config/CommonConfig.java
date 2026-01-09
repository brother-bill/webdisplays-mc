package net.montoyo.wd.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.config.annoconfg.AnnoCFG;
import net.montoyo.wd.config.annoconfg.annotation.format.*;
import net.montoyo.wd.config.annoconfg.annotation.value.Default;
import net.montoyo.wd.config.annoconfg.annotation.value.IntRange;
import net.montoyo.wd.config.annoconfg.annotation.value.LongRange;

@SuppressWarnings("DefaultAnnotationParam")
@Config(type = ModConfig.Type.COMMON)
public class CommonConfig {
	@SuppressWarnings("unused")
	private static AnnoCFG CFG;

	public static void init(IEventBus bus) {
		CFG = new AnnoCFG(bus, CommonConfig.class);
	}

	@Name("hard_recipes")
	@Comment("If true, breaking the minePad is required to craft upgrades.")
	@Translation("config.webdisplays.hard_recipes")
	@Default(valueBoolean = true)
	public static boolean hardRecipes = true;

	@Name("join_message")
	@Comment("Whether or not webdisplays should thank the user for using the mod")
	@Translation("config.webdisplays.join_message")
	@Default(valueBoolean = true)
	public static boolean joinMessage = true;

	@Name("disable_ownership_thief")
	@Comment("If true, the ownership thief item will be disabled")
	@Translation("config.webdisplays.disable_thief")
	@Default(valueBoolean = false)
	public static boolean disableOwnershipThief = false;

	@Comment("Options for the browsers (both the minePad and the screens)")
	@CFGSegment("browser_options")
	public static class Browser {
		@Name("blacklist")
		@Comment("The page which screens should open up to when turning on")
		@Translation("config.webdisplays.blacklist")
		@Default(valueStr = "")
		public static String[] blacklist = new String[0];

		@Name("home_page")
		@Comment("The page which screens should open up to when turning on")
		@Translation("config.webdisplays.home_page")
		@Default(valueStr = "mod://webdisplays/main.html")
		public static String homepage = "mod://webdisplays/main.html";
	}

	@Comment("Options for video playback synchronization across players")
	@CFGSegment("video_sync")
	public static class VideoSync {
		@Name("enabled")
		@Comment("Enable video playback synchronization across all players viewing the same screen")
		@Translation("config.webdisplays.sync_enabled")
		@Default(valueBoolean = true)
		public static boolean enabled = true;

		@Name("sync_whitelist")
		@Comment("List of domains where video sync is enabled. Use * to enable for all sites. Examples: youtube.com, hianime.to")
		@Translation("config.webdisplays.sync_whitelist")
		@Default(valueStr = "youtube.com,youtu.be,hianime.to,9animetv.to,9anime.to,aniwave.to")
		public static String[] syncWhitelist = new String[]{"youtube.com", "youtu.be", "hianime.to", "9animetv.to", "9anime.to", "aniwave.to"};

		@Name("sync_tolerance_seconds")
		@Comment("Time difference (in seconds) before forcing a seek to sync. Lower = stricter sync but more seeking")
		@Translation("config.webdisplays.sync_tolerance")
		@IntRange(minV = 1, maxV = 30)
		@Default(valueI = 3)
		public static int syncToleranceSeconds = 3;

		@Name("sync_interval_ms")
		@Comment("How often (in milliseconds) the master player broadcasts their playback time")
		@Translation("config.webdisplays.sync_interval")
		@IntRange(minV = 500, maxV = 10000)
		@Default(valueI = 2000)
		public static int syncIntervalMs = 2000;
	}

	@Comment("Options for the in world screen blocks")
	@CFGSegment("screen_options")
	public static class Screen {
		@Name("max_resolution_x")
		@Comment("The maximum value screen's horizontal resolution, in pixels")
		@Translation("config.webdisplays.max_res_x")
		@IntRange(minV = 0, maxV = Integer.MAX_VALUE)
		@Default(valueI = 1920)
		public static int maxResolutionX = 1920;

		@Name("max_resolution_y")
		@Comment("The maximum value screen's vertical resolution, in pixels")
		@Translation("config.webdisplays.max_res_y")
		@IntRange(minV = 0, maxV = Integer.MAX_VALUE)
		@Default(valueI = 1080)
		public static int maxResolutionY = 1080;

		@Name("max_width")
		@Comment("The maximum width for the screen multiblock, in blocks")
		@Translation("config.webdisplays.max_width")
		@IntRange(minV = 0, maxV = Integer.MAX_VALUE)
		@Default(valueI = 16)
		public static int maxScreenSizeX = 16;

		@Name("max_height")
		@Comment("The maximum height for the screen multiblock, in blocks")
		@Translation("config.webdisplays.max_height")
		@IntRange(minV = 0, maxV = Integer.MAX_VALUE)
		@Default(valueI = 16)
		public static int maxScreenSizeY = 16;
	}

	@Comment("Options for the miniserver")
	@CFGSegment("mini_server")
	public static class MiniServ {
		@Name("miniserv_port")
		@Comment("The port used by miniserv. 0 to disable")
		@Translation("config.webdisplays.miniserv_port")
		@IntRange(minV = 0, maxV = Short.MAX_VALUE)
		@Default(valueI = 25566)
		public static int miniservPort = 25566;

		@Name("miniserv_quota")
		@Comment("The amount of data that can be uploaded to miniserv, in KiB (so 1024 = 1 MiO)")
		@Translation("config.webdisplays.miniserv_quota")
		@LongRange(minV = 0, maxV = Long.MAX_VALUE)
		@Default(valueL = 1920)
		public static long miniservQuota = 1024; //It's stored as a string anyway
	}

	@SuppressWarnings("unused")
	public static void postLoad() {
		WebDisplays.INSTANCE.miniservPort = MiniServ.miniservPort;
		WebDisplays.INSTANCE.miniservQuota = MiniServ.miniservQuota * 1024L;
	}
}
