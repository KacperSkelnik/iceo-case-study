package pl.kacske

import cats.effect.{IO, IOApp}
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.{AMQPConnection, QueueName}
import fs2.Pipe
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2
import pl.kacske.rabbitmq.ClientWrapper
import pl.kacske.rabbitmq.config.RabbitConfig
import sttp.apispec.asyncapi.circe.yaml.RichAsyncAPI
import sttp.capabilities
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.docs.asyncapi.AsyncAPIInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.{CodecFormat, Endpoint, endpoint, query, webSocketBody}

object RabbitWebSocketService extends IOApp.Simple {

  def moduloEndpoint: Endpoint[Unit, String, Unit, Pipe[IO, String, String], Any with Fs2Streams[IO] with capabilities.WebSockets] =
    endpoint.get
      .in(query[String]("number"))
      .out(webSocketBody[String, CodecFormat.TextPlain, String, CodecFormat.TextPlain](Fs2Streams[IO]))

  def moduloRoutes(rabbitClient: RabbitClient[IO], rabbitConnection: AMQPConnection): WebSocketBuilder2[IO] => HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toWebSocketRoutes(
      moduloEndpoint.serverLogicSuccess[IO](number =>
        ClientWrapper.make[IO](rabbitClient, rabbitConnection).consumerPipe(QueueName(number))
      )
    )

  def apiDocs: String =
    AsyncAPIInterpreter()
      .toAsyncAPI(
        moduloEndpoint,
        "modulo",
        "1.0",
        List("dev" -> sttp.apispec.asyncapi.Server("0.0.0.0:8080", "ws"))
      )
      .toYaml // can be saved and rendered e.g. by https://studio.asyncapi.com/?redirectedFrom=playground

  def run: IO[Unit] = {
    (for {
      rabbitConfig <- RabbitConfig.get[IO]
      rabbitClient <- RabbitClient.default[IO](rabbitConfig).resource
      rabbitConnection <- rabbitClient.createConnection
    } yield (rabbitClient, rabbitConnection)).use { case (rabbitClient, rabbitConnection) =>
      BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpWebSocketApp(wsb => Router("/ws" -> moduloRoutes(rabbitClient, rabbitConnection)(wsb)).orNotFound)
        .resource
        .useForever
    }
  }

}
