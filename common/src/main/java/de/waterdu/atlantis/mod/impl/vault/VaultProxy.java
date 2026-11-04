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

/**
 * Uses the Vault economy API of a Bukkit plugin on a hybrid server. Finds the economy provider on each call and runs
 * the call on the server thread.
 */
public class VaultProxy implements EconomyDelegate<OfflinePlayer> {

    public static final String VAULT = "Vault";
    private static final Lazy<PluginBridge> BRIDGE = Lazy.concurrentOf(() -> PluginBridgeFactory.make(VAULT));
    private static final Lazy<Economy> ECONOMY = Lazy.concurrentOf(Economy::find);

    private static final String VAULT_ECONOMY_CLASSPATH = "net.milkbowl.vault.economy.Economy";
    private static final String VAULT_ECONOMY_RESPONSE_CLASSPATH = "net.milkbowl.vault.economy.EconomyResponse";

    /**
     * Gets the Bukkit offline player of the player. Creates a Vault account if the player has none.
     *
     * @param player the player to get the account of
     * @return the account. Fails with {@link MissingAccountException} if Vault cannot create one.
     */
    @Override
    public CompletableFuture<OfflinePlayer> account(PlayerReference player) {
        return this.call(economy -> this.account(economy, player));
    }

    /**
     * Gets the balance of the player from Vault's {@code getBalance}.
     *
     * @param player the player to get the balance of
     * @return the balance
     */
    @Override
    public CompletableFuture<BigDecimal> balance(PlayerReference player) {
        return this.call(economy -> BigDecimal.valueOf(economy.<Double>call("getBalance", this.account(economy, player))));
    }

    /**
     * Checks if the player has at least the amount, with Vault's {@code has}.
     *
     * @param player the player to check
     * @param amount the amount to check for
     * @return a successful result if the player has the amount. The result records no transactions.
     */
    @Override
    public CompletableFuture<Transaction.Result> hasBalance(PlayerReference player, BigDecimal amount) {
        return this.call(economy -> Transaction.Result.ofBoolean(economy.<Boolean>call("has", this.account(economy, player), amount.doubleValue())));
    }

    /**
     * Withdraws the amount from the player with Vault's {@code withdrawPlayer}.
     *
     * @param player the player to withdraw from
     * @param amount the amount to withdraw
     * @return on success, the result records the amount from Vault's response as a negative change
     */
    @Override
    public CompletableFuture<Transaction.Result> withdraw(PlayerReference player, BigDecimal amount) {
        return this.call(economy -> {
            var response = economy.call("withdrawPlayer", this.account(economy, player), amount.doubleValue());
            return Transaction.Result.ofBoolean(economy.successful(response), player, economy.amount(response).negate());
        });
    }

    /**
     * Deposits the amount to the player with Vault's {@code depositPlayer}.
     *
     * @param player the player to deposit to
     * @param amount the amount to deposit
     * @return on success, the result records the amount from Vault's response as a positive change
     */
    @Override
    public CompletableFuture<Transaction.Result> deposit(PlayerReference player, BigDecimal amount) {
        return this.call(economy -> {
            var response = economy.call("depositPlayer", this.account(economy, player), amount.doubleValue());
            return Transaction.Result.ofBoolean(economy.successful(response), player, economy.amount(response));
        });
    }

    /**
     * Gets the Bukkit offline player of the player. Creates a Vault account if the player has none.
     *
     * @param economy the economy to look up the account in
     * @param player the player to get the account of
     * @return the account
     * @throws CompletionException if the player has no account and Vault cannot create one. The cause is a
     *     {@link MissingAccountException}.
     */
    private OfflinePlayer account(Economy economy, PlayerReference player) {
        var account = Bukkit.getOfflinePlayer(player.uuid());
        if (!economy.<Boolean>call("hasAccount", account) && !economy.<Boolean>call("createPlayerAccount", account)) {
            throw new CompletionException(new MissingAccountException(player));
        }
        return account;
    }

    /**
     * Runs the action with the Vault economy on the server thread. If the current thread is the server thread, the
     * action runs at once.
     *
     * @param action the action to run
     * @param <T> the type of the result
     * @return the result of the action. Fails with {@link IllegalStateException} if no server is running.
     */
    private <T> CompletableFuture<T> call(Function<Economy, T> action) {
        var server = ServerUtils.server();
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Vault economy used without a running server"));
        }
        Executor executor = server.isSameThread() ? Runnable::run : server;
        return CompletableFuture.supplyAsync(() -> action.apply(ECONOMY.get()), executor);
    }

    /**
     * Holds the economy provider that is registered with Vault, and calls its methods through reflection.
     *
     * @param bridge the bridge to the Vault plugin
     * @param type the class of Vault's economy API
     * @param provider the registered economy provider
     */
    private record Economy(PluginBridge bridge, Class<?> type, Object provider) {

        /**
         * Finds the economy provider that is registered with Vault.
         *
         * @return the economy
         * @throws IllegalStateException if Vault is not enabled, the bridge or the API is missing, or no economy
         *     plugin is registered
         */
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

        /**
         * Calls an economy method that takes only an offline player.
         *
         * @param method the name of the method
         * @param player the player to pass
         * @param <T> the return type of the method
         * @return the return value of the method
         */
        private <T> T call(String method, OfflinePlayer player) {
            return this.invoke(this.type, this.provider, method, new Class<?>[]{OfflinePlayer.class}, player);
        }

        /**
         * Calls an economy method that takes an offline player and an amount.
         *
         * @param method the name of the method
         * @param player the player to pass
         * @param amount the amount to pass
         * @param <T> the return type of the method
         * @return the return value of the method
         */
        private <T> T call(String method, OfflinePlayer player, double amount) {
            return this.invoke(this.type, this.provider, method, new Class<?>[]{OfflinePlayer.class, double.class}, player, amount);
        }

        /**
         * Checks if a Vault economy response reports success, with its {@code transactionSuccess} method.
         *
         * @param response the economy response
         * @return {@code true} if the transaction was successful
         */
        private boolean successful(Object response) {
            return this.<Boolean>invoke(this.responseType(), response, "transactionSuccess", new Class<?>[0]);
        }

        /**
         * Reads the {@code amount} field of a Vault economy response.
         *
         * @param response the economy response
         * @return the amount of the transaction
         * @throws IllegalStateException if the field cannot be read
         */
        private BigDecimal amount(Object response) {
            var amount = this.bridge.<Double>findField(this.responseType(), "amount")
                    .map(field -> field.get(response))
                    .orElseThrow(() -> new IllegalStateException("Failed to read the amount of a Vault economy response"));
            return BigDecimal.valueOf(amount);
        }

        /**
         * Gets the class of Vault's economy response.
         *
         * @return the class
         * @throws IllegalStateException if the class is not found
         */
        private Class<?> responseType() {
            return this.bridge.findClass(VAULT_ECONOMY_RESPONSE_CLASSPATH).orElseThrow(() -> new IllegalStateException("Vault's economy response was not found"));
        }

        /**
         * Calls a method through the plugin bridge.
         *
         * @param type the class that declares the method
         * @param instance the object to call the method on
         * @param method the name of the method
         * @param parameters the parameter types of the method
         * @param arguments the arguments to pass
         * @param <T> the return type of the method
         * @return the return value of the method
         * @throws IllegalStateException if the method is not found, or the call fails or returns {@code null}
         */
        private <T> T invoke(Class<?> type, Object instance, String method, Class<?>[] parameters, Object... arguments) {
            return this.bridge.<T>findMethod(type, method, parameters)
                    .map(it -> it.invoke(instance, arguments))
                    .orElseThrow(() -> new IllegalStateException("Failed to call Vault economy method " + method));
        }
    }
}
