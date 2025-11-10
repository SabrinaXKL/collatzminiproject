package com.collatzminiproject.models

import cats.effect.std.MapRef
import cats.effect.{Fiber, IO}
import fs2.concurrent.{SignallingRef, Topic}
import org.http4s.ServerSentEvent

case class Machine(fiber: Fiber[IO, Throwable, Unit], state: SignallingRef[IO, Int])

type IOMapRefOptionVal = MapRef[IO, String, Option[Machine]]
type TopicSSE = Topic[IO, (String, Int)]
