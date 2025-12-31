package net.montoyo.wd.utilities.browser.handlers.js.queries;

import com.google.gson.JsonObject;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import net.montoyo.wd.utilities.browser.handlers.js.Scripts;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

public class ElementCenterQuery extends JSQueryHandler {
    boolean exists = false;
    double x, y;
    JsonObject obj;
    long start = -1;
    String extra = "";

    String elementName;
    String script = null;

    public ElementCenterQuery(String queryName, String name) {
        super(queryName);
        elementName = name;
    }

    public ElementCenterQuery addAdditional(String key, String value) {
        extra += "'," + key + ":' + " + value + " +";
        script = null;
        return this;
    }

    @Override
    public boolean handle(CefBrowser browser, CefFrame frame, JsonObject data, boolean persistent, CefQueryCallback callback) {
        exists = data.getAsJsonPrimitive("exists").getAsBoolean();
        if (exists) {
            x = data.getAsJsonPrimitive("x").getAsDouble() + data.getAsJsonPrimitive("w").getAsDouble() / 2;
            y = data.getAsJsonPrimitive("y").getAsDouble() + data.getAsJsonPrimitive("h").getAsDouble() / 2;
        }
        obj = data;

        start = -1;
        callback.success("Success");
        return true;
    }

    public void dispatch(CefBrowser browser) {
        if (script == null) {
            script = Scripts.QUERY_ELEMENT
                    .replace("%type%", elementName)
                    .replace("%Type%", name)
                    .replace("%extra%", extra)
            ;
        }

        if (start == -1) {
            browser.executeJavaScript(
                    script,
                    "CenterQuery",
                    0
            );
            start = System.currentTimeMillis();
        } else {
            long ms = System.currentTimeMillis();
            if (start + 1000 < ms) {
                browser.executeJavaScript(
                        script,
                        "CenterQuery",
                        0
                );
                start = System.currentTimeMillis();
            }
        }
    }

    public boolean hasFocused() {
        return exists;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public JsonObject getObj() {
        return obj;
    }
}
