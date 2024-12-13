use chrono::Utc;
use serde::Serialize;
use sha2::{Digest, Sha256};
use uuid::Uuid;

pub struct Block {
    pub index: u64,
    pub timestamp: i64,
    pub transactions: Vec<Transaction>,
    pub previous_hash: String,
    pub hash: String,
}

#[derive(Debug, Copy, Clone)]
pub struct Transaction {
    pub tx_id: Uuid,
    pub from_account: Uuid,
    pub to_account: Uuid,
    pub amount: i32,
    pub timestamp: u64,
}

#[derive(Serialize)]
pub struct ReadableAccount {
    pub account_id: String,
    pub account_name: String
}