package de.waterdu.atlantis.self;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.ConfigEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.PageEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.network.PacketInterceptEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.util.java.ClassNameCache;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.text.MessageUtils;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class AtlantisListeners {

    private static final Pair<Integer, PageOptions.FormFactor> DEFAULT_FORM_FACTOR = Pair.of(6, PageOptions.FormFactor.CHEST);
    private static final Text COPY = Text.of(ChatFormatting.BLACK + "[" + ChatFormatting.YELLOW + "Copy" + ChatFormatting.BLACK + "]");
    private static final Text COPY_LORE = Text.of(ChatFormatting.YELLOW + "Click to copy to clipboard");
    private static final Text DECORATE = Text.of(ChatFormatting.BLACK + "[" + ChatFormatting.GOLD + "Decorate" + ChatFormatting.BLACK + "]");
    private static final Text DECORATE_LORE = Text.of(ChatFormatting.GOLD + "Click to open decoration editor");

    private static final String PACKET_LOG = "{}, {}, {}: {}";
    private static final String SOURCELESS = "sourceless";
    private static final String CLIENTBOUND = "clientbound";
    private static final String SERVERBOUND = "serverbound";
    private static final String VANILLA = "vanilla";
    private static final String CUSTOM = "custom";

    private final Set<UUID> pageClasspathTracking = ConcurrentHashMap.newKeySet();
    private final BiMap<String, Class<? extends Page>> pages = Maps.synchronizedBiMap(HashBiMap.create());
    private final Map<Class<? extends Page>, Pair<Integer, PageOptions.FormFactor>> formFactors = new ConcurrentHashMap<>();
    private String[] pageNames = new String[0];

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private PacketLogger packetLogger;

    public AtlantisListeners() {
        Mods.modLoader().event(UEvent.CONSTRUCT_PAGE).listen(this::onPageOpen);
    }

    public void startLoggingPackets() {
        this.packetLogger = new PacketLogger();
    }

    public boolean toggleClasspathTracking(Entity player) {
        if (this.pageClasspathTracking.contains(player.getUUID())) {
            this.pageClasspathTracking.remove(player.getUUID());
            return false;
        } else {
            this.pageClasspathTracking.add(player.getUUID());
            return true;
        }
    }

    public Pair<Integer, PageOptions.FormFactor> formFactor(Class<? extends Page> clazz) {
        return this.formFactors.getOrDefault(clazz, AtlantisListeners.DEFAULT_FORM_FACTOR);
    }

    public String[] pageNames() {
        if (this.pageNames.length != this.pages.size()) {
            this.pageNames = this.pages.keySet().toArray(new String[0]);
        }
        return this.pageNames;
    }

    public Class<? extends Page> getPage(String name) {
        return this.pages.get(name);
    }

    public InteractionResult onPageOpen(PageEventPayload.Construct event) {
        Atlantis.THREAD_POOL.submit(() -> {
            var options = event.page().options(event.player());
            this.formFactors.put(event.page().getClass(), Pair.of(options.rows(), options.formFactor()));
            this.pages.put(event.page().getClass().getName(), event.page().getClass());
        });
        if (this.pageClasspathTracking.contains(event.player().uuid())) {
            var name = event.page().getClass().getName();
            MessageUtils.send(
                    AtlantisCommand.PREFIX.compile()
                            .append(AtlantisListeners.COPY.compile().withStyle(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, AtlantisListeners.COPY_LORE.compile()))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name))))
                            .append(" ")
                            .append(AtlantisListeners.DECORATE.compile().withStyle(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, AtlantisListeners.DECORATE_LORE.compile()))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/atlantis decorate " + name))))
                            .append(" " + ChatFormatting.GRAY + "Just opened: " + ChatFormatting.WHITE + name),
                    event.player()
            );
            AtlantisLogger.info("Just opened: " + name);
        }
        return InteractionResult.PASS;
    }

    private static final class PacketLogger {

        private final AtomicReference<AtlantisLogger.Severity> logLevel = new AtomicReference<>(AtlantisLogger.Severity.NONE);

        private PacketLogger() {
            Mods.modLoader().event(UEvent.READ_CONFIG).listen(this::onConfigReload);
            Mods.modLoader().event(UEvent.INTERCEPT_PACKET).listen(this::onInterceptPacket);
            this.logLevel.set(Settings.get().getLogPackets());
        }

        public InteractionResult onConfigReload(ConfigEventPayload.Read event) {
            if (event.object() instanceof Settings settings) {
                this.logLevel.set(settings.getLogPackets());
            }
            return InteractionResult.PASS;
        }

        public InteractionResult onInterceptPacket(PacketInterceptEventPayload event) {
            var logLevel = this.logLevel.get();
            if (logLevel != AtlantisLogger.Severity.NONE) {
                var player = event.player();
                var custom = event.isCustomPacket();
                AtlantisLogger.log(logLevel, PACKET_LOG,
                        player == null ? SOURCELESS : player.getName().getString(),
                        event.isPacketClientbound() ? CLIENTBOUND : SERVERBOUND,
                        custom ? CUSTOM : VANILLA,
                        ClassNameCache.name(custom ? event.customPacket().getClass() : event.packet().getClass())
                );
            }
            return InteractionResult.PASS;
        }
    }
}
