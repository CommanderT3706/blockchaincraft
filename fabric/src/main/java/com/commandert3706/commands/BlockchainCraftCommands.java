package com.commandert3706.commands;

import com.commandert3706.BlockchainCraft;
import com.commandert3706.interaction.AccountManager;
import com.commandert3706.interaction.OutputFormatting;
import com.commandert3706.interaction.Transaction;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

public class BlockchainCraftCommands {
    private final AccountManager accountManager;

    public BlockchainCraftCommands(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    public int createPlayerAccount(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");
        String accountName = StringArgumentType.getString(context, "account_name");
        ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);

        if (player == null) {
            return -1;
        }

        try {
            accountManager.addPlayerAccount(player.getUuid(), accountName);
        }
        catch (Exception e) {
            return -1;
        }

        context.getSource().sendFeedback(() -> Text.literal("Account %s created for player %s".formatted(accountName, playerName))
                .styled(style -> style.withColor(Formatting.GREEN)), false);
        return 1;
    }

    public int deletePlayerAccount(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");
        String accountName = StringArgumentType.getString(context, "account_name");
        ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);

        if (player == null) {
            System.out.println("Player is null");

            return -1;
        }

        boolean deletedAccount = accountManager.removePlayerAccountByName(player.getUuid(), accountName);

        if (deletedAccount) {
            context.getSource().sendFeedback(() -> Text.literal("Account %s deleted for player %s".formatted(accountName, playerName))
                    .styled(style -> style.withColor(Formatting.GREEN)), false);
            return 1;
        }
        else {
            context.getSource().sendFeedback(() -> Text.literal("Specified account %s deleted for player %s could not be found".formatted(accountName, playerName))
                    .styled(style -> style.withColor(Formatting.RED)), false);
            return 0;
        }
    }

    public int listDefaultPlayerAccounts(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (player == null) {
            return -1;
        }

        String playerName = player.getName().getString();
        StringBuilder outputText = new StringBuilder();
        outputText.append("Accounts for %s:\n".formatted(playerName));

        List<String> accounts = accountManager.getPlayerAccountNames(player.getUuid());

        if (!accounts.isEmpty()) {
            for (String account : accounts) {
                outputText.append(account).append("\n");
            }

            context.getSource().sendFeedback(() -> Text.literal(outputText.toString()), false);
            return 1;
        }
        else {
            context.getSource().sendFeedback(() -> Text.literal("No accounts for player %s".formatted(playerName))
                    .styled(style -> style.withColor(Formatting.RED)), false);
        }

        return -1;
    }


    public int listPlayerAccounts(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
        StringBuilder outputText = new StringBuilder();
        outputText.append("Accounts for %s:\n".formatted(playerName));

        if (player == null) {
            return -1;
        }

        List<String> accounts = accountManager.getPlayerAccountNames(player.getUuid());

        if (!accounts.isEmpty()) {
            for (String account : accounts) {
                outputText.append(account).append("\n");
            }

            context.getSource().sendFeedback(() -> Text.literal(outputText.toString()), false);
            return 1;
        }
        else {
            context.getSource().sendFeedback(() -> Text.literal("No accounts for player %s".formatted(playerName))
                    .styled(style -> style.withColor(Formatting.RED)), false);
        }

        return -1;
    }

    public int defaultAccountBalance(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String accountName = "main";

        if (player == null) {
            return -1;
        }

        UUID playerId = player.getUuid();
        UUID accountId = accountManager.getPlayerAccountByName(playerId, accountName);

        if (accountId == null) {
            return -1;
        }

        try {
            int balance = accountManager.getAccountBalance(accountId);

            context.getSource().sendFeedback(() -> Text.literal("Balance for account %s: %s".formatted(accountName, balance))
                    .styled(style -> style.withColor(Formatting.GOLD)), false);
        }
        catch (Exception e) {
            return -1;
        }

        return 1;
    }

    public int accountBalance(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String accountName = StringArgumentType.getString(context, "account_name");

        if (player == null || accountName == null) {
            return -1;
        }

        UUID playerId = player.getUuid();
        UUID accountId = accountManager.getPlayerAccountByName(playerId, accountName);

        if (accountId == null) {
            return -1;
        }

        try {
            int balance = accountManager.getAccountBalance(accountId);

            context.getSource().sendFeedback(() -> Text.literal("Balance for account %s: %s".formatted(accountName, balance))
                    .styled(style -> style.withColor(Formatting.GOLD)), false);
        }
        catch (Exception e) {
            return -1;
        }

        return 1;
    }

    public int depositIntoDefaultAccount(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (player == null || amount <= 0) {
            return -1;
        }

        Inventory inventory = player.getInventory();
        int diamondCount = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(Items.DIAMOND)) {
                diamondCount += stack.getCount();
            }
        }

        if (diamondCount < amount) {
            context.getSource().sendFeedback(() -> Text.literal("You don't have enough diamonds").styled(style -> style.withColor(Formatting.RED)), false);
            return -1;
        }

        UUID playerId = player.getUuid();
        UUID accountId = accountManager.getPlayerAccountByName(playerId, "main");

        try {
            int diamondsToRemove = amount;

            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (stack.isOf(Items.DIAMOND) && diamondsToRemove > 0) {
                    int amountToRemove = Math.min(diamondsToRemove, stack.getCount());
                    stack.decrement(amountToRemove); // Decrease item count
                    diamondsToRemove -= amountToRemove; // Decrease remaining items to remove
                }
                if (diamondsToRemove == 0) break;
            }

            accountManager.depositCubits(accountId, amount);

            context.getSource().sendFeedback(() -> Text.literal("Successfully deposited %s cubits to main".formatted(amount)).styled(style ->
                    style.withColor(Formatting.GREEN)
            ), false);
        }
        catch (Exception e) {
            return -1;
        }

        return 1;
    }

    public int depositIntoAccount(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String accountName = StringArgumentType.getString(context, "account_name");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (player == null || amount <= 0 || accountName == null) {
            return -1;
        }

        UUID playerId = player.getUuid();
        UUID accountId = accountManager.getPlayerAccountByName(playerId, accountName);

        Inventory inventory = player.getInventory();
        int diamondCount = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(Items.DIAMOND)) {
                diamondCount += stack.getCount();
            }
        }

        if (diamondCount < amount) {
            context.getSource().sendFeedback(() -> Text.literal("You don't have enough diamonds").styled(style -> style.withColor(Formatting.RED)), false);
            return -1;
        }

        try {
            int diamondsToRemove = amount;

            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (stack.isOf(Items.DIAMOND) && diamondsToRemove > 0) {
                    int amountToRemove = Math.min(diamondsToRemove, stack.getCount());
                    stack.decrement(amountToRemove); // Decrease item count
                    diamondsToRemove -= amountToRemove; // Decrease remaining items to remove
                }
                if (diamondsToRemove == 0) break;
            }

            accountManager.depositCubits(accountId, amount);

            context.getSource().sendFeedback(() -> Text.literal("Successfully deposited %s cubits to %s".formatted(amount, accountName)).styled(style ->
                    style.withColor(Formatting.GREEN)
            ), false);
        }
        catch (Exception e) {
            return -1;
        }

        return 1;
    }

    public int transferFromDefaultAccount(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String targetAccountName = StringArgumentType.getString(context, "target_account_name");
        String sourceAccountName = "main";
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (player == null || targetAccountName == null || amount <= 0) {
            return -1;
        }

        UUID playedId = player.getUuid();
        UUID sourceAccountId = accountManager.getPlayerAccountByName(playedId, sourceAccountName);
        UUID targetAccountId = accountManager.getPlayerAccountByName(playedId, targetAccountName);

        try {
            boolean success = accountManager.transferCubits(sourceAccountId, targetAccountId, amount);

            if (success) {
                player.sendMessage(Text.literal("Successfully transferred %s cubits from %s"
                        .formatted(amount, sourceAccountName)).styled(style -> style.withColor(Formatting.GREEN)));
            } else {
                player.sendMessage(Text.literal("You don't have enough cubits").styled(style -> style.withColor(Formatting.RED)));
            }
        }
        catch (Exception e) {
            return -1;
        }

        return 1;
    }

    public int transferFromAccount(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String targetAccountName = StringArgumentType.getString(context, "target_account_name");
        String sourceAccountName = StringArgumentType.getString(context, "source_account_name");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (player == null || targetAccountName == null || amount <= 0) {
            return -1;
        }

        UUID playedId = player.getUuid();
        UUID sourceAccountId = accountManager.getPlayerAccountByName(playedId, sourceAccountName);
        UUID targetAccountId = accountManager.getPlayerAccountByName(playedId, targetAccountName);

        try {
            boolean success = accountManager.transferCubits(sourceAccountId, targetAccountId, amount);

            if (success) {
                player.sendMessage(Text.literal("Successfully transferred %s cubits from %s"
                        .formatted(amount, sourceAccountName)).styled(style -> style.withColor(Formatting.GREEN)));
            } else {
                player.sendMessage(Text.literal("You don't have enough cubits").styled(style -> style.withColor(Formatting.RED)));
            }
        }
        catch (Exception e) {
            return -1;
        }

        return 1;
    }

    public int getLatestTransactions(CommandContext<ServerCommandSource> context) {
        try {
            List<Transaction> transactions = accountManager.getAllLatestTransactions();
            context.getSource().sendFeedback(OutputFormatting::getSeparator, false);

            for (Transaction transaction : transactions) {
                context.getSource().sendFeedback(() -> OutputFormatting.formatTransaction(transaction), false);
                context.getSource().sendFeedback(OutputFormatting::getSeparator, false);
            }
        }
        catch (Exception e) {
            return -1;
        }

        return 1;
    }

    public int getLatestAccountTransactions(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String accountName = StringArgumentType.getString(context, "account_name");

        if (player == null || accountName == null) {
            return -1;
        }

        UUID playerId = player.getUuid();
        UUID accountId = accountManager.getPlayerAccountByName(playerId, accountName);

        if (accountId == null) {
            return -1;
        }

        try {
            List<Transaction> transactions = accountManager.getAccountLatestTransactions(accountId);
            context.getSource().sendFeedback(OutputFormatting::getSeparator, false);

            for (Transaction transaction : transactions) {
                context.getSource().sendFeedback(() -> OutputFormatting.formatTransaction(transaction), false);
                context.getSource().sendFeedback(OutputFormatting::getSeparator, false);
            }
        }
        catch (Exception e) {
            return -1;
        }

        return 1;
    }
}
