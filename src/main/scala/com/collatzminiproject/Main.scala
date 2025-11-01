package com.collatzminiproject

import cats.*
import cats.effect.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jFactory
import com.collatzminiproject.routes.Routes
import org.http4s.ember.server.EmberServerBuilder
import config.{AppConfig, ConfigLoader}
import cats.syntax.all.*
import com.collatzminiproject.errors.Errors.failedSetupError
import fs2.concurrent.Topic
import org.http4s.HttpApp

object Main extends IOApp {
  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  val logger = LoggerFactory[IO].getLogger

  def setup(args: List[String]): IO[Either[Throwable, AppConfig]] =
    ConfigLoader.loadConfig().attempt

//  def resources: Resource[IO, Topic[IO, Int]] =
//    for {
//      topic <- Resource.eval(Topic[IO, Int])
//    } yield topic
//
//  def httpApp(resources: Resource[IO, Topic[IO, Int]]): HttpApp[IO] =
//    Routes.apis(resources)

  def run(args: List[String]): IO[ExitCode] = {
    setup(args).flatMap {
      case Left(throwable) => logger.error(throwable)(failedSetupError) *> ExitCode.Error.pure
      case Right(appConfig) =>
        EmberServerBuilder
          .default[IO]
          .withHost(appConfig.host)
          .withPort(appConfig.port)
          .withHttpApp(Routes.apis)
          .build
          .useForever
          .as(ExitCode.Success)
    }
  }


}
