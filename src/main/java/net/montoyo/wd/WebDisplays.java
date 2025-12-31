/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd;

import com.google.gson.Gson;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.client.gui.camera.KeyboardCamera;
import net.montoyo.wd.config.ClientConfig;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.controls.ScreenControlRegistry;
import net.montoyo.wd.core.*;
import net.montoyo.wd.miniserv.server.Server;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageServerInfo;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.registry.ItemRegistry;
import net.montoyo.wd.registry.TileRegistry;
import net.montoyo.wd.registry.WDTabs;
import net.montoyo.wd.utilities.DistSafety;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.serialization.Util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@Mod("webdisplays")
public class WebDisplays {
    public static WebDisplays INSTANCE;

    public static SharedProxy PROXY = null;

    public static final ResourceLocation ADV_PAD_BREAK = ResourceLocation.fromNamespaceAndPath("webdisplays", "webdisplays/pad_break");
    public static final String BLACKLIST_URL = "mod://webdisplays/blacklisted.html";
    public static final Gson GSON = new Gson();
    public static final ResourceLocation CAPABILITY = ResourceLocation.fromNamespaceAndPath("webdisplays", "customdatacap");

    // Event bus stored for config initialization
    private static IEventBus modEventBus;

    //Sounds
    public SoundEvent soundTyping;
    public SoundEvent soundUpgradeAdd;
    public SoundEvent soundUpgradeDel;
    public SoundEvent soundScreenCfg;
    public SoundEvent soundServer;
    public SoundEvent soundIronic;

    //Criterions - using DeferredRegister for NeoForge 1.21.1
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, "webdisplays");

    public static final DeferredHolder<CriterionTrigger<?>, WDCriterion> CRITERION_PAD_BREAK =
            TRIGGERS.register("pad_break", () -> new WDCriterion("pad_break"));
    public static final DeferredHolder<CriterionTrigger<?>, WDCriterion> CRITERION_UPGRADE_SCREEN =
            TRIGGERS.register("upgrade_screen", () -> new WDCriterion("upgrade_screen"));
    public static final DeferredHolder<CriterionTrigger<?>, WDCriterion> CRITERION_LINK_PERIPHERAL =
            TRIGGERS.register("link_peripheral", () -> new WDCriterion("link_peripheral"));
    public static final DeferredHolder<CriterionTrigger<?>, WDCriterion> CRITERION_KEYBOARD_CAT =
            TRIGGERS.register("keyboard_cat", () -> new WDCriterion("keyboard_cat"));


    //Config
    public static final double PAD_RATIO = 59.0 / 30.0;
    public double padResX;
    public double padResY;
    private int lastPadId = 0;
    public double unloadDistance2;
    public double loadDistance2;
    public int miniservPort;
    public long miniservQuota;
    public float ytVolume;
    public float avDist100;
    public float avDist0;

    // mod detection
    private boolean hasOC;
    private boolean hasCC;

    public WebDisplays(IEventBus bus) {
        INSTANCE = this;
        modEventBus = bus;

        if(FMLEnvironment.dist.isClient()) {
            PROXY = DistSafety.createProxy();
        } else {
            PROXY = new SharedProxy();
        }

        if (FMLEnvironment.dist.isClient()) {
            // proxies are annoying, so from now on, I'mma be just registering stuff in here
            bus.addListener(ClientProxy::onKeybindRegistry);
            bus.addListener(ClientProxy::onClientSetup);
            bus.addListener(ClientProxy::onModelRegistryEvent);
            NeoForge.EVENT_BUS.addListener(ClientProxy::onDrawSelection);
            NeoForge.EVENT_BUS.addListener(KeyboardCamera::updateCamera);
            NeoForge.EVENT_BUS.addListener(KeyboardCamera::gameTick);
            ClientConfig.init(bus);
        }

        CommonConfig.init(bus);

        //Criterions - register with the mod event bus
        TRIGGERS.register(bus);

        WDNetworkRegistry.init();
        SOUNDS.register(bus);
        onRegisterSounds();
        WDTabs.init(bus);
        BlockRegistry.init(bus);
        ItemRegistry.init(bus);
        TileRegistry.init(bus);
        WDDCapability.ATTACHMENT_TYPES.register(bus);

        PROXY.preInit();

        NeoForge.EVENT_BUS.register(this);

        //Other things
        PROXY.init();

        PROXY.postInit();
        hasOC = ModList.get().isLoaded("opencomputers");
        hasCC = ModList.get().isLoaded("computercraft");

        if (!FMLEnvironment.production) {
            ScreenControlRegistry.init();
        }
    }

    public static IEventBus getModEventBus() {
        return modEventBus;
    }

    public void onRegisterSounds() {
        soundTyping = registerSound("keyboard_type");
        soundUpgradeAdd = registerSound("upgrade_add");
        soundUpgradeDel = registerSound("upgrade_del");
        soundScreenCfg = registerSound("screencfg_open");
        soundServer = registerSound("server");
        soundIronic = registerSound("ironic");
    }

    ArrayList<ResourceKey<Level>> serverStartedDimensions = new ArrayList<>();

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load ev) {
        if (ev.getLevel() instanceof Level level) {
            if (ev.getLevel().isClientSide() || level.dimension() != Level.OVERWORLD)
                return;

            File worldDir = Objects.requireNonNull(ev.getLevel().getServer()).getServerDirectory().toFile();
            File f = new File(worldDir, "wd_next.txt");

            if (f.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String idx = br.readLine();
                    Util.silentClose(br);

                    if (idx == null)
                        throw new RuntimeException("Seems like the file is empty (1)");

                    idx = idx.trim();
                    if (idx.isEmpty())
                        throw new RuntimeException("Seems like the file is empty (2)");

                    lastPadId = Integer.parseInt(idx); //This will throw NumberFormatException if it goes wrong
                } catch (Throwable t) {
                    Log.warningEx("Could not read last minePad ID from %s. I'm afraid this might break all minePads.", t, f.getAbsolutePath());
                }
            }

            if (miniservPort != 0) {
                Server sv = Server.getInstance();

                if(!serverStartedDimensions.contains(level.dimension())) {
                    sv.setPort(miniservPort);
                    sv.setDirectory(new File(worldDir, "wd_filehost"));
                    sv.start();
                    serverStartedDimensions.add(level.dimension());
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldLeave(LevelEvent.Unload ev) throws IOException {
        if(ev.getLevel() instanceof Level level) {
            if (ev.getLevel().isClientSide() || level.dimension() != Level.OVERWORLD)
                return;
            Server sw = Server.getInstance();
            sw.stopServer();
            serverStartedDimensions.remove(level.dimension());
        }
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save ev) {
        if(ev.getLevel() instanceof Level level) {
            if (ev.getLevel().isClientSide() || level.dimension() != Level.OVERWORLD)
                return;
            File f = new File(Objects.requireNonNull(ev.getLevel().getServer()).getServerDirectory().toFile(), "wd_next.txt");

            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write("" + lastPadId + "\n");
                Util.silentClose(bw);
            } catch (Throwable t) {
                Log.warningEx("Could not save last minePad ID (%d) to %s. I'm afraid this might break all minePads.", t, lastPadId, f.getAbsolutePath());
            }
        }
    }

    @SubscribeEvent
    public void onToss(ItemTossEvent ev) {
        if(!ev.getEntity().level().isClientSide) {
            ItemStack is = ev.getEntity().getItem();

            if(is.getItem() == ItemRegistry.MINEPAD.get()) {
                CompoundTag tag = is.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();

                UUID thrower = ev.getPlayer().getGameProfile().getId();
                tag.putLong("ThrowerMSB", thrower.getMostSignificantBits());
                tag.putLong("ThrowerLSB", thrower.getLeastSignificantBits());
                tag.putDouble("ThrowHeight", ev.getPlayer().getY() + ev.getPlayer().getEyeHeight());
                is.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerCraft(PlayerEvent.ItemCraftedEvent ev) {
        if(CommonConfig.hardRecipes && ItemRegistry.isCompCraftItem(ev.getCrafting().getItem()) && (CraftComponent.EXTCARD.makeItemStack().is(ev.getCrafting().getItem()))) {
            if((ev.getEntity() instanceof ServerPlayer && !hasPlayerAdvancement((ServerPlayer) ev.getEntity(), ADV_PAD_BREAK)) || PROXY.hasClientPlayerAdvancement(ADV_PAD_BREAK) != HasAdvancement.YES) {
                ev.getCrafting().setDamageValue(CraftComponent.BADEXTCARD.ordinal());

                if(!ev.getEntity().level().isClientSide)
                    ev.getEntity().level().playSound(null, ev.getEntity().getX(), ev.getEntity().getY(), ev.getEntity().getZ(), SoundEvents.ITEM_BREAK, SoundSource.MASTER, 1.0f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent ev) throws IOException {
        Server.getInstance().stopServer();
    }

    @SubscribeEvent
    public void onLogIn(PlayerEvent.PlayerLoggedInEvent ev) {
        if (!CommonConfig.joinMessage) {
            return;
        }

        if(!ev.getEntity().level().isClientSide && ev.getEntity() instanceof ServerPlayer serverPlayer) {
            WDDCapability cap = WDDCapability.get(serverPlayer);

            if(cap.isFirstRun()) {
                Util.toast(ev.getEntity(), ChatFormatting.LIGHT_PURPLE, "welcome1");
                Util.toast(ev.getEntity(), ChatFormatting.LIGHT_PURPLE, "welcome2");
                Util.toast(ev.getEntity(), ChatFormatting.LIGHT_PURPLE, "welcome3");

                cap.clearFirstRun();
            }

            S2CMessageServerInfo message = new S2CMessageServerInfo(miniservPort);
            WDNetworkRegistry.sendToPlayer(serverPlayer, message);
        }
    }

    @SubscribeEvent
    public void onLogOut(PlayerEvent.PlayerLoggedOutEvent ev) {
        if(!ev.getEntity().level().isClientSide)
            Server.getInstance().getClientManager().revokeClientKey(ev.getEntity().getGameProfile().getId());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone ev) {
        // TODO: Implement with new data attachments system
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent ev) {
        String msg = ev.getMessage().getString().replaceAll("\\s+", " ").toLowerCase();
        StringBuilder sb = new StringBuilder(msg.length());
        for(int i = 0; i < msg.length(); i++) {
            char chr = msg.charAt(i);

            if(chr != '.' && chr != ',' && chr != ';' && chr != '!' && chr != '?' && chr != ':' && chr != '\'' && chr != '\"' && chr != '`')
                sb.append(chr);
        }

        if(sb.toString().equals("ironic he could save others from death but not himself")) {
            Player ply = ev.getPlayer();
            ply.level().playSound(null, ply.getX(), ply.getY(), ply.getZ(), soundIronic, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @SubscribeEvent
    public void onClientChat(ClientChatEvent ev) {
        if(ev.getMessage().equals("!WD render recipes"))
            PROXY.renderRecipes();
    }

    private boolean hasPlayerAdvancement(ServerPlayer ply, ResourceLocation rl) {
        MinecraftServer server = PROXY.getServer();
        if(server == null)
            return false;

        AdvancementHolder advHolder = server.getAdvancements().get(rl);
        return advHolder != null && ply.getAdvancements().getOrStartProgress(advHolder).isDone();
    }

    public static int getNextAvailablePadID() {
        return INSTANCE.lastPadId++;
    }

    public static DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "webdisplays");

    private static SoundEvent registerSound(String resName) {
        ResourceLocation resLoc = ResourceLocation.fromNamespaceAndPath("webdisplays", resName);
        SoundEvent ret = SoundEvent.createVariableRangeEvent(resLoc);

        SOUNDS.register(resName, () -> ret);
        return ret;
    }


    public static boolean isSiteBlacklisted(String url) {
        try {
            URL url2 = new URL(Util.addProtocol(url));
            for (String str : CommonConfig.Browser.blacklist)
                if (str.equalsIgnoreCase(url2.getHost())) return true;
            return false;
        } catch(MalformedURLException ex) {
            return false;
        }
    }

    public static String applyBlacklist(String url) {
        return isSiteBlacklisted(url) ? BLACKLIST_URL : url;
    }
}
