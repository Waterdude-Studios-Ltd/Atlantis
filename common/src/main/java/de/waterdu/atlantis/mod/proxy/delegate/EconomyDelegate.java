package de.waterdu.atlantis.mod.proxy.delegate;

import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.util.entity.PlayerReference;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public interface EconomyDelegate<Account> extends ModProxy {

    CompletableFuture<Account> account(PlayerReference player);

    CompletableFuture<BigDecimal> balance(PlayerReference player);

    CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount);

    CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount);

    CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount);

    default CompletableFuture<Transaction.Result> transfer(PlayerReference source, PlayerReference destination, BigDecimal amount) {
        return this.withdraw(source, amount).thenCompose(withdrawal -> {
            if (!withdrawal.successful()) {
                return CompletableFuture.completedFuture(new Transaction.Result.Failure(withdrawal));
            }
            return this.deposit(destination, amount).thenCompose(deposit -> {
                if (deposit.successful()) {
                    return CompletableFuture.completedFuture(new Transaction.Result.Success(withdrawal).include(deposit));
                }
                return this.deposit(source, amount).thenApply(refund -> new Transaction.Result.Failure(withdrawal).include(deposit).include(refund));
            });
        });
    }

    record Transaction(PlayerReference player, BigDecimal change) {

        public sealed abstract static class Result permits
                Result.Success,
                Result.Failure,
                Result.Error,
                Result.Ignore {

            private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

            protected Result() { /* noop */ }

            protected Result(Result other) {
                this.transactions.addAll(other.transactions);
            }

            public List<Transaction> transactions() {
                return this.transactions;
            }

            public Result transact(@Nullable Transaction transaction) {
                if (transaction != null) {
                    this.transactions.add(transaction);
                }
                return this;
            }

            public Result transact(PlayerReference player, BigDecimal change) {
                return this.transact(new Transaction(player, change));
            }

            public Result include(Result other) {
                this.transactions.addAll(other.transactions);
                return this;
            }

            public static final class Success extends Result {

                public Success() {
                    super();
                }

                public Success(Result other) {
                    super(other);
                }

                @Override
                public boolean successful() {
                    return true;
                }
            }

            public static final class Failure extends Result {

                public Failure() {
                    super();
                }

                public Failure(Result other) {
                    super(other);
                }

                @Override
                public boolean successful() {
                    return false;
                }
            }

            public static final class Error extends Result {

                public Error() {
                    super();
                }

                public Error(Result other) {
                    super(other);
                }

                @Override
                public boolean successful() {
                    return false;
                }
            }

            public static final class Ignore extends Result {

                public Ignore() {
                    super();
                }

                public Ignore(Result other) {
                    super(other);
                }

                @Override
                public boolean successful() {
                    return false;
                }
            }

            public static Result ofBoolean(boolean result) {
                return Result.ofBoolean(result, null, null);
            }

            public static Result ofBoolean(boolean result, PlayerReference player, BigDecimal amount) {
                if (result) {
                    var success = new Success();
                    if (player != null && amount != null) {
                        success.transact(player, amount);
                    }
                    return success;
                } else {
                    return new Failure();
                }
            }

            public abstract boolean successful();
        }
    }

    class MissingAccountException extends Exception {

        public MissingAccountException(PlayerReference player) {
            super("No backing economy account found for player " + player.uuid());
        }
    }
}
