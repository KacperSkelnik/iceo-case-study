package pl.kacske.rabbitmq

import cats.effect.implicits.genSpawnOps
import cats.effect.std.Queue
import cats.effect.{Async, Concurrent}
import cats.implicits._
import dev.profunktor.fs2rabbit.config.declaration.DeclarationQueueConfig
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model._
import fs2.{Pipe, Stream}
import pl.kacske.processing.Result

sealed trait ClientWrapper[F[_]] {
  def publisherPipe: Pipe[F, Result, Result]
  def consumerPipe(queueName: QueueName): F[Pipe[F, String, String]]
}

object ClientWrapper {
  def make[F[_]: Async: Concurrent](
      client: RabbitClient[F],
      rabbitConnection: AMQPConnection
  ): ClientWrapper[F] = new ClientWrapper[F] {

    private[rabbitmq] def publisher(
        moduloResult: String,
        message: String
    ): F[Unit] =
      client.createChannel(rabbitConnection).use { implicit channel =>
        for {
          exchangeName <- ExchangeName("modulo.topic").pure[F]
          _ <- client.declareExchange(exchangeName, ExchangeType.Topic)
          _ <- client.declareQueue(DeclarationQueueConfig.default(QueueName(moduloResult)))
          _ <- client.bindQueue(QueueName(moduloResult), exchangeName, RoutingKey(moduloResult))
          _ <- client
            .createPublisher[String](
              exchangeName,
              RoutingKey(moduloResult)
            )
            .flatMap(f => f(message))
        } yield ()
      }

    def publisherPipe: Pipe[F, Result, Result] = in =>
      in.evalTap(result =>
        publisher(
          result.moduloResult.toString,
          result.sum.toString
        )
      )

    def consumerPipe(queueName: QueueName): F[Pipe[F, String, String]] =
      for {
        outQueue <- Queue.unbounded[F, Option[String]]
        outStream = Stream.fromQueueNoneTerminated(outQueue)
        _ <- client
          .createChannel(rabbitConnection)
          .use { implicit channel =>
            for {
              _ <- client.declareQueue(DeclarationQueueConfig.default(QueueName(queueName.value)))
              consumer <- client.createAutoAckConsumer[String](queueName)
              _ <- consumer
                .evalTap(element => outQueue.offer(element.payload.some))
                .compile
                .drain
            } yield ()
          }
          .start
      } yield (_: fs2.Stream[F, String]) => outStream

  }
}
