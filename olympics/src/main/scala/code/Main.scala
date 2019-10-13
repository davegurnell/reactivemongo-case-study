package code

import com.github.tototoshi.csv._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONDocument, BSONString}

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
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

  def openDatabase: Future[DefaultDB] =
    for {
      uri  <- Future.fromTry(MongoConnection.parseURI(s"mongodb://localhost:27017"))
      conn <- Future.fromTry(driver.connection(uri, strictUri = true))
      db   <- conn.database("olympics")
    } yield db

  def program: Future[Unit] =
    for {
      db <- openDatabase
      _  <- Future.traverse(rawMedalData) { doc =>
              println(s"Inserting $doc")
              db.collection[BSONCollection]("medals").insert.one(doc)
            }
    } yield ()

  def main(args: Array[String]): Unit = {
    Await.result(program, 10.seconds)
  }
}
