package com.commandert3706.interaction;

import com.commandert3706.BlockchainCraft;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class AccountManager {
    private final HashMap<UUID, List<UUID>> playerAccounts = new HashMap<>();
    private final HashMap<UUID, String> accountList = new HashMap<>();

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public void fetchAccountsOnPlayerLogin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        UUID playerId = handler.player.getUuid();

        fetchAccountsForPlayer(playerId);
    }

    public void fetchAccountsForPlayer(UUID playerId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:3000/accounts/%s".formatted(playerId.toString())))
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            ReadableAccountResponse response = GSON.fromJson(httpResponse.body(), ReadableAccountResponse.class);

            List<UUID> accountIds = new ArrayList<>();

            for (ReadableAccountResponse.ReadableAccount readableAccount : response.getReadableAccounts()) {
                UUID accountId = UUID.fromString(readableAccount.getAccountId());

                accountIds.add(accountId);
                accountList.put(accountId, readableAccount.getAccountName());
            }

            playerAccounts.put(playerId, accountIds);

            BlockchainCraft.LOGGER.info("Fetched accounts for player: %s".formatted(playerId.toString()));
        }
        catch (Exception e) {
            BlockchainCraft.LOGGER.error("HTTP request failed");
        }
    }

    public List<UUID> getPlayerAccountUUIDs(UUID playerId) {
        return playerAccounts.get(playerId);
    }

    public List<String> getPlayerAccountNames(UUID playerId) {
        List<UUID> uuids = getPlayerAccountUUIDs(playerId);
        List<String> accountNames = new ArrayList<>();

        if (uuids != null) {
            for (UUID uuid : uuids) {
                String accountName = getAccountName(uuid);

                if (accountName != null) {
                    accountNames.add(accountName);
                }
            }
        }

        return accountNames;
    }

    public String getAccountName(UUID accountId) {
        return accountList.get(accountId);
    }

    public void addPlayerAccount(UUID playerId, String accountName) throws Exception {
        JsonObject postData = new JsonObject();
        postData.addProperty("player_id", playerId.toString());
        postData.addProperty("account_name", accountName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:3000/accounts/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(postData)))
                .build();

        HttpResponse<String> httpResponse = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject response = GSON.fromJson(httpResponse.body(), JsonObject.class);

        fetchAccountsForPlayer(playerId);
    }

    public void removePlayerAccount(UUID playerId, UUID accountId) throws Exception {
        JsonObject postData = new JsonObject();
        postData.addProperty("account_id", accountId.toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:3000/accounts/delete"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(postData)))
                .build();

        CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        accountList.remove(accountId);

        fetchAccountsForPlayer(playerId);
    }

    public UUID getPlayerAccountByName(UUID playerId, String accountName) {
        List<UUID> accounts = getPlayerAccountUUIDs(playerId);

        if (accounts == null) {
            return null;
        }

        for (UUID accountId : accounts) {
            String workingAccountName = getAccountName(accountId);

            if (workingAccountName != null && workingAccountName.equals(accountName)) {
                return accountId;
            }
        }

        return null;
    }

    public boolean removePlayerAccountByName(UUID playerId, String accountName) {
        UUID accountIDToRemove = null;

        List<UUID> currentAccounts = getPlayerAccountUUIDs(playerId);
        if (currentAccounts != null) {
            for (UUID accountId : currentAccounts) {
                String currentAccountName = getAccountName(accountId);

                if (currentAccountName != null && currentAccountName.equals(accountName)) {
                    accountIDToRemove = accountId;
                    break;
                }
            }
        }

        if (accountIDToRemove != null) {
            try {
                removePlayerAccount(playerId, accountIDToRemove);
            }
            catch (Exception e) {
                return false;
            }

            return true;
        }
        else {
            return false;
        }
    }

    public int getAccountBalance(UUID accountId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:3000/balance/%s".formatted(accountId.toString())))
                .GET()
                .build();

        HttpResponse<String> httpResponse = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        return Integer.parseInt(httpResponse.body());
    }

    public void depositCubits(UUID accountId, int amount) throws Exception {
        JsonObject postData = new JsonObject();
        postData.addProperty("account_id", accountId.toString());
        postData.addProperty("amount", amount);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:3000/accounts/deposit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(postData)))
                .build();

        CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public boolean transferCubits(UUID sourceId, UUID destinationId, int amount) throws Exception {
        JsonObject postData = new JsonObject();
        postData.addProperty("source_id", sourceId.toString());
        postData.addProperty("destination_id", destinationId.toString());
        postData.addProperty("amount", amount);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:3000/accounts/transfer"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(postData)))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 200;
    }

    public List<Transaction> getAllLatestTransactions() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:3000/history/all"))
                .GET()
                .build();

        HttpResponse<String> httpResponse = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        Type transactionListType = new TypeToken<List<Transaction>>(){}.getType();

        return GSON.fromJson(httpResponse.body(), transactionListType);
    }

    public List<Transaction> getAccountLatestTransactions(UUID accountId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:3000/history/%s".formatted(accountId)))
                .GET()
                .build();

        HttpResponse<String> httpResponse = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        Type transactionListType = new TypeToken<List<Transaction>>(){}.getType();

        return GSON.fromJson(httpResponse.body(), transactionListType);
    }
}
