  package com.collatzminiproject

  import cats.*
  import cats.effect.*
  import cats.effect.std.MapRef
  import org.typelevel.log4cats.*
  import org.typelevel.log4cats.slf4j.Slf4jFactory
  import com.collatzminiproject.routes.Routes
  import org.http4s.ember.server.EmberServerBuilder
  import config.{AppConfig, ConfigLoader}
  import cats.syntax.all.*
  import com.collatzminiproject.errors.Errors.failedSetupError
  import com.collatzminiproject.models.{IOMapRefOptionVal, Machine, TopicSSE}
  import fs2.concurrent.Topic

  object Main extends IOApp {
    given LoggerFactory[IO] = Slf4jFactory.create[IO]

    val logger = LoggerFactory[IO].getLogger

    def setup(args: List[String]): IO[Either[Throwable, AppConfig]] =
      ConfigLoader.loadConfig().attempt

    def makeMachines: Resource[IO, MapRef[IO, String, Option[Machine]]] =
      Resource.eval(
        MapRef.inConcurrentHashMap[IO, IO, String, Machine]()
      )

    def makeTopic: Resource[IO, TopicSSE] =
      Resource.eval(Topic[IO, (String, Int)])

    def run(args: List[String]): IO[ExitCode] = {
      setup(args).flatMap {
        case Left(throwable) => logger.error(throwable)(failedSetupError) *> ExitCode.Error.pure
        case Right(cfg) =>
          val resources = for {
            machinesRef <- makeMachines
            topic       <- makeTopic
          } yield (machinesRef, topic, cfg)

          resources.use { case (machinesRef, topic, appConfig)  =>
            given IOMapRefOptionVal = machinesRef
            given TopicSSE = topic
            
            EmberServerBuilder
              .default[IO]
              .withHost(appConfig.host)
              .withPort(appConfig.port)
              .withHttpApp(Routes.apis())
              .build
              .useForever
              .as(ExitCode.Success)
          }
      }
    }


  }
