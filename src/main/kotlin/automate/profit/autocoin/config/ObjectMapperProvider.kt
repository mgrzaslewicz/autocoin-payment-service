package automate.profit.autocoin.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

class ObjectMapperProvider {
    fun createObjectMapper(): ObjectMapper {
        return ObjectMapper()
                .registerModule(KotlinModule())
    }
}