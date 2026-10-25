package de.waterdu.atlantis.trident;

import de.waterdu.atlantis.util.java.ExceptionUtils;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.service.MixinService;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

final class TineMixins {

    private static final String MOD_ID_DECORATION = "fabric-modId";

    private TineMixins() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    static void register(Collection<TineContainer> tines) {
        var owners = new LinkedHashMap<String, String>();
        for (var tine : tines) {
            for (var config : tine.metadata().mixins()) {
                var owner = owners.putIfAbsent(config, tine.id());
                if (owner != null) {
                    TridentLogger.error("Skipping mixin config {} of Tine {}, Tine {} already declares it", config, tine.id(), owner);
                }
            }
        }

        if (owners.isEmpty()) {
            return;
        }

        owners.keySet().forEach(Mixins::addConfiguration);

        try {
            TineMixins.decorate(owners);
        } catch (Throwable e) {
            TridentLogger.error("Failed to access Mixin's configs, Tine mixins only apply if no other mixin has been applied yet", e);
            return;
        }

        TridentLogger.info("Applying {} Tine mixin config(s): {}", owners.size(), String.join(", ", owners.keySet()));
        TineMixins.select();
    }

    private static void decorate(Map<String, String> owners) throws Throwable {
        var found = new HashSet<String>();
        for (Object handle : Mixins.getConfigs()) {
            var type = handle.getClass();
            var config = (IMixinConfig) TineMixins.lookup(type).findVirtual(type, "getConfig", MethodType.methodType(IMixinConfig.class)).invoke(handle);
            var owner = owners.get(config.getName());
            if (owner != null && found.add(config.getName())) {
                config.decorate(MOD_ID_DECORATION, owner);
            }
        }

        owners.forEach((config, owner) -> {
            if (!found.contains(config)) {
                TridentLogger.error("Mixin config {} of Tine {} could not be read", config, owner);
            }
        });
    }

    private static void select() {
        var environment = MixinEnvironment.getCurrentEnvironment();
        var transformer = environment.getActiveTransformer();
        if (transformer == null) {
            return;
        }

        Object processor;
        MethodHandle select;
        try {
            var type = transformer.getClass();
            processor = TineMixins.lookup(type).findGetter(type, "processor", type.getDeclaredField("processor").getType()).invoke(transformer);
            select = TineMixins.lookup(processor.getClass()).findVirtual(processor.getClass(), "select", MethodType.methodType(void.class, MixinEnvironment.class));
        } catch (Throwable e) {
            TridentLogger.error("Failed to find Mixin's processor, Tine mixins only apply if no other mixin has been applied yet", e);
            return;
        }

        var lock = MixinService.getService().getReEntranceLock();
        synchronized (processor) {
            lock.push();
            try {
                select.invoke(processor, environment);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            } finally {
                lock.pop();
            }
        }
    }

    private static MethodHandles.Lookup lookup(Class<?> type) throws IllegalAccessException {
        return MethodHandles.privateLookupIn(type, MethodHandles.lookup());
    }
}
