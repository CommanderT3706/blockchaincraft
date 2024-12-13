use chrono::Utc;
use sha2::{Digest, Sha256};
use uuid::Uuid;
use crate::blockchain::models::{Block, Transaction};

pub mod models;
pub mod db;

pub fn calculate_hash(index: u64, timestamp: i64, transactions: Vec<Transaction>, previous_hash: &str) -> String {
    let mut hasher = Sha256::new();
    let data = format!("{}{}{:?}{}", index, timestamp, transactions, previous_hash);
    hasher.update(data);
    format!("{:x}", hasher.finalize())
}

pub fn create_genesis_block() -> Block {
    let hash = calculate_hash(0, Utc::now().timestamp(), vec![], "0");
    Block {
        index: 0,
        timestamp: Utc::now().timestamp(),
        transactions: vec![],
        previous_hash: String::from("0"),
        hash
    }
}

pub fn deposit_cubits(account_id: String, amount: i32) {
    let transaction = Transaction {
        tx_id: Uuid::new_v4(),
        from_account: Uuid::nil(),
        to_account: Uuid::parse_str(account_id.as_str()).unwrap(),
        amount,
        timestamp: Utc::now().timestamp() as u64,
    };

    db::add_transaction(&transaction);
    crate::db::add_to_balance(account_id, amount);
    db::mine_block();
}