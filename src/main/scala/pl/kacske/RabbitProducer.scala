package pl.kacske

import cats.effect.{IO, IOApp}
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import pl.kacske.processing.File
import pl.kacske.processing.config.ProcessorConfig
import pl.kacske.rabbitmq.ClientWrapper
import pl.kacske.rabbitmq.config.RabbitConfig

object RabbitProducer extends IOApp.Simple {
  def run: IO[Unit] =
    (for {
      config <- ProcessorConfig.get[IO]
      rabbitConfig <- RabbitConfig.get[IO]
      rabbitClient <- RabbitClient.default[IO](rabbitConfig).resource
      rabbitConnection <- rabbitClient.createConnection
    } yield (config, rabbitClient, rabbitConnection)).use { case (config, rabbitClient, rabbitConnection) =>
      File
        .make[IO]
        .get(
          config.filePath,
          config.moduloDivider
        )
        .map(result => result.through(ClientWrapper.make[IO](rabbitClient, rabbitConnection).publisherPipe))
        .parJoin(config.moduloDivider)
        .compile
        .drain
    }
}
