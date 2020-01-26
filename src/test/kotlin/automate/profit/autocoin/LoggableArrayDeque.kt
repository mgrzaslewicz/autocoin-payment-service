package automate.profit.autocoin

import mu.KLogging
import java.util.*

class LoggableArrayDeque<E>(initialCollection: List<E>) : ArrayDeque<E>(initialCollection) {
    companion object : KLogging()

    override fun pop(): E {
        val result = super.pop()
        logger.info { "Returning popped value: $result" }
        return result
    }
}