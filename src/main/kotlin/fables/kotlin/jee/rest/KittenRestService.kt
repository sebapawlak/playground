package fables.kotlin.jee.rest

import fables.kotlin.jee.business.KittenBusinessService
import fables.kotlin.jee.business.KittenEntity
import javax.inject.Inject
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonArrayBuilder
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

/**
 * JSON REST CRud service.
 * Class and all public methods are opened by Kotlin compiler plugin.
 * JEE will first create one noarg constructor instance, and then injected constructor instances.
 * @author Zeljko Trogrlic
 */
@Path("kitten")
class KittenRestService @Inject constructor(private val kittenBusinessService: KittenBusinessService) {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun find(
            @PathParam("id") id: Int
    ): KittenRest = kittenBusinessService[id]
            ?.let { KittenRest(it.name, it.cuteness) }
            ?: throw NotFoundException("ID $id not found") // TIP: Do not forget to throw, otherwise 200 OK

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun add(
            kittenRest: KittenRest
    ): Int = kittenRest
            .let { KittenEntity(it.name, it.cuteness) }
            .also { kittenBusinessService += it }
            .let { it.id!! }

    @GET
    fun getAll(): JsonArray =
        kittenBusinessService
            .getAll()
            .map { KittenRest(it.name, it.cuteness) }
            .map { it.toJson() }
            .fold(Json.createArrayBuilder(), {jsonArray, jsonObject -> jsonArray.add(jsonObject)})
            .build()
}
