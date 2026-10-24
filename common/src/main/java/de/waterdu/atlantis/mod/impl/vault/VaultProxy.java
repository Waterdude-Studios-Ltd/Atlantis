package de.waterdu.atlantis.mod.impl.vault;

import de.waterdu.atlantis.mod.plugin.PluginBridge;
import de.waterdu.atlantis.mod.plugin.PluginBridgeFactory;
import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import de.waterdu.atlantis.util.server.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class VaultProxy implements EconomyDelegate<OfflinePlayer> {

    public static final String VAULT = "Vault";
    private static final Lazy<PluginBridge> BRIDGE = Lazy.concurrentOf(() -> PluginBridgeFactory.make(VAULT));
    private static final Lazy<Economy> ECONOMY = Lazy.concurrentOf(Economy::find);

    private static final String VAULT_ECONOMY_CLASSPATH = "net.milkbowl.vault.economy.Economy";
    private static final String VAULT_ECONOMY_RESPONSE_CLASSPATH = "net.milkbowl.vault.economy.EconomyResponse";

    @Override
    public CompletableFuture<OfflinePlayer> account(PlayerReference player) {
        return this.call(economy -> this.account(economy, player));
    }

    @Override
    public CompletableFuture<BigDecimal> balance(PlayerReference player) {
        return this.call(economy -> BigDecimal.valueOf(economy.<Double>call("getBalance", this.account(economy, player))));
    }

    @Override
    public CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount) {
        return this.call(economy -> Transaction.Result.ofBoolean(economy.<Boolean>call("has", this.account(economy, player), amount.doubleValue())));
    }

    @Override
    public CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount) {
        return this.call(economy -> {
            var response = economy.call("withdrawPlayer", this.account(economy, player), amount.doubleValue());
            return Transaction.Result.ofBoolean(economy.successful(response), player, economy.amount(response).negate());
        });
    }

    @Override
    public CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount) {
        return this.call(economy -> {
            var response = economy.call("depositPlayer", this.account(economy, player), amount.doubleValue());
            return Transaction.Result.ofBoolean(economy.successful(response), player, economy.amount(response));
        });
    }

    private OfflinePlayer account(Economy economy, PlayerReference player) {
        var account = Bukkit.getOfflinePlayer(player.uuid());
        if (!economy.<Boolean>call("hasAccount", account) && !economy.<Boolean>call("createPlayerAccount", account)) {
            throw new CompletionException(new MissingAccountException(player));
        }
        return account;
    }

    private <T> CompletableFuture<T> call(Function<Economy, T> action) {
        var server = ServerUtils.server();
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Vault economy used without a running server"));
        }
        Executor executor = server.isSameThread() ? Runnable::run : server;
        return CompletableFuture.supplyAsync(() -> action.apply(ECONOMY.get()), executor);
    }

    private record Economy(PluginBridge bridge, Class<?> type, Object provider) {

        private static Economy find() {
            if (!Bukkit.getPluginManager().isPluginEnabled(VAULT)) {
                throw new IllegalStateException("Vault is not enabled");
            }
            var bridge = BRIDGE.get();
            if (bridge == null) {
                throw new IllegalStateException("Bridge is not initialized");
            }
            var type = bridge.findClass(VAULT_ECONOMY_CLASSPATH).orElseThrow(() -> new IllegalStateException("Vault's economy API was not found"));
            var registration = Bukkit.getServicesManager().getRegistration(type);
            if (registration == null) {
                throw new IllegalStateException("No economy plugin is registered with Vault");
            }
            return new Economy(bridge, type, registration.getProvider());
        }

        private <T> T call(String method, OfflinePlayer player) {
            return this.invoke(this.type, this.provider, method, new Class<?>[]{OfflinePlayer.class}, player);
        }

        private <T> T call(String method, OfflinePlayer player, double amount) {
            return this.invoke(this.type, this.provider, method, new Class<?>[]{OfflinePlayer.class, double.class}, player, amount);
        }

        private boolean successful(Object response) {
            return this.<Boolean>invoke(this.responseType(), response, "transactionSuccess", new Class<?>[0]);
        }

        private BigDecimal amount(Object response) {
            var amount = this.bridge.<Double>findField(this.responseType(), "amount")
                    .map(field -> field.get(response))
                    .orElseThrow(() -> new IllegalStateException("Failed to read the amount of a Vault economy response"));
            return BigDecimal.valueOf(amount);
        }

        private Class<?> responseType() {
            return this.bridge.findClass(VAULT_ECONOMY_RESPONSE_CLASSPATH).orElseThrow(() -> new IllegalStateException("Vault's economy response was not found"));
        }

        private <T> T invoke(Class<?> type, Object instance, String method, Class<?>[] parameters, Object... arguments) {
            return this.bridge.<T>findMethod(type, method, parameters)
                    .map(it -> it.invoke(instance, arguments))
                    .orElseThrow(() -> new IllegalStateException("Failed to call Vault economy method " + method));
        }
    }
}
