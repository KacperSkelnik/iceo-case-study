package pl.kacske.processing

import fs2.data.csv.generic.semiauto._
import fs2.data.csv.{RowDecoder, RowEncoder}

final case class CSVRow(
    value: Int
)

object CSVRow {
  object implicits {
    implicit val numberDecoder: RowDecoder[CSVRow] = deriveRowDecoder[CSVRow]

    implicit val numberEncoder: RowEncoder[CSVRow] = deriveRowEncoder[CSVRow]
  }
}
