package de.waterdu.atlantis.mod.impl.impactor;

import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Uses the Impactor economy service as the {@link EconomyDelegate}.
 */
public class ImpactorProxy implements EconomyDelegate<Account> {

    private final Lazy<EconomyService> service = Lazy.concurrentOf(EconomyService::instance);

    /**
     * Gets the Impactor account of the player from the economy service.
     *
     * @param player the player
     * @return a future that completes with the account
     */
    @Override
    public CompletableFuture<Account> account(PlayerReference player) {
        return this.service.get().account(player.uuid());
    }

    /**
     * Gets the balance of the Impactor account of the player.
     *
     * @param player the player
     * @return a future that completes with the balance
     */
    @Override
    public CompletableFuture<BigDecimal> balance(PlayerReference player) {
        return this.account(player).thenApply(Account::balance);
    }

    /**
     * Checks if the Impactor account of the player has at least the given amount.
     *
     * @param player the player
     * @param amount the amount to check
     * @return a future that completes with a successful result if the balance is at least the amount
     */
    @Override
    public CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> Transaction.Result.ofBoolean(account.balance().compareTo(amount) >= 0));
    }

    /**
     * Withdraws the amount from the Impactor account of the player.
     *
     * @param player the player
     * @param amount the amount to withdraw
     * @return a future that completes with the result, which holds the negated amount that Impactor withdrew
     */
    @Override
    public CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> {
            var result = account.withdraw(amount);
            return Transaction.Result.ofBoolean(result.successful(), player, result.amount().negate());
        });
    }

    /**
     * Deposits the amount into the Impactor account of the player.
     *
     * @param player the player
     * @param amount the amount to deposit
     * @return a future that completes with the result, which holds the amount that Impactor deposited
     */
    @Override
    public CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> {
            var result = account.deposit(amount);
            return Transaction.Result.ofBoolean(result.successful(), player, result.amount());
        });
    }

    /**
     * Withdraws the amount from the source and deposits it into the destination, with the default implementation of
     * {@link EconomyDelegate}. If the deposit fails, the amount goes back to the source.
     *
     * @param source the player who pays
     * @param destination the player who receives the amount
     * @param amount the amount to transfer
     * @return a future that completes with the result of the transfer
     */
    @Override
    public CompletableFuture<Transaction.Result> transfer(PlayerReference source, PlayerReference destination, BigDecimal amount) {
        return EconomyDelegate.super.transfer(source, destination, amount);
    }
}
