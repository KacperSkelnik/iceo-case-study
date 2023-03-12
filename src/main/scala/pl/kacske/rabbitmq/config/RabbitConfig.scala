package pl.kacske.rabbitmq.config

import cats.data.NonEmptyList
import cats.effect.{Async, Resource}
import cats.implicits._
import ciris.env
import dev.profunktor.fs2rabbit.config.{Fs2RabbitConfig, Fs2RabbitNodeConfig}

import scala.concurrent.duration.DurationInt

object RabbitConfig {
  def get[F[_]: Async]: Resource[F, Fs2RabbitConfig] = {
    (
      env("RABBIT_HOST").default("localhost").as[String],
      env("RABBIT_USERNAME").default("guest").as[String],
      env("RABBIT_PASSWORD").default("guest").as[String].secret
    ).parMapN((host, username, password) =>
      Fs2RabbitConfig(
        virtualHost = "/",
        nodes = NonEmptyList.one(Fs2RabbitNodeConfig(host = host, port = 5672)),
        username = Some(username),
        password = Some(password.value),
        ssl = false,
        connectionTimeout = 3.seconds,
        requeueOnNack = false,
        requeueOnReject = false,
        internalQueueSize = Some(500),
        requestedHeartbeat = 60.seconds,
        automaticRecovery = true,
        clientProvidedConnectionName = Some("app:io-acker-consumer")
      )
    ).resource[F]
  }
}
