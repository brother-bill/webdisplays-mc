package net.montoyo.wd.utilities.browser.handlers.js;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FileName {
    String value();
}
