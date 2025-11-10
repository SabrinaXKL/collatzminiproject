package com.collatzminiproject.models

import cats.effect.std.MapRef
import cats.effect.{Fiber, IO, Ref}
import fs2.concurrent.Topic

case class Machine(fiber: Fiber[IO, Throwable, Unit], state: Ref[IO, Int])

type IOMapRefOptionVal = MapRef[IO, String, Option[Machine]]
type TopicSSE = Topic[IO, (String, Int)]
