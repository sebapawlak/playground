package fables.kotlin.jee.rest

import fables.kotlin.jee.Kitten
import javax.json.Json
import javax.json.JsonObject

/**
 * Constructor parameter names mapped to JSON by jackson-module-kotlin.
 * @author Zeljko Trogrlic
 */
data class KittenRest(
        override val name: String,
        override val cuteness: Int
) : Kitten {
    fun toJson(): JsonObject = Json.createObjectBuilder()
            .add("name", name)
            .add("cuteness", cuteness)
            .build()
}