use axum::extract::Path;
use axum::Json;
use rusqlite::Connection;
use uuid::Uuid;
use crate::blockchain::models::ReadableAccount;

pub fn initialise_database() {
    let conn = Connection::open("accounts.db").unwrap();

    conn.execute(r#"
    CREATE TABLE IF NOT EXISTS accounts (
        account_id TEXT PRIMARY KEY,
        player_id TEXT NOT NULL,
        account_name TEXT NOT NULL,
        balance INTEGER NOT NULL DEFAULT 0
    );"#, ()).unwrap();
}

pub fn add_account(account_id: String, player_id: String, account_name: String) {
    let conn = Connection::open("accounts.db").unwrap();

    conn.execute("INSERT INTO accounts (account_id, player_id, account_name) VALUES (?1, ?2, ?3)",
                 (account_id, player_id, account_name.clone())).unwrap();

    println!("Account {} created", account_name);
}

pub fn delete_account(account_id: String) {
    let conn = Connection::open("accounts.db").unwrap();

    conn.execute("DELETE FROM accounts WHERE account_id = ?1", [account_id.clone()]).unwrap();

    println!("Account {} deleted", account_id);
}

pub fn get_accounts(player_id: String) -> Vec<ReadableAccount> {
    let conn = Connection::open("accounts.db").unwrap();
    let mut stmt = conn.prepare("SELECT account_id, account_name FROM accounts WHERE player_id = ?").unwrap();

    let accounts_iter = stmt
        .query_map([player_id.clone()], |row| {
            Ok(ReadableAccount {
                account_id: row.get(0).unwrap(),
                account_name: row.get(1).unwrap(),
            })
        }).unwrap();

    let mut accounts: Vec<ReadableAccount> = accounts_iter.filter_map(Result::ok).collect();

    if accounts.is_empty() {
        println!("No accounts found for {player_id}, creating main");

        let account_id = Uuid::new_v4();

        add_account(account_id.to_string(), player_id, "main".to_string());

        let new_account = ReadableAccount {
            account_id: account_id.to_string(),
            account_name: "main".to_string(),
        };

        accounts.push(new_account);
    }

    accounts
}

pub fn add_to_balance(account_id: String, amount: i32) {
    let conn = Connection::open("accounts.db").unwrap();

    conn.execute("UPDATE accounts SET balance = balance + ?1 WHERE account_id = ?2", (amount, account_id)).unwrap();
}

pub fn remove_from_balance(account_id: String, amount: i32) {
    let conn = Connection::open("accounts.db").unwrap();

    conn.execute("UPDATE accounts SET balance = balance - ?1 WHERE account_id = ?2", (amount, account_id)).unwrap();
}

pub fn get_balance(account_id: String) -> i32 {
    let conn = Connection::open("accounts.db").unwrap();

    let mut stmt = conn.prepare("SELECT balance FROM accounts WHERE account_id = ?").unwrap();

    stmt.query_row([account_id], |row| row.get(0)).unwrap()
}