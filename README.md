# autocoin-payment-service

Simple implementation of handling BTC payments and ETH, enough for a few thousands of recurring payments.
Uses no shared DB, just json files stored in given location so it's suitable for running only one instance with no data redundation and failover.

Service responsibility:
- Respond with subscription details describing how to pay with BTC and ETH
- Checks if user has subscription active
- Schedule checking if payment has corresponding transaction in blockchain to mark subscription as active.
This ^ is not implemented in first iteration to focus on actually getting first customers and automating checking transactions later

To run locally (on developer machine) - use RunLocal.kt with default properties provided (but no oauth client id and password)