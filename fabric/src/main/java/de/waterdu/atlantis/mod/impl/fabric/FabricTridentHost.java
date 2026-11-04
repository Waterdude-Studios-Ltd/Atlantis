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

/**
 * Gives {@link Trident} the parts of Fabric Loader that it needs. It touches no Minecraft classes, so the
 * {@code preLaunch} entrypoint can use it.
 */
public class FabricTridentHost implements TridentHost {

    private static final String BUILTIN_MOD_TYPE = "builtin";
    private static final String ISSUES_CONTACT = "issues";
    private static final String FABRIC_API_MODULE = "fabric-api:module-lifecycle";

    /**
     * @return the config directory of Fabric Loader
     */
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    /**
     * Checks if Fabric Loader has loaded the mod, or if Trident has loaded a Tine with the ID.
     *
     * @param modId the ID of the mod or Tine
     * @return {@code true} if the mod or Tine is loaded
     */
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId) || Trident.isLoaded(modId);
    }

    /**
     * Gets the info of the Fabric mods and the loaded Tines. Built-in mods, nested mods and Fabric API modules are
     * left out.
     *
     * @return the info of the mods. The version check of each Fabric mod is {@link VersionCheck#UNSUPPORTED}.
     */
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

    /**
     * Remaps the jars with {@link TineRemapper} if necessary, then adds them to the Knot class path.
     *
     * @param jars the jars and directories of the Tines
     * @return the class loader of Atlantis, which now also loads the Tine classes
     */
    @Override
    public ClassLoader addToClassPath(List<Path> jars) {
        var launcher = FabricLauncherBase.getLauncher();
        TineRemapper.remap(jars, this.configDir()).forEach(launcher::addToClassPath);
        return FabricTridentHost.class.getClassLoader();
    }

    /**
     * Checks if the mod has the {@code builtin} type, such as Minecraft and Java.
     *
     * @param mod the mod container
     * @return {@code true} if the mod is built in
     */
    static boolean isBuiltin(ModContainer mod) {
        return BUILTIN_MOD_TYPE.equals(mod.getMetadata().getType());
    }
}
