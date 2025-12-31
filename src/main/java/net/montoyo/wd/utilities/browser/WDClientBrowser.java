package net.montoyo.wd.utilities.browser;

import com.cinemamod.mcef.MCEFBrowser;
import com.cinemamod.mcef.MCEFClient;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.browser.handlers.js.queries.ElementCenterQuery;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.HashMap;

public class WDClientBrowser extends MCEFBrowser implements WDBrowser {
    ElementCenterQuery focusedEl = new ElementCenterQuery("ActiveElement", "document.activeElement");
    ElementCenterQuery pointerLockEl =
            new ElementCenterQuery("PointerElement", "document.pointerLockElement")
                    .addAdditional("unadjust", "document.webdisplays__unadjustPointerMotion")
            ;
    HashMap<String, JSQueryHandler> handlerHashMap = new HashMap<>();

    ScreenBlockEntity be;
    BlockSide side;

    public WDClientBrowser(MCEFClient client, String url, boolean transparent) {
        super(client, url, transparent);
    }

    @Override
    public HashMap<String, JSQueryHandler> queryHandlers() {
        return handlerHashMap;
    }

    @Override
    public ElementCenterQuery focusedElement() {
        return focusedEl;
    }

    @Override
    public ElementCenterQuery pointerLockElement() {
        return pointerLockEl;
    }

    @Override
    public void setBe(ScreenBlockEntity blockEntity, BlockSide side) {
        this.be = blockEntity;
        this.side = side;
    }

    @Override
    public ScreenBlockEntity getBe() {
        return be;
    }

    @Override
    public BlockSide getSide() {
        return side;
    }
}
