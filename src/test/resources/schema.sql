create table if not exists SUBSCRIPTION
(
    subscription_code    VARCHAR(128) PRIMARY KEY NOT NULL,
    description          VARCHAR(1024)            NOT NULL,
    period_days          INTEGER                  NOT NULL,
    btc_receiver_address VARCHAR(1024),
    eth_receiver_address VARCHAR(1024),
    usd_amount           NUMERIC                  NOT NULL
);

CREATE TYPE PAYMENT_STATUS AS ENUM ('NEW', 'PAID', 'APPROVED_MANUALLY');

create table if not exists USER_PAYMENT
(
    user_payment_id      VARCHAR(36)    NOT NULL PRIMARY KEY,
    user_account_id      VARCHAR(36)    NOT NULL,
    subscription_code    VARCHAR(128)   NOT NULL,
    btc_sender_address   VARCHAR(1024),
    eth_sender_address   VARCHAR(1024),
    btc_receiver_address VARCHAR(1024),
    eth_receiver_address VARCHAR(1024),
    btc_amount_required  NUMERIC,
    eth_amount_required  NUMERIC,
    amount_paid          NUMERIC,
    payment_status       PAYMENT_STATUS NOT NULL DEFAULT 'NEW',
    insert_time          TIMESTAMP      NOT NULL,
    update_time          TIMESTAMP               DEFAULT NULL,
    constraint fk_user_payment_subscription_code FOREIGN KEY (subscription_code) references SUBSCRIPTION (subscription_code)
);

create table if not exists USER_SUBSCRIPTION
(
    user_account_id      VARCHAR(36)   NOT NULL,
    last_user_payment_id VARCHAR(36)   NOT NULL,
    subscription_code    VARCHAR(1024) NOT NULL,
    valid_from           TIMESTAMP     NOT NULL,
    valid_to             TIMESTAMP     NOT NULL,
    insert_time          TIMESTAMP     NOT NULL,
    update_time          TIMESTAMP DEFAULT NULL,
    constraint fk_user_subscription_subscription_code FOREIGN KEY (subscription_code) references SUBSCRIPTION (subscription_code),
    constraint fk_user_subscription_user_payment_id FOREIGN KEY (last_user_payment_id) references USER_PAYMENT (user_payment_id),
    constraint uq_user_subscription_user_account_id_subscription_code unique (user_account_id, subscription_code)
);

