package automate.profit.autocoin.payment

import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

interface SubscriptionRepository {
    @SqlQuery("SELECT * FROM SUBSCRIPTION WHERE subscription_code = ?")
    @RegisterKotlinMapper(Subscription::class)
    fun getSubscriptionByCode(subscriptionCode: String): Subscription?

    @SqlQuery("SELECT * FROM SUBSCRIPTION WHERE 1 = 1")
    @RegisterKotlinMapper(Subscription::class)
    fun getAllSubscriptions(): List<Subscription>

    @SqlQuery("SELECT * FROM USER_SUBSCRIPTION WHERE subscription_code = ? AND user_account_id = ?")
    @RegisterKotlinMapper(UserSubscription::class)
    fun getUserSubscriptionByCode(subscriptionCode: String, userAccountId: String): UserSubscription?

    @SqlUpdate("""
        INSERT INTO user_subscription(
            last_user_payment_id, 
            user_account_id, 
            subscription_code, 
            valid_from, 
            valid_to, 
            insert_time, 
            update_time
        ) 
        VALUES (
            :us.lastUserPaymentId, 
            :us.userAccountId, 
            :us.subscriptionCode, 
            :us.validFrom, 
            :us.validTo, 
            :us.insertTime, 
            :us.updateTime
        )""")
    @RegisterKotlinMapper(UserSubscription::class)
    fun insert(@BindBean("us") userSubscription: UserSubscription)

    @SqlUpdate("""
        INSERT INTO subscription(
            subscription_code, 
            btc_receiver_address, 
            eth_receiver_address, 
            usd_amount, 
            description, 
            period_days
        ) 
        VALUES (
            :s.subscriptionCode, 
            :s.btcReceiverAddress, 
            :s.ethReceiverAddress, 
            :s.usdAmount, 
            :s.description, 
            :s.periodDays
        )""")
    fun insert(@BindBean("s") subscription: Subscription)

    @SqlUpdate("UPDATE user_subscription SET valid_from = :us.validFrom, valid_to = :us.validTo, update_time = :us.updateTime WHERE  user_account_id = :us.userAccountId AND subscription_code = :us.subscriptionCode")
    @RegisterKotlinMapper(UserSubscription::class)
    fun update(@BindBean("us") userSubscription: UserSubscription)

    @SqlQuery("SELECT * FROM user_payment WHERE subscription_code = ? AND user_account_id = ? AND payment_status = 'NEW' ORDER BY insert_time DESC LIMIT 1")
    @RegisterKotlinMapper(UserPayment::class)
    fun getLatestUnpaidUserPayment(subscriptionCode: String, userAccountId: String): UserPayment?

    @SqlUpdate("""
        UPDATE user_payment SET 
        btc_sender_address = :up.btcSenderAddress, 
        eth_sender_address = :up.ethSenderAddress,  
        btc_receiver_address = :s.btcReceiverAddress, 
        eth_receiver_address = :s.ethReceiverAddress, 
        btc_amount_required = :s.btcAmountRequired,
        eth_amount_required = :s.ethAmountRequired,
        amount_paid = :up.amountPaid,
        payment_status = :up.paymentStatus::PAYMENT_STATUS,
        update_time = :up.updateTime 
        WHERE user_payment_id = :up.userPaymentId
        """)
    fun update(@BindBean("up") userPayment: UserPayment, @BindBean("s") subscription: Subscription)

    @SqlUpdate("""
        UPDATE user_payment SET 
        btc_sender_address = :up.btcSenderAddress, 
        eth_sender_address = :up.ethSenderAddress,  
        amount_paid = :up.amountPaid,
        payment_status = :up.paymentStatus::PAYMENT_STATUS,
        update_time = :up.updateTime 
        WHERE user_payment_id = :up.userPaymentId
        """)
    fun update(@BindBean("up") userPayment: UserPayment)

    @SqlUpdate("""
        INSERT INTO user_payment
         (
            user_payment_id     ,
            user_account_id     ,
            subscription_code   ,
            btc_sender_address  ,
            eth_sender_address  ,
            btc_receiver_address,
            eth_receiver_address,
            btc_amount_required ,
            eth_amount_required ,
            amount_paid         ,
            payment_status      ,
            insert_time         
         )
         VALUES(
            :up.userPaymentId,
            :up.userAccountId,
            :up.subscriptionCode,
            :up.btcSenderAddress,
            :up.ethSenderAddress,
            :up.btcReceiverAddress,
            :up.ethReceiverAddress,
            :up.btcAmountRequired,
            :up.ethAmountRequired,
            :up.amountPaid,
            :up.paymentStatus::PAYMENT_STATUS,
            :up.insertTime
         )
        """)
    fun create(@BindBean("up") userPayment: UserPayment)

    @SqlQuery("SELECT * FROM user_payment WHERE user_payment_id = ?")
    @RegisterKotlinMapper(UserPayment::class)
    fun getUserPaymentByUserPaymentId(userPaymentId: String): UserPayment?
}