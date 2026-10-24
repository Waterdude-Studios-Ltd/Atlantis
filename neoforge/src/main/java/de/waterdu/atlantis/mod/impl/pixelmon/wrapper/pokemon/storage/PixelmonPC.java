package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.storage.PCBox;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PixelmonPC extends UPC<PCStorage, PixelmonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pc");

    private final UUID owner;

    public PixelmonPC(UUID owner, CompletableFuture<PCStorage> pc) {
        super(TYPE, pc);
        this.owner = owner;
    }

    @Override
    public UUID owner() {
        return this.owner;
    }

    @Override
    public List<PixelmonPokemon> all() {
        var all = new ArrayList<PixelmonPokemon>();
        if (this.isLoaded()) {
            for (var pokemon : this.dereference().getAll()) {
                if (pokemon != null) {
                    all.add(new PixelmonPokemon(pokemon));
                }
            }
        }
        return all;
    }

    @Override
    public boolean hasSpace() {
        return this.isLoaded() && this.dereference().hasSpace();
    }

    @Override
    public boolean add(UPokemon<?> pokemon) {
        return this.isLoaded() && this.dereference().add(PixelmonPokemon.unwrap(pokemon));
    }

    @Override
    public boolean remove(UPokemon<?> pokemon) {
        if (!this.isLoaded()) {
            return false;
        }
        var pc = this.dereference();
        var position = pc.getPosition(PixelmonPokemon.unwrap(pokemon));
        if (position == null) {
            return false;
        }
        pc.set(position, null);
        return true;
    }

    @Override
    public @Nullable PixelmonPokemon find(UUID uuid) {
        return this.isLoaded() ? PixelmonPokemon.of(this.dereference().find(uuid)) : null;
    }

    @Override
    public @Nullable PixelmonPokemon get(int box, int slot) {
        return this.isLoaded() ? PixelmonPokemon.of(this.dereference().get(box, slot)) : null;
    }

    @Override
    public @Nullable PixelmonPokemon set(@Nullable UPokemon<?> pokemon, int box, int slot) {
        if (!this.isLoaded()) {
            return null;
        }
        var old = this.get(box, slot);
        this.dereference().set(box, slot, pokemon == null ? null : PixelmonPokemon.unwrap(pokemon));
        return old;
    }

    @Override
    public int boxCount() {
        return this.isLoaded() ? this.dereference().getBoxCount() : 0;
    }

    @Override
    public int boxSize() {
        return PCBox.POKEMON_PER_BOX;
    }
}
