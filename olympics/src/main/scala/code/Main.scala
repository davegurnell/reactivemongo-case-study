package code

import com.github.tototoshi.csv._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONDocument, BSONString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.io.{BufferedSource, Source}

object Main {
  def rawMedalData: Iterator[Map[String, String]] = {
    val source: BufferedSource =
      Source.fromInputStream(getClass.getResourceAsStream("/medals-expanded.csv"))

    val lines: Iterator[Seq[String]] =
      CSVReader.open(source).iterator

    val titles: Seq[String] =
      lines.next

    val values: Iterator[Map[String, String]] =
      lines.map(values => titles.zip(values).toMap)

    values
  }

  def medalBSON: Iterator[BSONDocument] =
    rawMedalData.map { data =>
      BSONDocument(data.map {
        case (key, value) =>
          key -> BSONString(value)
      })
    }

  val driver = MongoDriver()

  def collection: Future[BSONCollection] =
    for {
      uri  <- Future.fromTry(MongoConnection.parseURI(s"mongodb://localhost:27017"))
      conn <- Future.fromTry(driver.connection(uri, strictUri = true))
      db   <- conn.database("olympics")
    } yield db.collection[BSONCollection]("medals")

  def program: Future[Unit] =
    for {
      coll <- collection
      // This fails with repeated exceptions:
      //     'No active channel can be found to the primary node:
      //         'localhost:27017' { connected:1, channels:2 } (Supervisor-1/Connection-1)'
      _    <- Future.traverse(rawMedalData)(doc => coll.insert.one(doc))
      // This does not:
      // _    <- coll.insert.many(rawMedalData.toIterable)
    } yield ()

  def main(args: Array[String]): Unit = {
    Await.result(program, 60.seconds)
  }
}
