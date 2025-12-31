package net.montoyo.wd.utilities.browser;

import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.browser.handlers.js.queries.GetSizeQuery;
import net.montoyo.wd.utilities.data.BlockSide;

public class InWorldQueries {
    private static final GetSizeQuery getSize = new GetSizeQuery();

    public static void attach(ScreenBlockEntity blockEntity, BlockSide side, WDBrowser browser) {
        browser.setBe(blockEntity, side);
        browser.queryHandlers().put(getSize.getName(), getSize);
    }
}
