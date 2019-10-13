package controllers

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import javax.inject.Inject
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.akkastream.{State, cursorProducer}
import reactivemongo.api.Cursor
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

case class Medal(
  year: String,
  sport: String,
  athlete: String,
  team: String,
  medal: String
)

object Medal {
  implicit val format: OFormat[Medal] = {
    val fields =
      (__ \ "Year").format[String] ~
        (__ \ "Sport").format[String] ~
        (__ \ "Athlete").format[String] ~
        (__ \ "Team").format[String] ~
        (__ \ "Medal").format[String]

    fields(Medal.apply, unlift(Medal.unapply))
  }
}

class MedalsController @Inject() (
  components: ControllerComponents,
  reactiveMongoApi: ReactiveMongoApi
)(implicit ec: ExecutionContext, mat: Materializer) extends AbstractController(components) {
  def search(
    order: Option[String],
    start: Option[Int],
    count: Option[Int],
    year: Option[Int],
    medal: Option[String],
  ): Action[AnyContent] =
    Action.async { request =>
      reactiveMongoApi.database.flatMap { db =>
        val query: JsObject =
          year.fold(JsObject.empty)(y => Json.obj("Year" -> y)) ++
          medal.fold(JsObject.empty)(m => Json.obj("Medal" -> m))

        val filtered =
          db.collection[JSONCollection]("medals")
            .find(query, projection = Option.empty[JsObject])

        val sorted =
          order match {
            case Some("Year")  => filtered.sort(Json.obj("Year" -> 1))
            case Some("Medal") => filtered.sort(Json.obj("Medal" -> 1))
            case _             => filtered
          }

        val skipped =
          start match {
            case Some(s) => sorted.skip(s)
            case None    => sorted
          }

        val medals: Future[List[Medal]] =
          skipped
            .cursor[Medal]()
            .collect[List](maxDocs = count.getOrElse(-1), Cursor.FailOnError())

        medals.map { medals =>
          Ok(Json.toJson(medals))
        }
      }
    }

  def stream(
    order: Option[String],
    start: Option[Int],
    count: Option[Int],
    year: Option[Int],
    medal: Option[String],
  ): Action[AnyContent] =
    Action.async { request =>
      reactiveMongoApi.database.map { db =>
        val query: JsObject =
          year.fold(JsObject.empty)(y => Json.obj("Year" -> y)) ++
            medal.fold(JsObject.empty)(m => Json.obj("Medal" -> m))

        val filtered =
          db.collection[JSONCollection]("medals")
            .find(query, projection = Option.empty[JsObject])

        val sorted =
          order match {
            case Some("Year")  => filtered.sort(Json.obj("Year" -> 1))
            case Some("Medal") => filtered.sort(Json.obj("Medal" -> 1))
            case _             => filtered
          }

        val skipped =
          start match {
            case Some(s) => sorted.skip(s)
            case None    => sorted
          }

        val medals: Source[JsValue, Future[State]] =
          skipped
            .cursor[Medal]()
            .documentSource()
            .map(m => Json.toJson(m))

        Ok.streamed(medals, None, Some("application/stream+json"))
      }
    }
}
