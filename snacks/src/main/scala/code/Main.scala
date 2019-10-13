package code

import reactivemongo.api.{Cursor, DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object Main {
  val snacks = List(
    Snack("Chocolate", 8, healthy = false),
    Snack("Crisps", 8, healthy = false),
    Snack("Celery", 4, healthy = true)
  )

  val driver = MongoDriver()

  def openConnection(driver: MongoDriver): Try[MongoConnection] =
    for {
      uri  <- MongoConnection.parseURI(s"mongodb://localhost:27017")
      conn <- driver.connection(uri, strictUri = true)
    } yield conn

  def openSnackDatabase(connection: MongoConnection): Future[DefaultDB] =
    connection.database("snackshop")

  def insertSnack(database: DefaultDB, snack: Snack): Future[Unit] =
    database
      // Gotcha: Make sure you specify [BSONCollection] here.
      .collection[BSONCollection]("snacks")
      .insert.one(snack).map(_ => ())

  def selectAllSnacks(database: DefaultDB): Future[List[Snack]] =
    database
      // Gotcha: Make sure you specify [BSONCollection] here.
      .collection[BSONCollection]("snacks")
      .find(selector = BSONDocument(), projection = None)
      .cursor[Snack]()
      .collect[List](maxDocs = -1, Cursor.FailOnError())

  def program: Future[List[Snack]] =
    for {
      conn <- Future.fromTry(openConnection(driver))
      db <- openSnackDatabase(conn)
      _ <- Future.traverse(snacks)(insertSnack(db, _))
      snacks <- selectAllSnacks(db)
    } yield snacks

  def main(args: Array[String]): Unit = {
    println(Await.result(program, 5.seconds))
    driver.close(2.seconds)
  }
}
