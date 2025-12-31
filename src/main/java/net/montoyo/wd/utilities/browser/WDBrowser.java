package net.montoyo.wd.utilities.browser;

import com.cinemamod.mcef.MCEF;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.browser.handlers.js.queries.ElementCenterQuery;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import net.montoyo.wd.utilities.data.BlockSide;
import org.cef.browser.CefBrowser;

import java.util.HashMap;
import java.util.Map;

public interface WDBrowser {
    static CefBrowser createBrowser(String url, boolean transparent) {
        WDClientBrowser browser = new WDClientBrowser(MCEF.getClient(), url, transparent);
        browser.setCloseAllowed();
        browser.createImmediately();
        registerQueries(browser);
        return browser;
    }

    static void registerQueries(WDBrowser browser) {
        Map<String, JSQueryHandler> handlerMap = browser.queryHandlers();

        JSQueryHandler handler;

        handler = browser.focusedElement();
        handlerMap.put(handler.getName(), handler);
        handler = browser.pointerLockElement();
        handlerMap.put(handler.getName(), handler);
    }

    HashMap<String, JSQueryHandler> queryHandlers();
    ElementCenterQuery focusedElement();
    ElementCenterQuery pointerLockElement();

    void setBe(ScreenBlockEntity blockEntity, BlockSide side);
    ScreenBlockEntity getBe();
    BlockSide getSide();
}
