use chrono::Utc;
use rusqlite::Connection;
use uuid::Uuid;
use crate::blockchain::calculate_hash;
use crate::blockchain::models::{Block, Transaction};
use crate::request_models::TransactionsResponse;

pub fn initialize_database() {
    let conn = Connection::open("blockchain.db").unwrap();

    let first_table_command = r#"
        CREATE TABLE IF NOT EXISTS blocks (
            block_index INTEGER PRIMARY KEY,
            timestamp INTEGER NOT NULL,
            prev_hash TEXT NOT NULL,
            hash TEXT NOT NULL
        );
    "#;

    let second_table_command = r#"
        CREATE TABLE IF NOT EXISTS transactions (
            tx_id TEXT PRIMARY KEY,
            from_account TEXT NOT NULL,
            to_account TEXT NOT NULL,
            amount INTEGER NOT NULL,
            timestamp INTEGER NOT NULL,
            block_index INTEGER,
            FOREIGN KEY (block_index) REFERENCES blocks (block_index)
        );
    "#;

    conn.execute(first_table_command, ()).unwrap();
    conn.execute(second_table_command, ()).unwrap();
}

pub fn insert_block(block: &Block) {
    let conn = Connection::open("blockchain.db").unwrap();

    conn.execute(
        "INSERT INTO blocks (block_index, timestamp, prev_hash, hash) VALUES (?1, ?2, ?3, ?4)",
        (
            block.index, block.timestamp, block.previous_hash.clone(), block.hash.clone()
        )
    ).unwrap();
}

pub fn create_genesis_block_if_needed() {
    let conn = Connection::open("blockchain.db").unwrap();

    let mut stmt = conn.prepare("SELECT COUNT(*) FROM blocks").unwrap();
    let count: i64 = stmt.query_row((), |row| row.get(0)).unwrap();

    if count == 0 {
        println!("No blocks found. Creating genesis block...");
        let genesis_block = crate::blockchain::create_genesis_block();
        insert_block(&genesis_block);
    } else {
        println!("Blockchain already initialized.");
    }
}

pub fn add_transaction(transaction: &Transaction) {
    let conn = Connection::open("blockchain.db").unwrap();

    conn.execute(
        "INSERT INTO transactions (tx_id, from_account, to_account, amount, timestamp, block_index)
         VALUES (?1, ?2, ?3, ?4, ?5, NULL)",
        (
            transaction.tx_id.to_string(),
            transaction.from_account.to_string(),
            transaction.to_account.to_string(),
            transaction.amount,
            transaction.timestamp as i64,
        ),
    )
        .unwrap();
}

pub fn mine_block() {
    let conn = Connection::open("blockchain.db").unwrap();

    // Fetch unconfirmed transactions.
    let mut stmt = conn.prepare("SELECT tx_id, from_account, to_account, amount, timestamp FROM transactions WHERE block_index IS NULL").unwrap();
    let transactions = stmt
        .query_map([], |row| {
            Ok(Transaction {
                tx_id: Uuid::parse_str(&row.get::<_, String>(0)?).unwrap(),
                from_account: Uuid::parse_str(&row.get::<_, String>(1)?).unwrap(),
                to_account: Uuid::parse_str(&row.get::<_, String>(2)?).unwrap(),
                amount: row.get(3)?,
                timestamp: row.get::<_, i64>(4)? as u64,
            })
        })
        .unwrap()
        .collect::<Result<Vec<_>, _>>()
        .unwrap();

    // Fetch the last block to determine the previous hash.
    let last_block: (u64, String) = conn
        .query_row("SELECT block_index, hash FROM blocks ORDER BY block_index DESC LIMIT 1", [], |row| {
            Ok((row.get(0)?, row.get(1)?))
        })
        .unwrap_or((0, String::from("0")));

    let (last_index, last_hash) = last_block;

    // Create the new block.
    let new_block = Block {
        index: last_index + 1,
        timestamp: Utc::now().timestamp(),
        transactions: transactions.clone(),
        previous_hash: last_hash.clone(),
        hash: calculate_hash(last_index + 1, Utc::now().timestamp(), transactions.clone(), &last_hash),
    };

    // Insert the new block into the database.
    insert_block(&new_block);

    // Update transactions to reference the new block.
    for tx in &transactions {
        conn.execute(
            "UPDATE transactions SET block_index = ?1 WHERE tx_id = ?2",
            (new_block.index as i64, tx.tx_id.to_string()),
        )
            .unwrap();
    }
}

pub fn get_all_transactions() -> Vec<TransactionsResponse> {
    let conn = Connection::open("blockchain.db").unwrap();

    let mut stmt = conn.prepare("SELECT tx_id, from_account, to_account, amount, timestamp FROM transactions ORDER BY timestamp DESC LIMIT 10").unwrap();

    stmt.query_map([], |row| {
        Ok(TransactionsResponse {
            tx_id: row.get(0)?,
            from_account: row.get(1)?,
            to_account: row.get(2)?,
            amount: row.get(3)?,
            timestamp: row.get(4)?,
        })
    }).unwrap()
        .collect::<Result<Vec<_>, _>>()
        .unwrap()
}

pub fn get_all_transactions_for_account(account_id: String) -> Vec<TransactionsResponse> {
    let conn = Connection::open("blockchain.db").unwrap();

    let mut stmt = conn.prepare("SELECT tx_id, from_account, to_account, amount, timestamp FROM transactions WHERE from_account = ?1 OR to_account = ?1 ORDER BY timestamp DESC LIMIT 10").unwrap();

    stmt.query_map([account_id.clone()], |row| {
        Ok(TransactionsResponse {
            tx_id: row.get(0)?,
            from_account: row.get(1)?,
            to_account: row.get(2)?,
            amount: row.get(3)?,
            timestamp: row.get(4)?,
        })
    }).unwrap()
        .collect::<Result<Vec<_>, _>>()
        .unwrap()
}