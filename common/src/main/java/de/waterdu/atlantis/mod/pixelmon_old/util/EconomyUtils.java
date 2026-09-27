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

    private static <T> Optional<? extends BankAccount> getAccount(T player) {
        return PlayerReference.getSafely(player).flatMap(p -> Optional.ofNullable(BankAccountProxy.getBankAccountNow(p.uuid())));
    }

    public static <T> BigDecimal getBalance(T player) {
        return EconomyUtils.getAccount(player).map(BankAccount::getBalance).orElseGet(() -> {
            EconomyUtils.throwBankAccountError(player);
            return BigDecimal.ZERO;
        });
    }

    public static <T> boolean hasBalance(T player, long amount) {
        return EconomyUtils.getAccount(player).map(account -> account.hasBalance(amount)).orElseGet(() -> {
            EconomyUtils.throwBankAccountError(player);
            return false;
        });
    }
    
    public static <T> boolean hasBalance(T player, int amount) {
        return EconomyUtils.hasBalance(player, (long) amount);
    }
    
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

    public static <T> boolean addBalance(T player, int amount) {
        return EconomyUtils.addBalance(player, (long) amount);
    }
    
    public static <T> boolean deductBalance(T player, long amount) {
        return EconomyUtils.getAccount(player).map(account -> account.hasBalance(amount) && account.take(amount)).orElseGet(() -> {
            EconomyUtils.throwBankAccountError(player);
            return false;
        });
    }

    public static <T> boolean deductBalance(T player, int amount) {
        return EconomyUtils.deductBalance(player, (long) amount);
    }

    public static <T> boolean setBalance(T player, long amount) {
        return EconomyUtils.getAccount(player).map(account -> {
            account.setBalance(amount);
            return true;
        }).orElseGet(() -> {
            EconomyUtils.throwBankAccountError(player);
            return false;
        });
    }

    public static <T> boolean setBalance(T player, int amount) {
        return EconomyUtils.setBalance(player, (long) amount);
    }

    private static <T> void throwBankAccountError(T player) {
        var reference = PlayerReference.get(player);
        if (reference != null) {
            AtlantisLogger.error("Failed to fetch bank account for player {} ({})", reference.uuid(), reference.name());
        } else {
            AtlantisLogger.error("Failed to fetch bank account for unknown player {}", player.toString());
        }
    }
}
