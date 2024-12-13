package com.commandert3706.interaction;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class Transaction {
    @SerializedName("tx_id")
    private UUID txId;
    @SerializedName("from_account")
    private UUID fromAccount;
    @SerializedName("to_account")
    private UUID toAccount;
    private int amount;
    private long timestamp;

    // Getters and setters
    public UUID getTxId() {
        return txId;
    }

    public void setTxId(UUID txId) {
        this.txId = txId;
    }

    public UUID getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(UUID fromAccount) {
        this.fromAccount = fromAccount;
    }

    public UUID getToAccount() {
        return toAccount;
    }

    public void setToAccount(UUID toAccount) {
        this.toAccount = toAccount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "txId=" + txId +
                ", fromAccount=" + fromAccount +
                ", toAccount=" + toAccount +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
