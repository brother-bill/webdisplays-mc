/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

public enum VideoType {
    YOUTUBE(
            "document.getElementById(\"movie_player\").",
            new Function("setVolume(", ")"),
            new Function("getCurrentTime(", ")"),
            new Function("seekTo(", ")"),
            true // supports sync
    ),
    YOUTUBE_EMBED(
            "document.getElementsByClassName(\"html5-video-player\")[0].",
            new Function("setVolume(", ")"),
            new Function("getCurrentTime(", ")"),
            new Function("seekTo(", ")"),
            true // supports sync
    ),
    // Generic HTML5 video - works for most sites with standard video players
    GENERIC_HTML5(
            "(function(){var v=document.querySelector('video');if(!v){var iframes=document.querySelectorAll('iframe');for(var i=0;i<iframes.length;i++){try{v=iframes[i].contentDocument.querySelector('video');if(v)break;}catch(e){}}}return v?v:null;})().",
            new Function("volume=", "/100"),  // HTML5 video uses 0-1 range
            new Function("currentTime", ""),
            new Function("currentTime=", ""),
            true // supports sync
    );

    private final String base;
    private final Function volume;
    private final Function getTime;
    private final Function setTime;
    private final int volumeCap;
    private final boolean supportsSync;

    VideoType(
            String base,
            Function volume,
            Function getTime,
            Function setTime,
            boolean supportsSync
    ) {
        this.base = base;
        this.volume = volume;
        this.getTime = getTime;
        this.setTime = setTime;
        this.supportsSync = supportsSync;
        // lol, what?
        volumeCap = volume.prefix.length() + 5 + volume.suffix.length();
    }

    public boolean supportsSync() {
        return supportsSync;
    }

//    public static void registerQueries(JSQueryDispatcher jsQueryDispatcher) {
//		// TODO: register GetTime query
//    }

    protected static class Function {
        String prefix, suffix;

        public Function(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String apply() {
            return prefix + suffix;
        }

        public String apply(String arg) {
            return prefix + arg + suffix;
        }
    }

    @Nullable
    public static VideoType getTypeFromURL(@Nonnull URL url) {
        String loHost = url.getHost().toLowerCase();

        // Check YouTube first
        if (loHost.equals("youtu.be"))
            return url.getPath().length() > 1 ? YOUTUBE : null;
        else if (loHost.equals("www.youtube.com") || loHost.equals("youtube.com")) {
            String loPath = url.getPath().toLowerCase();
            if (loPath.equals("/watch")) {
                if (url.getQuery() != null && (url.getQuery().startsWith("v=") || url.getQuery().contains("&v=")))
                    return YOUTUBE;
            } else if (loPath.startsWith("/embed/"))
                return loPath.length() > 7 ? YOUTUBE_EMBED : null;
            return null;
        }

        // For other sites, check sync whitelist and return GENERIC_HTML5
        if (isInSyncWhitelist(loHost)) {
            return GENERIC_HTML5;
        }

        return null;
    }

    /**
     * Check if a host is in the video sync whitelist
     */
    public static boolean isInSyncWhitelist(@Nonnull String host) {
        String loHost = host.toLowerCase();
        // Remove www. prefix for matching
        if (loHost.startsWith("www.")) {
            loHost = loHost.substring(4);
        }

        try {
            String[] whitelist = net.montoyo.wd.config.CommonConfig.VideoSync.syncWhitelist;
            for (String domain : whitelist) {
                if (domain.equals("*")) return true;
                String loDomain = domain.toLowerCase().trim();
                if (loDomain.startsWith("www.")) {
                    loDomain = loDomain.substring(4);
                }
                if (loHost.equals(loDomain) || loHost.endsWith("." + loDomain)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Config not loaded yet
        }
        return false;
    }

    /**
     * Check if sync is enabled for a URL
     */
    public static boolean isSyncEnabledForURL(@Nonnull String url) {
        try {
            if (!net.montoyo.wd.config.CommonConfig.VideoSync.enabled) {
                return false;
            }
            URL parsed = new URL(url);
            return isInSyncWhitelist(parsed.getHost());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Nullable
    public static VideoType getTypeFromURL(@Nonnull String url) {
        try {
            return getTypeFromURL(new URL(url));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    @Nonnull
    public String getVideoIDFromURL(@Nonnull URL url) {
        if (this == YOUTUBE) {
            if (url.getHost().equalsIgnoreCase("youtu.be"))
                return url.getPath().substring(1);

            String args[] = url.getQuery().split("&");
            for (String arg : args) {
                if (arg.startsWith("v="))
                    return arg.substring(2);
            }
        } else if (this == YOUTUBE_EMBED)
            return url.getPath().substring(7);

        return "";
    }

    @Nonnull
    public String getURLFromID(@Nonnull String vid, boolean autoplay) {
        String format;
        if (this == YOUTUBE)
            format = autoplay ? "https://www.youtube.com/watch?v=%s&autoplay=1" : "https://www.youtube.com/watch?v=%s";
        else if (this == YOUTUBE_EMBED)
            format = autoplay ? "https://www.youtube.com/embed/%s?autoplay=1" : "https://www.youtube.com/embed/%s";
        else
            return "";

        return String.format(format, vid);
    }

    // Returns a complete JS expression that sets the player's volume.
    // Previously this returned just `setVolume(N)` with no base — i.e. invalid JS that silently
    // did nothing, which is why spatial-audio "worked in Java" but volume never actually changed.
    // Now wraps in try/catch and prepends the player base, matching getSeekJS / getPlayPauseJS pattern.
    @Nonnull
    public String getVolumeJSQuery(int volInt, int volFrac) {
        String vol = volInt + "." + volFrac;
        if (this == YOUTUBE || this == YOUTUBE_EMBED) {
            // YouTube setVolume takes 0-100 integer (decimals are rounded down).
            // Chain to genericHTML5JS so embedded media (the embedded player IS a video element)
            // also gets attenuated as belt-and-suspenders.
            return "(function(){try{var p=" + base.substring(0, base.length() - 1) + ";" +
                   "if(p&&p.setVolume)p.setVolume(" + vol + ");}catch(e){}})();" +
                   genericMediaVolumeJS(vol);
        } else if (this == GENERIC_HTML5) {
            return genericMediaVolumeJS(vol);
        }
        return "";
    }

    /**
     * Generic JS that sets {@code .volume} on every {@code <video>} and {@code <audio>}
     * element in the document, and recurses into same-origin iframes. Used as a fallback
     * for arbitrary URLs that aren't recognised as a specific {@link VideoType} — covers
     * the majority of HTML5 media on the modern web.
     *
     * <p>Limitations: cross-origin iframes can't be reached (browser security). Pages that
     * route audio through Web Audio API (gain nodes) won't honour the element-level volume.
     * Embedded Flash and custom player implementations also escape this. For those a proper
     * fix requires intercepting via {@code CefAudioHandler} at the engine level.
     */
    @Nonnull
    public static String genericMediaVolumeJS(String volStr) {
        return "(function(){try{" +
               "var els=document.querySelectorAll('video,audio');" +
               "for(var i=0;i<els.length;i++){try{els[i].volume=" + volStr + "/100;}catch(e){}}" +
               "var ifr=document.querySelectorAll('iframe');" +
               "for(var j=0;j<ifr.length;j++){try{" +
               "var es=ifr[j].contentDocument.querySelectorAll('video,audio');" +
               "for(var k=0;k<es.length;k++){try{es[k].volume=" + volStr + "/100;}catch(e){}}" +
               "}catch(e){}}" +
               "}catch(e){}})()";
    }

    /**
     * Resolve a {@link VideoType} for a URL, falling back to {@link #GENERIC_HTML5} when no
     * specific match exists. This is the entry point for volume control — using
     * {@link #getTypeFromURL} alone returns null for arbitrary URLs (no whitelist match),
     * which causes volume attenuation to be skipped. For attenuation purposes we always
     * have at least the GENERIC fallback available.
     */
    @Nonnull
    public static VideoType resolveForVolume(@Nonnull String url) {
        VideoType specific = getTypeFromURL(url);
        return specific != null ? specific : GENERIC_HTML5;
    }

    public String getTimeStampQuery() {
        return getTime.apply();
    }

    public String setTimeStampQuery(float ts) {
        return setTime.apply(String.valueOf(ts));
    }

    /**
     * Generate JavaScript to get video playback state for sync.
     * Returns JSON with currentTime and paused state.
     */
    @Nonnull
    public String getSyncStateJS() {
        if (this == YOUTUBE || this == YOUTUBE_EMBED) {
            return "(function(){try{var p=" + base.substring(0, base.length() - 1) + ";" +
                   "if(!p)return null;" +
                   "return JSON.stringify({time:p.getCurrentTime(),paused:p.getPlayerState()===2});" +
                   "}catch(e){return null;}})()";
        } else if (this == GENERIC_HTML5) {
            return "(function(){try{var v=document.querySelector('video');" +
                   "if(!v){var iframes=document.querySelectorAll('iframe');" +
                   "for(var i=0;i<iframes.length;i++){try{v=iframes[i].contentDocument.querySelector('video');if(v)break;}catch(e){}}}" +
                   "if(!v)return null;" +
                   "return JSON.stringify({time:v.currentTime,paused:v.paused});" +
                   "}catch(e){return null;}})()";
        }
        return "null";
    }

    /**
     * Generate JavaScript to seek video to a specific time.
     */
    @Nonnull
    public String getSeekJS(double timeSeconds) {
        if (this == YOUTUBE || this == YOUTUBE_EMBED) {
            return "(function(){try{var p=" + base.substring(0, base.length() - 1) + ";" +
                   "if(p)p.seekTo(" + timeSeconds + ",true);}catch(e){}})()";
        } else if (this == GENERIC_HTML5) {
            return "(function(){try{var v=document.querySelector('video');" +
                   "if(!v){var iframes=document.querySelectorAll('iframe');" +
                   "for(var i=0;i<iframes.length;i++){try{v=iframes[i].contentDocument.querySelector('video');if(v)break;}catch(e){}}}" +
                   "if(v)v.currentTime=" + timeSeconds + ";}catch(e){}})()";
        }
        return "";
    }

    /**
     * Generate JavaScript to set play/pause state.
     */
    @Nonnull
    public String getPlayPauseJS(boolean play) {
        if (this == YOUTUBE || this == YOUTUBE_EMBED) {
            String action = play ? "playVideo()" : "pauseVideo()";
            return "(function(){try{var p=" + base.substring(0, base.length() - 1) + ";" +
                   "if(p)p." + action + ";}catch(e){}})()";
        } else if (this == GENERIC_HTML5) {
            String action = play ? "play()" : "pause()";
            return "(function(){try{var v=document.querySelector('video');" +
                   "if(!v){var iframes=document.querySelectorAll('iframe');" +
                   "for(var i=0;i<iframes.length;i++){try{v=iframes[i].contentDocument.querySelector('video');if(v)break;}catch(e){}}}" +
                   "if(v)v." + action + ";}catch(e){}})()";
        }
        return "";
    }
}
