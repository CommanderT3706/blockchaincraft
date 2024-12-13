package com.commandert3706.interaction;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReadableAccountResponse {
    @SerializedName("accounts")
    private List<ReadableAccount> readableAccounts;

    public List<ReadableAccount> getReadableAccounts() {
        return readableAccounts;
    }

    public void setReadableAccounts(List<ReadableAccount> readableAccounts) {
        this.readableAccounts = readableAccounts;
    }

    public static class ReadableAccount {
        @SerializedName("account_id")
        private String accountId;

        @SerializedName("account_name")
        private String accountName;

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }
    }
}
