package net.montoyo.wd.client.gui.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.montoyo.wd.client.gui.GuiKeyboard;
import net.montoyo.wd.config.ClientConfig;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.queries.ElementCenterQuery;
import net.montoyo.wd.utilities.data.BlockSide;

public class KeyboardCamera {
    private static ScreenBlockEntity tes;
    private static BlockSide side;

    private static double oxCrd = -1;
    private static double xCrd = -1;
    private static double nxCrd = -1;
    private static double oyCrd = -1;
    private static double yCrd = -1;
    private static double nyCrd = -1;

    private static double nextX = -1;
    private static double nextY = -1;
    private static double focalX = -1;
    private static double focalY = -1;

    private static final boolean[] mouseStatus = new boolean[2];

    protected static Vec2 pxToHit(ScreenData scr, Vec2 dst) {
        float cx, cy;
        if (scr.rotation.isVertical) {
            cy = dst.x;
            cx = dst.y;
        } else {
            cx = dst.x;
            cy = dst.y;
        }

        cx /= (float) scr.resolution.x;
        cy /= (float) scr.resolution.y;

        switch (scr.rotation) {
            case ROT_270:
                cx = 1.0f - cx;
                break;

            case ROT_180:
                cx = 1.0f - cx;
                cy = 1.0f - cy;
                break;

            case ROT_90:
                cy = 1.0f - cy;
                break;
        }

        if (side != BlockSide.BOTTOM)
            cy = 1.0f - cy;

        float swInverse = (((float) scr.size.x) - 4.f / 16.f);
        float shInverse = (((float) scr.size.y) - 4.f / 16.f);

        cx *= swInverse;
        cy *= shInverse;

        if (side.right.x > 0 || side.right.z > 0)
            cx += 1.f;

        if (side == BlockSide.TOP || side == BlockSide.BOTTOM)
            cy -= 1.f;

        return new Vec2(cx + (2 / 16f), cy + (2 / 16f));
    }

    protected static void updateCrd(ElementCenterQuery lock) {
        ScreenData scr = tes.getScreen(side);
        if (scr != null) {
            Vec2 c;

            if (!mouseStatus[0] && !mouseStatus[1]) {
                if (lock.hasFocused()) {
                    if (ClientConfig.Input.keyboardCamera) {
                        nextX = lock.getX();
                        nextY = lock.getY();

                        c = pxToHit(scr, new Vec2((float) nextX, (float) nextY));
                    } else c = new Vec2(scr.size.x / 2f, scr.size.y / 2f);
                } else c = new Vec2(scr.size.x / 2f, scr.size.y / 2f);
//            } else c = new Vec2((float) focalX, (float) focalY);
            } else return;

            focalX = c.x;
            focalY = c.y;

            nextX = c.x;
            nextY = c.y;

            if (nextX < 0) nextX = 0;
            else if (nextX > scr.size.x) nextX = scr.size.x;
            if (nextY < 0) nextY = 0;
            else if (nextY > scr.size.y) nextY = scr.size.y;

            float scl = Math.max(scr.size.x, scr.size.y);

            double mx = Minecraft.getInstance().mouseHandler.xpos();
            mx /= Minecraft.getInstance().getWindow().getWidth();

            double my = Minecraft.getInstance().mouseHandler.ypos();
            my /= Minecraft.getInstance().getWindow().getHeight();

            Vec2 v2 = new Vec2((float) mx, (float) my).add(-0.5f);

            nextX += v2.x * scl;
            nextY -= v2.y * scl;
        }
    }

    protected static void pollElement() {
        ScreenBlockEntity teTmp = tes;
        BlockSide sdTmp = side;

        // async nonsense can occur here
        if (teTmp == null || sdTmp == null) return;

        ScreenData scr = teTmp.getScreen(sdTmp);
        if (scr != null) {
            if (scr.browser instanceof WDBrowser wdBrowser) {
                wdBrowser.focusedElement().dispatch(scr.browser);
                updateCrd(((WDBrowser) scr.browser).focusedElement());
            }
        }
    }

    public static float[] getAngle(Entity e, double pct) {
        BlockEntity tes = KeyboardCamera.tes;
        BlockSide side = KeyboardCamera.side;
        if (tes == null) return new float[]{Float.NaN, 0};
        if (side == null) return new float[]{Float.NaN, 0};

        double coxCrd = Mth.lerp(0.5 * pct, oxCrd, xCrd);
        double coyCrd = Mth.lerp(0.5 * pct, oyCrd, yCrd);

        double focalX = tes.getBlockPos().getX() +
                side.right.x * (coxCrd - 1) + side.up.x * coyCrd + Math.abs(side.forward.x) * 0.5;
        double focalY = tes.getBlockPos().getY() +
                side.right.y * (coxCrd - 1) + side.up.y * coyCrd + Math.abs(side.forward.y) * 0.5;
        double focalZ = tes.getBlockPos().getZ() +
                side.right.z * (coxCrd - 1) + side.up.z * coyCrd + Math.abs(side.forward.z) * 0.5;

        focalX += side.forward.x * 0.5f;
        focalY += side.forward.y * 0.5f;
        focalZ += side.forward.z * 0.5f;

        float[] angle = lookAt(
                e, EntityAnchorArgument.Anchor.EYES,
                new Vec3(focalX, focalY, focalZ)
        );

        return angle;
    }

    public static void setMouse(int side, boolean pressed) {
        mouseStatus[side] = pressed;
    }

    public static void updateCamera(ViewportEvent.ComputeCameraAngles event) {
        if (tes == null) {
            xCrd = -1;
            yCrd = -1;
            return; // nothing to do
        }

        if (xCrd == -1) return;
        if (yCrd == -1) return;

        float[] angle = getAngle(event.getCamera().getEntity(), event.getPartialTick());

        if (Float.isNaN(angle[0])) return;

//        float xRot = event.getYaw(); // left right
//        float yRot = event.getPitch(); // up down

        // TODO: smooth in/out
        event.setYaw(angle[1]);
        event.setPitch(angle[0]);
    }

    public static void focus(ScreenBlockEntity screen, BlockSide side) {
        KeyboardCamera.tes = screen;
        KeyboardCamera.side = side;
    }

    public static float[] lookAt(Entity entity, EntityAnchorArgument.Anchor pAnchor, Vec3 pTarget) {
        Vec3 vec3 = pAnchor.apply(entity);
        double d0 = pTarget.x - vec3.x;
        double d1 = pTarget.y - vec3.y;
        double d2 = pTarget.z - vec3.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float xr = (Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * (double) (180F / (float) Math.PI)))));
        float yr = (Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F));
        return new float[]{xr, yr};
    }

    protected static int delay = 8;

    public static void gameTick(ClientTickEvent.Post event) {
        if (mouseStatus[0] || mouseStatus[1]) {
            oxCrd = Mth.lerp(0.5, oxCrd, xCrd);
            oyCrd = Mth.lerp(0.5, oyCrd, yCrd);
            return;
        }
        // Post event means we're at the end of the tick
        if (side == null) {
                delay = 1;
                oxCrd = -1;
                oyCrd = -1;
                xCrd = -1;
                yCrd = -1;
                nxCrd = -1;
                nyCrd = -1;
                return;
            }

            if (!(Minecraft.getInstance().screen instanceof GuiKeyboard)) {
                tes = null;
                side = null;
                return;
            }

            pollElement();

            double anxx = nextX;
            double anxy = nextY;

            if (
                    anxx == -1 || anxy == -1 ||
                            nxCrd == -1 || nyCrd == -1 ||
                            oxCrd == -1 || oyCrd == -1 ||
                            xCrd == -1 || yCrd == -1
            ) {
                ScreenData data = tes.getScreen(side);
                if (data == null)
                    return;

                anxx = data.size.x / 2.0;
                anxy = data.size.y / 2.0;

                if (nxCrd == -1) {
                    oxCrd = xCrd = anxx;
                    oyCrd = yCrd = anxy;
                }
            }

            nxCrd = anxx;
            nyCrd = anxy;

            oxCrd = Mth.lerp(0.5, oxCrd, xCrd);
            xCrd = Mth.lerp(0.15, xCrd, nxCrd);

            oyCrd = Mth.lerp(0.5, oyCrd, yCrd);
            yCrd = Mth.lerp(0.15, yCrd, nyCrd);
    }
}
