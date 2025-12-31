/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * WebDisplays player data using NeoForge Data Attachments system.
 */
public class WDDCapability implements IWDDCapability {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "webdisplays");

    public static final Codec<WDDCapability> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("firstRun").forGetter(cap -> cap.firstRun)
            ).apply(instance, WDDCapability::new)
    );

    public static final Supplier<AttachmentType<WDDCapability>> WDD_ATTACHMENT = ATTACHMENT_TYPES.register(
            "player_data",
            () -> AttachmentType.builder((java.util.function.Supplier<WDDCapability>) WDDCapability::new)
                    .serialize(CODEC)
                    .copyOnDeath()
                    .build()
    );

    private boolean firstRun;

    public WDDCapability() {
        this.firstRun = true;
    }

    public WDDCapability(boolean firstRun) {
        this.firstRun = firstRun;
    }

    @Override
    public boolean isFirstRun() {
        return firstRun;
    }

    @Override
    public void clearFirstRun() {
        firstRun = false;
    }

    @Override
    public void cloneTo(IWDDCapability dst) {
        if (!isFirstRun())
            dst.clearFirstRun();
    }

    /**
     * Get the WDD capability data from a player.
     */
    public static WDDCapability get(Player player) {
        return player.getData(WDD_ATTACHMENT);
    }

    /**
     * Check if a player has the WDD attachment.
     */
    public static boolean has(Player player) {
        return player.hasData(WDD_ATTACHMENT);
    }
}
