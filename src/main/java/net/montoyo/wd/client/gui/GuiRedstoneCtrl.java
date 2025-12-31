/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.client.gui.controls.Button;
import net.montoyo.wd.client.gui.controls.TextField;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

import javax.annotation.Nullable;

public class GuiRedstoneCtrl extends WDScreen {

    private ResourceLocation dimension;
    private Vector3i pos;
    private String risingEdgeURL;
    private String fallingEdgeURL;

    @FillControl
    private TextField tfRisingEdge;

    @FillControl
    private TextField tfFallingEdge;

    @FillControl
    private Button btnOk;

    public GuiRedstoneCtrl(Component component, ResourceLocation d, Vector3i p, String r, String f) {
        super(component);
        dimension = d;
        pos = p;
        risingEdgeURL = r;
        fallingEdgeURL = f;
    }

    @Override
    public void init() {
        super.init();
        loadFrom(ResourceLocation.fromNamespaceAndPath("webdisplays", "gui/redstonectrl.json"));
        tfRisingEdge.setText(risingEdgeURL);
        tfFallingEdge.setText(fallingEdgeURL);
    }

//    @GuiSubscribe
//    public void onClick(Button.ClickEvent ev) {
//        if(ev.getSource() == btnOk) {
//            API mcef = ((ClientProxy) WebDisplays.PROXY).getMCEF();
//
//            String rising = mcef.punycode(Util.addProtocol(tfRisingEdge.getText()));
//            String falling = mcef.punycode(Util.addProtocol(tfFallingEdge.getText()));
//            WDNetworkRegistry.sendToServer(new C2SMessageRedstoneCtrl(pos, rising, falling));
//        }
//
//        minecraft.setScreen(null);
//    }

    @Override
    public boolean isForBlock(BlockPos bp, BlockSide side) {
        return pos.equalsBlockPos(bp);
    }

    @Nullable
    @Override
    public String getWikiPageName() {
        return "Redstone_Controller";
    }

}
