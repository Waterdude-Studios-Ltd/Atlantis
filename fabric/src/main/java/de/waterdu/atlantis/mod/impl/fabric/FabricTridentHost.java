package de.waterdu.atlantis.mod.impl.fabric;

import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.VersionCheck;
import de.waterdu.atlantis.trident.TridentHost;
import de.waterdu.atlantis.trident.Trident;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FabricTridentHost implements TridentHost {

    private static final String BUILTIN_MOD_TYPE = "builtin";
    private static final String ISSUES_CONTACT = "issues";
    private static final String FABRIC_API_MODULE = "fabric-api:module-lifecycle";

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId) || Trident.isLoaded(modId);
    }

    @Override
    public List<ModInfo> mods() {
        var mods = new ArrayList<ModInfo>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            if (FabricTridentHost.isBuiltin(mod) || mod.getContainingMod().isPresent() || mod.getMetadata().containsCustomValue(FABRIC_API_MODULE)) {
                continue;
            }
            var metadata = mod.getMetadata();
            mods.add(new ModInfo(
                    metadata.getId(),
                    metadata.getName(),
                    metadata.getVersion().getFriendlyString(),
                    metadata.getAuthors().stream().map(Person::getName).toList(),
                    metadata.getContact().get(ISSUES_CONTACT).orElse(null),
                    () -> VersionCheck.UNSUPPORTED
            ));
        }
        mods.addAll(Trident.mods());
        return mods;
    }

    @Override
    public ClassLoader addToClassPath(List<Path> jars) {
        var launcher = FabricLauncherBase.getLauncher();
        TineRemapper.remap(jars, this.configDir()).forEach(launcher::addToClassPath);
        return FabricTridentHost.class.getClassLoader();
    }

    static boolean isBuiltin(ModContainer mod) {
        return BUILTIN_MOD_TYPE.equals(mod.getMetadata().getType());
    }
}
