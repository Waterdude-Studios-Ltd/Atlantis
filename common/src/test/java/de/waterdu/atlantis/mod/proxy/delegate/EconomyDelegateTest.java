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

class EconomyDelegateTest {

    private final PlayerReference source = mock(PlayerReference.class);
    private final PlayerReference destination = mock(PlayerReference.class);
    private final FakeEconomy economy = new FakeEconomy();

    @Test
    void ofBooleanSuccessRecordsTransaction() {
        var result = Transaction.Result.ofBoolean(true, this.source, BigDecimal.TEN);

        assertThat(result).isInstanceOf(Transaction.Result.Success.class);
        assertThat(result.successful()).isTrue();
        assertThat(result.transactions()).containsExactly(new Transaction(this.source, BigDecimal.TEN));
    }

    @Test
    void ofBooleanWithoutDetailsRecordsNothing() {
        assertThat(Transaction.Result.ofBoolean(true).transactions()).isEmpty();
        assertThat(Transaction.Result.ofBoolean(true, this.source, null).transactions()).isEmpty();
    }

    @Test
    void ofBooleanFailureRecordsNothing() {
        var result = Transaction.Result.ofBoolean(false, this.source, BigDecimal.TEN);

        assertThat(result).isInstanceOf(Transaction.Result.Failure.class);
        assertThat(result.successful()).isFalse();
        assertThat(result.transactions()).isEmpty();
    }

    @Test
    void onlySuccessIsSuccessful() {
        assertThat(new Transaction.Result.Success().successful()).isTrue();
        assertThat(new Transaction.Result.Failure().successful()).isFalse();
        assertThat(new Transaction.Result.Error().successful()).isFalse();
        assertThat(new Transaction.Result.Ignore().successful()).isFalse();
    }

    @Test
    void copyConstructorCopiesTransactionsWithoutSharingThem() {
        var original = new Transaction.Result.Success().transact(this.source, BigDecimal.ONE);
        var copy = new Transaction.Result.Failure(original);
        copy.transact(this.destination, BigDecimal.TEN);

        assertThat(copy.transactions()).containsExactly(new Transaction(this.source, BigDecimal.ONE), new Transaction(this.destination, BigDecimal.TEN));
        assertThat(original.transactions()).containsExactly(new Transaction(this.source, BigDecimal.ONE));
    }

    @Test
    void transactIgnoresNull() {
        assertThat(new Transaction.Result.Success().transact(null).transactions()).isEmpty();
    }

    @Test
    void includeAppendsTransactions() {
        var first = new Transaction.Result.Success().transact(this.source, BigDecimal.ONE);
        var second = new Transaction.Result.Success().transact(this.destination, BigDecimal.TWO);

        assertThat(first.include(second).transactions()).containsExactly(new Transaction(this.source, BigDecimal.ONE), new Transaction(this.destination, BigDecimal.TWO));
    }

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

    @Test
    void transferWithInsufficientFundsChangesNothing() {
        this.economy.balances.put(this.source, BigDecimal.valueOf(10));

        var result = this.economy.transfer(this.source, this.destination, BigDecimal.valueOf(30)).join();

        assertThat(result).isInstanceOf(Transaction.Result.Failure.class);
        assertThat(result.transactions()).isEmpty();
        assertThat(this.economy.balance(this.source).join()).isEqualByComparingTo("10");
        assertThat(this.economy.balance(this.destination).join()).isEqualByComparingTo("0");
    }

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

    @Test
    void ignoredDepositIsRefunded() {
        this.economy.balances.put(this.source, BigDecimal.valueOf(100));
        this.economy.ignoreDeposits.add(this.destination);

        var result = this.economy.transfer(this.source, this.destination, BigDecimal.valueOf(30)).join();

        assertThat(result).isInstanceOf(Transaction.Result.Failure.class);
        assertThat(this.economy.balance(this.source).join()).isEqualByComparingTo("100");
    }

    private static final class FakeEconomy implements EconomyDelegate<BigDecimal> {

        private final Map<PlayerReference, BigDecimal> balances = new HashMap<>();
        private final Set<PlayerReference> rejectDeposits = new HashSet<>();
        private final Set<PlayerReference> ignoreDeposits = new HashSet<>();

        @Override
        public CompletableFuture<BigDecimal> account(PlayerReference player) {
            return this.balance(player);
        }

        @Override
        public CompletableFuture<BigDecimal> balance(PlayerReference player) {
            return CompletableFuture.completedFuture(this.balances.getOrDefault(player, BigDecimal.ZERO));
        }

        @Override
        public CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount) {
            return this.balance(player).thenApply(balance -> Transaction.Result.ofBoolean(balance.compareTo(amount) >= 0));
        }

        @Override
        public CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount) {
            var balance = this.balances.getOrDefault(player, BigDecimal.ZERO);
            if (balance.compareTo(amount) < 0) {
                return CompletableFuture.completedFuture(Transaction.Result.ofBoolean(false));
            }
            this.balances.put(player, balance.subtract(amount));
            return CompletableFuture.completedFuture(Transaction.Result.ofBoolean(true, player, amount.negate()));
        }

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
