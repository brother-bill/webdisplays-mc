package net.montoyo.wd.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.montoyo.wd.registry.ItemRegistry;
import net.montoyo.wd.item.ItemLaserPointer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(at = @At("HEAD"), method = "onPress", cancellable = true)
    public void prePress(long p_91531_, int p_91532_, int p_91533_, int p_91534_, CallbackInfo ci) {
        boolean flag = p_91533_ == 1;

        if (Minecraft.getInstance().screen == null) {
            if (
                    minecraft.player != null && minecraft.level != null &&
                            minecraft.player.getItemInHand(InteractionHand.MAIN_HAND).getItem().equals(ItemRegistry.LASER_POINTER.get()) &&
                            (minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.BLOCK || minecraft.hitResult.getType() == HitResult.Type.MISS)
            ) {
                ItemLaserPointer.press(flag, p_91532_);
                ci.cancel();
            }
        }
    }
}
