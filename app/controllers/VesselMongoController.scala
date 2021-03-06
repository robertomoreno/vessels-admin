package controllers

import models.Vessel
import play.api._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import reactivemongo.core.commands.LastError
import scala.concurrent.Future
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection


/**
 * Created by roberto on 13/03/2015.
 * MongoDB controller.
 */
class VesselMongoController extends Controller with VesselAbstractController with MongoController {
  import models.JsonFormats._

  def collection: JSONCollection = db.collection[JSONCollection]("vessels")

  // Get all vessels
  def vessels = getVessel(Json.obj())

  // Get vessels witch id match.
  def vessel(id: String) = getVessel(Json.obj("id" -> id))

  // Delete vessel
  def deleteVessel(id: String) = Action.async(parse.json) {request =>
    collection.remove(Json.obj("id" -> id)).map { lastError  => validateResult("Remove",lastError) }
  }

  // Edit vessel
  def editVessel(id:String) = Action.async(parse.json) {request =>
    /* Validate the JSon received in order to it can be parsed to a Vessel object. If validations fail then a
    BadRequest will be responsed */
    request.body.validate[Vessel].map { vessel =>
      //Execute update
      collection.update(Json.obj("id" -> id),vessel).map { lastError => validateResult("Update",lastError)}
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def newVessel = Action.async(parse.json) {request =>
    request.body.validate[Vessel].map { result =>
      collection.insert(result).map { lastError => validateResult("Create",lastError) }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  //Utility functions

  def validateResult (operation : String, lastError : LastError) : Result = {
    if (lastError.inError){
      Logger.error(s"Error updating: $lastError")
      InternalServerError(lastError.getMessage())
    } else
      Created("$operation Vessel")
  }

  def getVessel(objectToFind : JsObject)=  Action.async {
    val query = collection.find(objectToFind).cursor[JsObject]
    query.collect[List]().map (persons => Json.arr(persons)).map(Ok(_))
  }
}
