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

  // Insert most of you app here...

  def main(args: Array[String]): Unit = {
    // Insert a single line to kick things off here...
    println("TODO")
  }
}
