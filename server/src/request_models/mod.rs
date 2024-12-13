use serde::{Deserialize, Serialize};
use crate::blockchain::models::ReadableAccount;

#[derive(Serialize)]
pub struct PlayerAccountsResponse {
    pub accounts: Vec<ReadableAccount>
}

#[derive(Serialize)]
pub struct TransactionsResponse {
    pub tx_id: String,
    pub from_account: String,
    pub to_account: String,
    pub amount: i32,
    pub timestamp: u64
}

#[derive(Deserialize)]
pub struct AddPlayerAccountRequest {
    pub player_id: String,
    pub account_name: String,
}

#[derive(Deserialize)]
pub struct DeletePlayerAccountRequest {
    pub account_id: String
}

#[derive(Deserialize)]
pub struct DepositIntoPlayerAccountRequest {
    pub account_id: String,
    pub amount: i32,
}

#[derive(Deserialize)]
pub struct TransferCubitsRequest {
    pub source_id: String,
    pub destination_id: String,
    pub amount: i32,
}