val catsEffectVersion = "3.4.8"
val tapirVersion = "1.2.9"
val tapirFs2Version = "3.8.12"
val fs2DataVersion = "1.6.1"
val fs2Version = "3.6.1"
val fs2RabbitVersion = "5.0.0"
val cirisVersion = "3.1.0"
val blazeServerVersion = "0.23.13"
val asyncapiDocsVersion = "1.2.9"
val asyncapiCirceVersion = "0.3.2"

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "iceo-case-study",
    version := "1.0.0",
    organization := "pl.kacske",
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.client3" %% "fs2" % tapirFs2Version,
      "com.softwaremill.sttp.tapir" %% "tapir-asyncapi-docs" % asyncapiDocsVersion,
      "com.softwaremill.sttp.apispec" %% "asyncapi-circe-yaml" % asyncapiCirceVersion,
      "org.http4s" %% "http4s-blaze-server" % blazeServerVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "org.gnieh" %% "fs2-data-csv" % fs2DataVersion,
      "org.gnieh" %% "fs2-data-csv-generic" % fs2DataVersion,
      "dev.profunktor" %% "fs2-rabbit" % fs2RabbitVersion,
      "is.cir" %% "ciris" % cirisVersion
    )
  )
)
