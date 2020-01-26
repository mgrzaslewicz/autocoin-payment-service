# autocoin-payment-service

Simple implementation of handling BTC payments and ETH, enough for a few thousands of recurring payments.
Requires postgreSQL DB parameters provided (@see RunLocal.java).

Service responsibility:
- Responds with subscription details describing how to pay with BTC and ETH
- Checks if user has subscription active
- Schedule checking if unpaid payment has corresponding transaction in blockchain to mark subscription as active and payment as paid.
This ^ is not implemented in first iteration to focus on actually getting first customers and automating checking actual transactions automatically later

To run locally (on developer machine) - use RunLocal.kt with default properties provided (but no oauth client id and password)