package com.collatzminiproject.logger

import cats.effect.IO
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

trait Logging {
  given LoggerFactory[IO] = Slf4jFactory.create[IO]
}
