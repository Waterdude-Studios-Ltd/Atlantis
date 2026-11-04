package de.waterdu.atlantis.mod.proxy.delegate;

import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate.Transaction;
import de.waterdu.atlantis.util.entity.PlayerReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link EconomyDelegate#transfer} and the transaction results of {@link EconomyDelegate}.
 */
class EconomyDelegateTest {

    private final PlayerReference source = mock(PlayerReference.class);
    private final PlayerReference destination = mock(PlayerReference.class);
    private final FakeEconomy economy = new FakeEconomy();

    /**
     * Checks that a successful {@code ofBoolean} result records the player and the amount as a transaction.
     */
    @Test
    void ofBooleanSuccessRecordsTransaction() {
        var result = Transaction.Result.ofBoolean(true, this.source, BigDecimal.TEN);

        assertThat(result).isInstanceOf(Transaction.Result.Success.class);
        assertThat(result.successful()).isTrue();
        assertThat(result.transactions()).containsExactly(new Transaction(this.source, BigDecimal.TEN));
    }

    /**
     * Checks that a successful {@code ofBoolean} result without a player or an amount records no transaction.
     */
    @Test
    void ofBooleanWithoutDetailsRecordsNothing() {
        assertThat(Transaction.Result.ofBoolean(true).transactions()).isEmpty();
        assertThat(Transaction.Result.ofBoolean(true, this.source, null).transactions()).isEmpty();
    }

    /**
     * Checks that a failed {@code ofBoolean} result is a failure and records no transaction.
     */
    @Test
    void ofBooleanFailureRecordsNothing() {
        var result = Transaction.Result.ofBoolean(false, this.source, BigDecimal.TEN);

        assertThat(result).isInstanceOf(Transaction.Result.Failure.class);
        assertThat(result.successful()).isFalse();
        assertThat(result.transactions()).isEmpty();
    }

    /**
     * Checks that only a success result is successful, and failure, error and ignore results are not.
     */
    @Test
    void onlySuccessIsSuccessful() {
        assertThat(new Transaction.Result.Success().successful()).isTrue();
        assertThat(new Transaction.Result.Failure().successful()).isFalse();
        assertThat(new Transaction.Result.Error().successful()).isFalse();
        assertThat(new Transaction.Result.Ignore().successful()).isFalse();
    }

    /**
     * Checks that the copy constructor copies the transactions, so new transactions on the copy do not change the
     * original.
     */
    @Test
    void copyConstructorCopiesTransactionsWithoutSharingThem() {
        var original = new Transaction.Result.Success().transact(this.source, BigDecimal.ONE);
        var copy = new Transaction.Result.Failure(original);
        copy.transact(this.destination, BigDecimal.TEN);

        assertThat(copy.transactions()).containsExactly(new Transaction(this.source, BigDecimal.ONE), new Transaction(this.destination, BigDecimal.TEN));
        assertThat(original.transactions()).containsExactly(new Transaction(this.source, BigDecimal.ONE));
    }

    /**
     * Checks that {@code transact} does not record a {@code null} transaction.
     */
    @Test
    void transactIgnoresNull() {
        assertThat(new Transaction.Result.Success().transact(null).transactions()).isEmpty();
    }

    /**
     * Checks that {@code include} adds the transactions of another result after the existing ones.
     */
    @Test
    void includeAppendsTransactions() {
        var first = new Transaction.Result.Success().transact(this.source, BigDecimal.ONE);
        var second = new Transaction.Result.Success().transact(this.destination, BigDecimal.TWO);

        assertThat(first.include(second).transactions()).containsExactly(new Transaction(this.source, BigDecimal.ONE), new Transaction(this.destination, BigDecimal.TWO));
    }

    /**
     * Checks that a transfer moves the amount from the source to the destination and records both changes.
     */
    @Test
    void transferMovesMoneyAndRecordsBothChanges() {
        this.economy.balances.put(this.source, BigDecimal.valueOf(100));

        var result = this.economy.transfer(this.source, this.destination, BigDecimal.valueOf(30)).join();

        assertThat(result).isInstanceOf(Transaction.Result.Success.class);
        assertThat(this.economy.balance(this.source).join()).isEqualByComparingTo("70");
        assertThat(this.economy.balance(this.destination).join()).isEqualByComparingTo("30");
        assertThat(result.transactions()).containsExactly(
                new Transaction(this.source, BigDecimal.valueOf(-30)),
                new Transaction(this.destination, BigDecimal.valueOf(30))
        );
    }

    /**
     * Checks that a transfer fails and changes no balance when the source does not have enough money.
     */
    @Test
    void transferWithInsufficientFundsChangesNothing() {
        this.economy.balances.put(this.source, BigDecimal.valueOf(10));

        var result = this.economy.transfer(this.source, this.destination, BigDecimal.valueOf(30)).join();

        assertThat(result).isInstanceOf(Transaction.Result.Failure.class);
        assertThat(result.transactions()).isEmpty();
        assertThat(this.economy.balance(this.source).join()).isEqualByComparingTo("10");
        assertThat(this.economy.balance(this.destination).join()).isEqualByComparingTo("0");
    }

    /**
     * Checks that a transfer gives the money back to the source when the deposit fails, and records the refund.
     */
    @Test
    void failedDepositIsRefunded() {
        this.economy.balances.put(this.source, BigDecimal.valueOf(100));
        this.economy.rejectDeposits.add(this.destination);

        var result = this.economy.transfer(this.source, this.destination, BigDecimal.valueOf(30)).join();

        assertThat(result).isInstanceOf(Transaction.Result.Failure.class);
        assertThat(this.economy.balance(this.source).join()).isEqualByComparingTo("100");
        assertThat(result.transactions()).containsExactly(
                new Transaction(this.source, BigDecimal.valueOf(-30)),
                new Transaction(this.source, BigDecimal.valueOf(30))
        );
    }

    /**
     * Checks that a refund that fails is not recorded, so only the withdrawal stays in the result.
     */
    @Test
    void failedRefundIsNotRecorded() {
        this.economy.balances.put(this.source, BigDecimal.valueOf(100));
        this.economy.rejectDeposits.add(this.destination);
        this.economy.rejectDeposits.add(this.source);

        var result = this.economy.transfer(this.source, this.destination, BigDecimal.valueOf(30)).join();

        assertThat(result).isInstanceOf(Transaction.Result.Failure.class);
        assertThat(this.economy.balance(this.source).join()).isEqualByComparingTo("70");
        assertThat(result.transactions()).containsExactly(new Transaction(this.source, BigDecimal.valueOf(-30)));
    }

    /**
     * Checks that a transfer gives the money back to the source when the deposit gives an ignore result.
     */
    @Test
    void ignoredDepositIsRefunded() {
        this.economy.balances.put(this.source, BigDecimal.valueOf(100));
        this.economy.ignoreDeposits.add(this.destination);

        var result = this.economy.transfer(this.source, this.destination, BigDecimal.valueOf(30)).join();

        assertThat(result).isInstanceOf(Transaction.Result.Failure.class);
        assertThat(this.economy.balance(this.source).join()).isEqualByComparingTo("100");
    }

    /**
     * An economy that keeps balances in memory. Tests can make deposits to some players fail or give an ignore
     * result.
     */
    private static final class FakeEconomy implements EconomyDelegate<BigDecimal> {

        private final Map<PlayerReference, BigDecimal> balances = new HashMap<>();
        private final Set<PlayerReference> rejectDeposits = new HashSet<>();
        private final Set<PlayerReference> ignoreDeposits = new HashSet<>();

        /**
         * Gets the balance of the player as the account.
         *
         * @param player the player
         * @return a completed future with the balance of the player
         */
        @Override
        public CompletableFuture<BigDecimal> account(PlayerReference player) {
            return this.balance(player);
        }

        /**
         * Gets the balance of the player.
         *
         * @param player the player
         * @return a completed future with the balance, or zero if the player has no balance
         */
        @Override
        public CompletableFuture<BigDecimal> balance(PlayerReference player) {
            return CompletableFuture.completedFuture(this.balances.getOrDefault(player, BigDecimal.ZERO));
        }

        /**
         * Checks if the balance of the player is at least the amount.
         *
         * @param player the player
         * @param amount the amount
         * @return a future with a success result if the balance is enough, otherwise a failure result
         */
        @Override
        public CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount) {
            return this.balance(player).thenApply(balance -> Transaction.Result.ofBoolean(balance.compareTo(amount) >= 0));
        }

        /**
         * Removes the amount from the balance of the player, if the balance is enough.
         *
         * @param player the player
         * @param amount the amount to remove
         * @return a completed future with a success result that records the negated amount, or a failure result
         */
        @Override
        public CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount) {
            var balance = this.balances.getOrDefault(player, BigDecimal.ZERO);
            if (balance.compareTo(amount) < 0) {
                return CompletableFuture.completedFuture(Transaction.Result.ofBoolean(false));
            }
            this.balances.put(player, balance.subtract(amount));
            return CompletableFuture.completedFuture(Transaction.Result.ofBoolean(true, player, amount.negate()));
        }

        /**
         * Adds the amount to the balance of the player, unless the test makes deposits to the player fail or give an
         * ignore result.
         *
         * @param player the player
         * @param amount the amount to add
         * @return a completed future with a success result that records the amount, a failure result or an ignore
         *     result
         */
        @Override
        public CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount) {
            if (this.ignoreDeposits.contains(player)) {
                return CompletableFuture.completedFuture(new Transaction.Result.Ignore());
            }
            if (this.rejectDeposits.contains(player)) {
                return CompletableFuture.completedFuture(Transaction.Result.ofBoolean(false));
            }
            this.balances.merge(player, amount, BigDecimal::add);
            return CompletableFuture.completedFuture(Transaction.Result.ofBoolean(true, player, amount));
        }
    }
}
