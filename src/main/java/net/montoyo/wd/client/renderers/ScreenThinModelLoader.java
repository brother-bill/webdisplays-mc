package net.montoyo.wd.client.renderers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

/** Counterpart of {@link ScreenModelLoader} for thin screen blocks — produces {@link ScreenThinBaker}. */
public class ScreenThinModelLoader implements IGeometryLoader<ScreenThinModelLoader.ThinGeometry> {
    public static final ResourceLocation LOADER_ID =
            ResourceLocation.fromNamespaceAndPath("webdisplays", "screen_thin_loader");

    @Override
    public ThinGeometry read(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new ThinGeometry();
    }

    public static class ThinGeometry implements IUnbakedGeometry<ThinGeometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides) {
            return new ScreenThinBaker(modelState, spriteGetter, overrides, context.getTransforms());
        }
    }
}
