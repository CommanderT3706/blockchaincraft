package com.commandert3706.commands;

import com.commandert3706.interaction.AccountManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopCommand {
    private final AccountManager accountManager;

    public ShopCommand(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    public int createShopSign(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        int price = IntegerArgumentType.getInteger(context, "price");

        if (player == null) {
            return -1;
        }

        String paymentAccount = StringArgumentType.getString(context, "payment_account");

        if (paymentAccount == null) {
            return -1;
        }

        String playerName = player.getName().getString();

        HitResult hitResult = player.raycast(4.5, 1.0F, false);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;

            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockEntity blockEntity = player.getWorld().getBlockEntity(blockPos);

            if (blockEntity instanceof SignBlockEntity signBlockEntity) {
                signBlockEntity.setWaxed(true);

                SignText signText = new SignText();
                signText = signText.withMessage(1, Text.literal("Click to Pay!").styled(style ->
                        style.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/bcc transfer %s %s %s".formatted(price, playerName, paymentAccount)))
                ));
                signText = signText.withMessage(2, Text.literal("Price: %s Cubits".formatted(price)).styled(style ->
                        style.withColor(Formatting.GOLD)
                ));
                signText = signText.withGlowing(true);

                signBlockEntity.setText(signText, true);

                signBlockEntity.markDirty();
            }
        }

        return 1;
    }
}
