package com.commandert3706;

import com.commandert3706.commands.BlockchainCraftCommands;
import com.commandert3706.commands.ShopCommand;
import com.commandert3706.commands.suggestions.CurrentPlayerAccountSuggestionProvider;
import com.commandert3706.interaction.AccountManager;
import com.commandert3706.commands.suggestions.PlayerAccountSuggestionProvider;
import com.commandert3706.commands.suggestions.PlayerSuggestionProvider;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockchainCraft implements ModInitializer {
	public static final String MOD_ID = "blockchaincraft";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final AccountManager accountManager = new AccountManager();
	private final BlockchainCraftCommands commands = new BlockchainCraftCommands(accountManager);
	private final ShopCommand shopCommand = new ShopCommand(accountManager);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ServerPlayConnectionEvents.JOIN.register(accountManager::fetchAccountsOnPlayerLogin);

		CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
			var command = commandDispatcher.register(CommandManager.literal("blockchaincraft")
					.then(CommandManager.literal("account")
							.then(CommandManager.literal("create")
									.requires(source -> source.hasPermissionLevel(3))
									.then(CommandManager.argument("player", StringArgumentType.string()).suggests(new PlayerSuggestionProvider())
										.then(CommandManager.argument("account_name", StringArgumentType.string())
												.executes(commands::createPlayerAccount)))
							)

							.then(CommandManager.literal("delete")
									.requires(source -> source.hasPermissionLevel(3))
									.then(CommandManager.argument("player", StringArgumentType.string())
											.suggests(new PlayerSuggestionProvider())
											.then(CommandManager.argument("account_name", StringArgumentType.string())
													.suggests(new PlayerAccountSuggestionProvider(accountManager))
													.executes(commands::deletePlayerAccount)
											)
									)
							)

							.then(CommandManager.literal("list")
									.executes(commands::listDefaultPlayerAccounts)
									.then(CommandManager.argument("player", StringArgumentType.string())
											.suggests(new PlayerSuggestionProvider())
											.executes(commands::listPlayerAccounts)
									)
							).requires(source -> source.hasPermissionLevel(0))
					)
					.then(CommandManager.literal("balance")
							.executes(commands::defaultAccountBalance)
							.then(CommandManager.argument("account_name", StringArgumentType.string())
									.suggests(new CurrentPlayerAccountSuggestionProvider(accountManager))
									.requires(source -> source.hasPermissionLevel(0))
									.executes(commands::accountBalance)
							)
					)
					.then(CommandManager.literal("transfer")
							.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
									.then(CommandManager.argument("player", StringArgumentType.string()).suggests(new PlayerSuggestionProvider())
											.then(CommandManager.argument("target_account_name", StringArgumentType.string())
													.suggests(new PlayerAccountSuggestionProvider(accountManager))
													.requires(source -> source.hasPermissionLevel(0))
													.executes(commands::transferFromDefaultAccount)

													.then(CommandManager.argument("source_account_name", StringArgumentType.string())
															.suggests(new CurrentPlayerAccountSuggestionProvider(accountManager))
															.requires(source -> source.hasPermissionLevel(0))
															.executes(commands::transferFromAccount)
													)
											)
									)
							)
					)
					.then(CommandManager.literal("deposit")
							.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
									.requires(source -> source.hasPermissionLevel(0))
									.executes(commands::depositIntoDefaultAccount)
									.then(CommandManager.argument("account_name", StringArgumentType.string())
											.suggests(new CurrentPlayerAccountSuggestionProvider(accountManager))
											.requires(source -> source.hasPermissionLevel(0))
											.executes(commands::depositIntoAccount)
									)
							)
					)
					.then(CommandManager.literal("history")
							.then(CommandManager.literal("all")
									.requires(source -> source.hasPermissionLevel(0))
									.executes(commands::getLatestTransactions)
							)
							.then(CommandManager.literal("account")
									.then(CommandManager.argument("player", StringArgumentType.string()).suggests(new PlayerSuggestionProvider())
											.then(CommandManager.argument("account_name", StringArgumentType.string())
													.suggests(new PlayerAccountSuggestionProvider(accountManager))
													.requires(source -> source.hasPermissionLevel(0))
													.executes(commands::getLatestAccountTransactions)
											)
									)
							)
					)
			);

			commandDispatcher.register(CommandManager.literal("shop")
					.then(CommandManager.argument("price", IntegerArgumentType.integer(1))
							.then(CommandManager.argument("payment_account", StringArgumentType.string())
									.suggests(new CurrentPlayerAccountSuggestionProvider(accountManager))
									.requires(source -> source.hasPermissionLevel(0))
									.executes(shopCommand::createShopSign)
							)
					)
			);

			commandDispatcher.register(CommandManager.literal("bcc").redirect(command));
		}));
	}
}