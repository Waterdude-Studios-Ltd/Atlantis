package de.waterdu.atlantis.mod.proxy.delegate;

import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.util.entity.PlayerReference;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gives access to the economy of a mod or plugin. Get the active one with {@code Mods.economy()}.
 *
 * @param <Account> the type of the economy account
 */
public interface EconomyDelegate<Account> extends ModProxy {

    /**
     * Gets the economy account of the player.
     *
     * @param player the player to get the account of
     * @return the account
     */
    CompletableFuture<Account> account(PlayerReference player);

    /**
     * Gets the balance of the player.
     *
     * @param player the player to get the balance of
     * @return the balance
     */
    CompletableFuture<BigDecimal> balance(PlayerReference player);

    /**
     * @return a successful result if the player has at least the amount. The result records no transactions.
     */
    CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount);

    /**
     * @return on success, the result records the withdrawal as a negative change
     */
    CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount);

    /**
     * Deposits the amount to the player.
     *
     * @param player the player to deposit to
     * @param amount the amount to deposit
     * @return on success, the result records the deposit as a positive change
     */
    CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount);

    /**
     * Withdraws from the source, then deposits to the destination. If the deposit fails, deposits the amount back to
     * the source.
     *
     * @return the result records each transaction that happened, including a refund
     */
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

    /**
     * Records one change to the balance of a player.
     *
     * @param player the player whose balance changed
     * @param change the change. A withdrawal is negative.
     */
    record Transaction(PlayerReference player, BigDecimal change) {

        /**
         * Tells if an economy operation was successful, and records the transactions that happened.
         */
        public sealed abstract static class Result permits
                Result.Success,
                Result.Failure,
                Result.Error,
                Result.Ignore {

            private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

            /**
             * Creates a result with no transactions.
             */
            protected Result() { /* noop */ }

            /**
             * Creates a result with a copy of the transactions of another result.
             *
             * @param other the result to copy the transactions from
             */
            protected Result(Result other) {
                this.transactions.addAll(other.transactions);
            }

            /**
             * @return the transactions that this result records, in order
             */
            public List<Transaction> transactions() {
                return this.transactions;
            }

            /**
             * Records the transaction.
             *
             * @param transaction the transaction to record. If it is {@code null}, nothing is recorded.
             * @return this result
             */
            public Result transact(@Nullable Transaction transaction) {
                if (transaction != null) {
                    this.transactions.add(transaction);
                }
                return this;
            }

            /**
             * Records a transaction with the player and the change.
             *
             * @param player the player whose balance changed
             * @param change the change to the balance
             * @return this result
             */
            public Result transact(PlayerReference player, BigDecimal change) {
                return this.transact(new Transaction(player, change));
            }

            /**
             * Adds all transactions of another result to this result.
             *
             * @param other the result to add the transactions of
             * @return this result
             */
            public Result include(Result other) {
                this.transactions.addAll(other.transactions);
                return this;
            }

            /**
             * Tells that the operation was successful.
             */
            public static final class Success extends Result {

                /**
                 * Creates a successful result with no transactions.
                 */
                public Success() {
                    super();
                }

                /**
                 * Creates a successful result with a copy of the transactions of another result.
                 *
                 * @param other the result to copy the transactions from
                 */
                public Success(Result other) {
                    super(other);
                }

                /**
                 * @return {@code true}
                 */
                @Override
                public boolean successful() {
                    return true;
                }
            }

            /**
             * Tells that the operation failed.
             */
            public static final class Failure extends Result {

                /**
                 * Creates a failed result with no transactions.
                 */
                public Failure() {
                    super();
                }

                /**
                 * Creates a failed result with a copy of the transactions of another result.
                 *
                 * @param other the result to copy the transactions from
                 */
                public Failure(Result other) {
                    super(other);
                }

                /**
                 * @return {@code false}
                 */
                @Override
                public boolean successful() {
                    return false;
                }
            }

            /**
             * Tells that an error stopped the operation.
             */
            public static final class Error extends Result {

                /**
                 * Creates an error result with no transactions.
                 */
                public Error() {
                    super();
                }

                /**
                 * Creates an error result with a copy of the transactions of another result.
                 *
                 * @param other the result to copy the transactions from
                 */
                public Error(Result other) {
                    super(other);
                }

                /**
                 * @return {@code false}
                 */
                @Override
                public boolean successful() {
                    return false;
                }
            }

            /**
             * Tells that the operation was ignored.
             */
            public static final class Ignore extends Result {

                /**
                 * Creates an ignored result with no transactions.
                 */
                public Ignore() {
                    super();
                }

                /**
                 * Creates an ignored result with a copy of the transactions of another result.
                 *
                 * @param other the result to copy the transactions from
                 */
                public Ignore(Result other) {
                    super(other);
                }

                /**
                 * @return {@code false}
                 */
                @Override
                public boolean successful() {
                    return false;
                }
            }

            /**
             * Creates a result with no transactions from a boolean.
             *
             * @param result {@code true} for success
             * @return an empty {@link Success} or an empty {@link Failure}
             */
            public static Result ofBoolean(boolean result) {
                return Result.ofBoolean(result, null, null);
            }

            /**
             * @param player the player of the transaction to record on success. Can be {@code null}.
             * @param amount the change to record on success. Can be {@code null}.
             * @return a {@link Success} that records the transaction, or an empty {@link Failure}
             */
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

            /**
             * Checks if the operation was successful.
             *
             * @return {@code true} only for a {@link Success}
             */
            public abstract boolean successful();
        }
    }

    /**
     * Tells that the economy has no account for a player and cannot create one.
     */
    class MissingAccountException extends Exception {

        /**
         * Creates an exception with a message that holds the UUID of the player.
         *
         * @param player the player with no account
         */
        public MissingAccountException(PlayerReference player) {
            super("No backing economy account found for player " + player.uuid());
        }
    }
}
