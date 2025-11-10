package com.collatzminiproject.routes

import cats.effect.*
import cats.effect.std.MapRef
import com.collatzminiproject.models.{IOMapRefOptionVal, Machine, TopicSSE}
import org.http4s.ember.client.*
import org.http4s.implicits.*
import munit.CatsEffectSuite
import com.comcast.ip4s.*
import fs2.concurrent.Topic
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.http4s.Method.*
import org.http4s.Request

class RoutesSpec extends CatsEffectSuite {
  implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]
  implicit val topic: TopicSSE = Topic[IO, (String, Int)].unsafeRunSync()
  implicit val machinesRef: IOMapRefOptionVal = MapRef.inConcurrentHashMap[IO, IO, String, Machine]().unsafeRunSync()

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"127.0.0.1")
    .withPort(port"8080")
    .withHttpApp(Routes.apis())
    .build

  test("[POST] create normal post works to create an id of string x with number y") {
    server.use { server =>
      val clientRes = EmberClientBuilder.default[IO].build
      val id = "ao1"
      clientRes.use { client =>
        val uri = uri"http://localhost:8080" / s"create" / id / 50
        client.expect[String](Request[IO](POST, uri)).map { body =>
          assert(body == s"Machine created with id: $id")
        }
      }
    }
  }
}
