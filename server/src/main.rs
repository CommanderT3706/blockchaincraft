use axum::extract::Path;
use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::{Json, Router};
use axum::routing::{get, post};
use chrono::Utc;
use uuid::Uuid;
use crate::blockchain::models::Transaction;
use crate::request_models::{AddPlayerAccountRequest, DeletePlayerAccountRequest, DepositIntoPlayerAccountRequest, PlayerAccountsResponse, TransferCubitsRequest};

mod request_models;
mod blockchain;
mod db;

#[tokio::main]
async fn main() {
    println!("Initializing accounts...");
    db::initialise_database();
    println!("Accounts initialized");

    println!("Initializing blockchain...");
    blockchain::db::initialize_database();
    blockchain::db::create_genesis_block_if_needed();
    println!("Blockchain initialized");

    println!("Starting server...");

    let app = Router::new()
        .route("/accounts/create", post(create_account_for_player))
        .route("/accounts/delete", post(delete_player_account))
        .route("/accounts/deposit", post(deposit_into_player_account))
        .route("/accounts/transfer", post(transfer_cubits))
        .route("/accounts/:player_id", get(get_accounts_for_player))
        .route("/history/all", get(get_all_transaction_history))
        .route("/history/:account_id", get(get_transaction_history_for_account))
        .route("/balance/:account_id", get(get_account_balance));

    let listener = tokio::net::TcpListener::bind("127.0.0.1:3000").await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn create_account_for_player(Json(payload): Json<AddPlayerAccountRequest>) -> impl IntoResponse {
    db::add_account(Uuid::new_v4().to_string(), payload.player_id, payload.account_name);

    StatusCode::CREATED
}

async fn delete_player_account(Json(payload): Json<DeletePlayerAccountRequest>) -> impl IntoResponse {
    db::delete_account(payload.account_id.clone());

    println!("Deleted account '{}'", payload.account_id);

    StatusCode::OK
}

async fn deposit_into_player_account(Json(payload): Json<DepositIntoPlayerAccountRequest>) -> impl IntoResponse {
    blockchain::deposit_cubits(payload.account_id.clone(), payload.amount);

    println!("Deposited {} into account '{}'", payload.amount, payload.account_id);
}

async fn transfer_cubits(Json(payload): Json<TransferCubitsRequest>) -> impl IntoResponse {
    let balance = db::get_balance(payload.source_id.clone());

    if balance < payload.amount {
        return StatusCode::EXPECTATION_FAILED;
    }

    let transaction = Transaction {
        tx_id: Uuid::new_v4(),
        from_account: Uuid::parse_str(payload.source_id.as_str()).unwrap(),
        to_account: Uuid::parse_str(payload.destination_id.as_str()).unwrap(),
        amount: payload.amount,
        timestamp: Utc::now().timestamp() as u64
    };

    blockchain::db::add_transaction(&transaction);
    db::remove_from_balance(payload.source_id.clone(), payload.amount);
    db::add_to_balance(payload.destination_id.clone(), payload.amount);
    blockchain::db::mine_block();

    StatusCode::OK
}

async fn get_accounts_for_player(Path(player_id): Path<String>) -> impl IntoResponse {
    let accounts = db::get_accounts(player_id);
    let response = PlayerAccountsResponse { accounts };

    (StatusCode::OK, Json(response))
}

async fn get_all_transaction_history() -> impl IntoResponse {
    (StatusCode::OK, Json(blockchain::db::get_all_transactions()))
}

async fn get_transaction_history_for_account(Path(account_id): Path<String>) -> impl IntoResponse {
    (StatusCode::OK, Json(blockchain::db::get_all_transactions_for_account(account_id)))
}

async fn get_account_balance(Path(account_id): Path<String>) -> impl IntoResponse {
    let balance = db::get_balance(account_id);

    (StatusCode::OK, balance.to_string())
}
