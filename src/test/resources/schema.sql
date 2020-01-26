create table if not exists SUBSCRIPTION
(
    subscription_code    VARCHAR(128) PRIMARY KEY NOT NULL,
    description          VARCHAR(1024)            NOT NULL,
    period_days          INTEGER                  NOT NULL,
    btc_receiver_address VARCHAR(1024),
    eth_receiver_address VARCHAR(1024),
    usd_amount           NUMERIC                  NOT NULL
);

create table if not exists USER_SUBSCRIPTION
(
    user_account_id   VARCHAR(32),
    subscription_code VARCHAR(1024),
    valid_from        TIMESTAMP NOT NULL,
    valid_to          TIMESTAMP NOT NULL
        constraint fk_subscription_code_subscription FOREIGN KEY (subscription_code) references SUBSCRIPTION (subscription_code),
    constraint uq_user_account_id_subscription_code unique (user_account_id, subscription_code)
);

CREATE TYPE PAYMENT_STATUS AS ENUM ('NEW', 'PAID', 'APPROVED_MANUALLY');

create table if not exists USER_PAYMENT
(
    user_payment_id     VARCHAR(32)    NOT NULL,
    user_account_id     VARCHAR(32)    NOT NULL,
    subscription_code   VARCHAR(128)   NOT NULL,
    btc_sender_address  VARCHAR(1024),
    eth_sender_address  VARCHAR(1024),
    btc_amount_required NUMERIC,
    eth_amount_required NUMERIC,
    amount_paid         NUMERIC,
    payment_status      PAYMENT_STATUS NOT NULL DEFAULT 'NEW',
    ins_time            TIMESTAMP      NOT NULL,
    upd_time            TIMESTAMP
);