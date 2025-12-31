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
            new Function("seekTo(", ")")
    ),
    YOUTUBE_EMBED(
            "document.getElementsByClassName(\"html5-video-player\")[0].",
            new Function("setVolume(", ")"),
            new Function("getCurrentTime(", ")"),
            new Function("seekTo(", ")")
    );

    private final String base;
    private final Function volume;
    private final Function getTime;
    private final Function setTime;
    private final int volumeCap;

    VideoType(
            String base,
            Function volume,
            Function getTime,
            Function setTime
    ) {
        this.base = base;
        this.volume = volume;
        this.getTime = getTime;
        this.setTime = setTime;
        // lol, what?
        volumeCap = volume.prefix.length() + 5 + volume.suffix.length();
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
        if (loHost.equals("youtu.be"))
            return url.getPath().length() > 1 ? YOUTUBE : null;
        else if (!loHost.equals("www.youtube.com") && !loHost.equals("youtube.com"))
            return null;

        String loPath = url.getPath().toLowerCase();
        if (loPath.equals("/watch")) {
            if (url.getQuery() != null && (url.getQuery().startsWith("v=") || url.getQuery().contains("&v=")))
                return YOUTUBE;
        } else if (loPath.startsWith("/embed/"))
            return loPath.length() > 7 ? YOUTUBE_EMBED : null;

        return null;
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

    // TODO: timestamp stuff
    @Nonnull
    public String getVolumeJSQuery(int volInt, int volFrac) {
        return volume.apply(volInt + "." + volFrac);
    }

    public String getTimeStampQuery() {
        return getTime.apply();
    }

    public String setTimeStampQuery(float ts) {
        return setTime.apply(String.valueOf(ts));
    }
}
