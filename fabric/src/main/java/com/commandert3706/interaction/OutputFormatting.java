package com.commandert3706.interaction;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OutputFormatting {
    public static Text formatTransaction(Transaction transaction) {

        return Text.literal("Transaction ID: %s\nFrom: %s\nTo: %s\nAmount: %s\nTimestamp: %s"
                .formatted(transaction.getTxId(), transaction.getFromAccount(), transaction.getToAccount(),
                        transaction.getAmount(), convertTimestampToHumanReadable(transaction.getTimestamp())));
    }

    public static Text getSeparator() {
        return Text.literal("=====================================================").styled(style -> style.withColor(Formatting.GOLD));
    }

    public static String convertTimestampToHumanReadable(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp); // Convert timestamp to Instant
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss") // Set your desired format
                .withZone(ZoneId.systemDefault()); // Use system's default timezone

        return formatter.format(instant); // Return formatted date and time
    }
}
