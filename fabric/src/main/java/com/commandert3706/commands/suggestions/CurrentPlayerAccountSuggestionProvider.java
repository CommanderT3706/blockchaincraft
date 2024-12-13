package com.commandert3706.commands.suggestions;

import com.commandert3706.interaction.AccountManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CurrentPlayerAccountSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    private final AccountManager accountManager;

    public CurrentPlayerAccountSuggestionProvider(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> commandContext, SuggestionsBuilder suggestionsBuilder) {
        ServerPlayerEntity playerEntity = commandContext.getSource().getPlayer();

        if (playerEntity != null) {
            UUID playerId = playerEntity.getUuid();
            List<String> playerAccounts = accountManager.getPlayerAccountNames(playerId);

            for (String playerAccount : playerAccounts) {
                suggestionsBuilder.suggest(playerAccount);
            }
        }

        return suggestionsBuilder.buildFuture();
    }
}
