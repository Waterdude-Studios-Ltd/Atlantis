package de.waterdu.atlantis.mod.impl.impactor;

import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class ImpactorProxy implements EconomyDelegate<Account> {
    
    private final EconomyService service = EconomyService.instance();

    @Override
    public CompletableFuture<Account> account(PlayerReference player) {
        return this.service.account(player.uuid());
    }

    @Override
    public CompletableFuture<BigDecimal> balance(PlayerReference player) {
        return this.account(player).thenApply(Account::balance);
    }

    @Override
    public CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> Transaction.Result.ofBoolean(account.balance().compareTo(amount) >= 0));
    }

    @Override
    public CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> {
            var result = account.withdraw(amount);
            return Transaction.Result.ofBoolean(result.successful(), player, result.amount().negate());
        });
    }

    @Override
    public CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount) {
        return this.account(player).thenApply(account -> {
            var result = account.deposit(amount);
            return Transaction.Result.ofBoolean(result.successful(), player, result.amount());
        });
    }

    @Override
    public CompletableFuture<Transaction.Result> transfer(PlayerReference source, PlayerReference destination, BigDecimal amount) {
        return EconomyDelegate.super.transfer(source, destination, amount);
    }
}
