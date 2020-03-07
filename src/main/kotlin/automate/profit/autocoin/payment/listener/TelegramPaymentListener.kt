package automate.profit.autocoin.payment.listener

import automate.profit.autocoin.payment.UserPayment
import mu.KLogging
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.ExecutorService

class TelegramBot(
        private val botUserName: String,
        private val botToken: String,
        /**
         * How to get chat id?
         * https://stackoverflow.com/questions/32423837/telegram-bot-how-to-get-a-group-chat-id
         */
        private val chatId: Long
) : TelegramLongPollingBot() {
    companion object : KLogging()

    override fun getBotUsername() = botUserName

    override fun getBotToken() = botToken

    override fun onUpdateReceived(update: Update) {
        logger.info { "onUpdateReceived($update)" }
    }

    fun sendMessage(message: String) {
        execute(SendMessage(chatId, message))
    }

}

class TelegramPaymentListener(private val executorService: ExecutorService, private val telegramBot: TelegramBot) : PaymentListener {
    override fun onPaymentCreated(userPayment: UserPayment) {
        executorService.submit {
            telegramBot.sendMessage("User has created payment: $userPayment")
        }
    }
}

class NoOpPaymentListener : PaymentListener {
    companion object : KLogging()

    var lastPaymentCreated: UserPayment? = null

    override fun onPaymentCreated(userPayment: UserPayment) {
        logger.info { "Would notify about created payment: $userPayment" }
        lastPaymentCreated = userPayment
    }
}