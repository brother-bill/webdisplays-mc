package net.montoyo.wd.client.renderers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

public class ScreenModelLoader implements IGeometryLoader<ScreenModelLoader.ScreenModelGeometry> {
    public static final ResourceLocation SCREEN_LOADER = ResourceLocation.fromNamespaceAndPath("webdisplays", "screen_loader");

    public static final ResourceLocation SCREEN_SIDE = ResourceLocation.fromNamespaceAndPath("webdisplays", "block/screen");

    private static final ResourceLocation[] SIDES = new ResourceLocation[16];
    public static final Material[] MATERIALS_SIDES = new Material[16];

    static {
        for (int i = 0; i < SIDES.length; i++) {
            SIDES[i] = ResourceLocation.fromNamespaceAndPath(SCREEN_SIDE.getNamespace(), SCREEN_SIDE.getPath() + i);
            MATERIALS_SIDES[i] = new Material(InventoryMenu.BLOCK_ATLAS, SIDES[i]);
        }
    }

    @Override
    public ScreenModelGeometry read(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new ScreenModelGeometry();
    }

    public static class ScreenModelGeometry implements IUnbakedGeometry<ScreenModelGeometry> {

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            return new ScreenBaker(modelState, spriteGetter, overrides, context.getTransforms());
        }
    }
}
