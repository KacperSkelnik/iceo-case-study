package pl.kacske.processing.config

import cats.effect.{Resource, Async}
import cats.implicits._
import ciris.env
import fs2.io.file.Path

final case class ProcessorConfig(
    moduloDivider: Int,
    filePath: Path
)

object ProcessorConfig {
  def get[F[_]: Async]: Resource[F, ProcessorConfig] =
    (
      env("MODULO_DIVIDER").as[Int],
      env("FILE_PATH").as[String].map(Path(_))
    ).parMapN(ProcessorConfig(_, _)).resource[F]
}
