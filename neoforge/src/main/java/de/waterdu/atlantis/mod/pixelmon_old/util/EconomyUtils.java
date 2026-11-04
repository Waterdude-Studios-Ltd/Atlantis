package de.waterdu.atlantis.mod.pixelmon_old.util;

import com.pixelmonmod.pixelmon.api.economy.BankAccount;
import com.pixelmonmod.pixelmon.api.economy.BankAccountProxy;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.Util;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads and changes the balance of the Pixelmon bank account of a player.
 */
public class EconomyUtils {

    public static final BankAccount DUMMY_ACCOUNT = new BankAccount() {

        @Override
        public UUID getIdentifier() {
            return Util.NIL_UUID;
        }

        @Override
        public BigDecimal getBalance() {
            return BigDecimal.ZERO;
        }

        @Override
        public void setBalance(BigDecimal bigDecimal) {
        }

        @Override
        public boolean hasBalance(BigDecimal bigDecimal) {
            return false;
        }

        @Override
        public boolean take(BigDecimal bigDecimal) {
            return false;
        }

        @Override
        public boolean add(BigDecimal bigDecimal) {
            return false;
        }
    };

    private EconomyUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Gets the Pixelmon bank account of the player, if Pixelmon has loaded it.
     *
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @param <T> the type of the player object
     * @return the bank account, or an empty optional if the player or the account is not found
     */
    private static <T> Optional<? extends BankAccount> getAccount(T player) {
        return PlayerReference.getSafely(player).flatMap(p -> Optional.ofNullable(BankAccountProxy.getBankAccountNow(p.uuid())));
    }

    /**
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @return zero if the player has no bank account. This also logs an error.
     */
    public static <T> BigDecimal getBalance(T player) {
        return EconomyUtils.getAccount(player).map(BankAccount::getBalance).orElseGet(() -> {
            EconomyUtils.throwBankAccountError(player);
            return BigDecimal.ZERO;
        });
    }

    /**
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @return also false if the player has no bank account. This also logs an error.
     */
    public static <T> boolean hasBalance(T player, long amount) {
        return EconomyUtils.getAccount(player).map(account -> account.hasBalance(amount)).orElseGet(() -> {
            EconomyUtils.throwBankAccountError(player);
            return false;
        });
    }
    
    /**
     * Checks if the player has at least the amount in their bank account.
     *
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @param amount the amount to check for
     * @param <T> the type of the player object
     * @return also {@code false} if the player has no bank account. This also logs an error.
     */
    public static <T> boolean hasBalance(T player, int amount) {
        return EconomyUtils.hasBalance(player, (long) amount);
    }
    
    /**
     * Adds the amount to the bank account of the player. If the amount is not positive, this calls
     * {@link #deductBalance(Object, long)} with the same amount.
     *
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @param amount the amount to add
     * @param <T> the type of the player object
     * @return {@code false} if the change fails or the player has no bank account. This also logs an error.
     */
    public static <T> boolean addBalance(T player, long amount) {
        if (amount > 0) {
            return EconomyUtils.getAccount(player).map(account -> account.add(amount)).orElseGet(() -> {
                EconomyUtils.throwBankAccountError(player);
                return false;
            });
        } else {
            return EconomyUtils.deductBalance(player, amount);
        }
    }

    /**
     * Adds the amount to the bank account of the player, as {@link #addBalance(Object, long)} does.
     *
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @param amount the amount to add
     * @param <T> the type of the player object
     * @return {@code false} if the change fails or the player has no bank account
     */
    public static <T> boolean addBalance(T player, int amount) {
        return EconomyUtils.addBalance(player, (long) amount);
    }
    
    /**
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @return false if the balance is too low or the player has no bank account
     */
    public static <T> boolean deductBalance(T player, long amount) {
        return EconomyUtils.getAccount(player).map(account -> account.hasBalance(amount) && account.take(amount)).orElseGet(() -> {
            EconomyUtils.throwBankAccountError(player);
            return false;
        });
    }

    /**
     * Takes the amount from the bank account of the player, as {@link #deductBalance(Object, long)} does.
     *
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @param amount the amount to take
     * @param <T> the type of the player object
     * @return {@code false} if the balance is too low or the player has no bank account
     */
    public static <T> boolean deductBalance(T player, int amount) {
        return EconomyUtils.deductBalance(player, (long) amount);
    }

    /**
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @return false if the player has no bank account. This also logs an error.
     */
    public static <T> boolean setBalance(T player, long amount) {
        return EconomyUtils.getAccount(player).map(account -> {
            account.setBalance(amount);
            return true;
        }).orElseGet(() -> {
            EconomyUtils.throwBankAccountError(player);
            return false;
        });
    }

    /**
     * Sets the balance of the bank account of the player, as {@link #setBalance(Object, long)} does.
     *
     * @param player any type that {@link PlayerReference#getSafely(Object)} accepts
     * @param amount the new balance
     * @param <T> the type of the player object
     * @return {@code false} if the player has no bank account. This also logs an error.
     */
    public static <T> boolean setBalance(T player, int amount) {
        return EconomyUtils.setBalance(player, (long) amount);
    }

    /**
     * Logs an error that the bank account of the player was not found. Despite the name, this does not throw.
     *
     * @param player any type that {@link PlayerReference#get(Object)} accepts
     * @param <T> the type of the player object
     */
    private static <T> void throwBankAccountError(T player) {
        var reference = PlayerReference.get(player);
        if (reference != null) {
            AtlantisLogger.error("Failed to fetch bank account for player {} ({})", reference.uuid(), reference.name());
        } else {
            AtlantisLogger.error("Failed to fetch bank account for unknown player {}", player.toString());
        }
    }
}
