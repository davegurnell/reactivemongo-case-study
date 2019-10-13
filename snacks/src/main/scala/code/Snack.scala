package code

import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}

case class Snack(
  name: String,
  tastiness: Int,
  healthy: Boolean
)

object Snack {
  implicit val writer: BSONDocumentWriter[Snack] =
    Macros.writer

  implicit val reader: BSONDocumentReader[Snack] =
    Macros.reader
}
