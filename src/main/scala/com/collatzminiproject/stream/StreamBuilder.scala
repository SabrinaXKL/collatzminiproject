package com.collatzminiproject.stream

import com.collatzminiproject.collatzCalculator.CollatzCalculator
import fs2.*

import scala.concurrent.duration.*
import fs2.concurrent.SignallingRef
import org.http4s.{MediaType, Response, ServerSentEvent}
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`
import cats.effect.*

import com.collatzminiproject.models.{IOMapRefOptionVal, Machine, TopicSSE}


object StreamBuilder extends CollatzCalculator {

  def createMachine(id: String, startNumber: Int)(using machinesRef: IOMapRefOptionVal, topic: TopicSSE): IO[Response[IO]] =
    def createStream(startNumber: Int, signallingRef: SignallingRef[IO, Int]): Stream[IO, Unit] = {
      Stream.eval(signallingRef.set(startNumber)) ++ Stream
        .awakeEvery[IO](1.second)
        .evalMap(_ =>
          signallingRef.update { current =>
            if current == 1 then startNumber
            else calculateNextCollatzNumber(current)
          } >>
            signallingRef.get.flatMap { value =>
              topic.publish1((id, value)).void
            }
        )
    }

    for {
      internalMachineState <- SignallingRef[IO, Int](startNumber)
      fiber <- createStream(startNumber, internalMachineState).compile.drain.start
      machineRef = machinesRef(id)
      inserted <- machineRef.modify {
        case None =>
          (Some(Machine(fiber, internalMachineState)), true)
        case someExisting =>
          (someExisting, false)
      }
      response <- if inserted then
        Created(s"Machine created with id: $id")
      else
        fiber.cancel >> IO.raiseError(new IllegalArgumentException(s"Machine with ID: $id already exists"))
    } yield response

  def incrementMachine(id: String, inputInt: Int)(using machinesRef: IOMapRefOptionVal): IO[Response[IO]] = {
    val machineRef = machinesRef(id)
    machineRef.get.flatMap {
      case Some(machine) =>
        machine.state.update(_ + inputInt) >>
          Ok(s"Machine with id: $id updated by $inputInt")
      case None =>
        NotFound(s"Could not find machine with id: $id")
    }
  }

  def destroyMachine(id: String)(using machinesRef: IOMapRefOptionVal): IO[Response[IO]] = {
    val machineRef = machinesRef(id)
    machineRef.modify {
      case Some(machine) =>
        (None, Some(machine))
      case None =>
        (None, None)
    }.flatMap {
      case Some(machine) =>
        machine.fiber.cancel >> Ok(s"Machine with id: $id destroyed")
      case None =>
        NotFound(s"Could not find machine with id: $id")
    }
  }

  def getMessageFromId(id: String)(using topic: TopicSSE): IO[Response[IO]] = {
    val stream: Stream[IO, ServerSentEvent] =
      topic
        .subscribe(100)
        .filter { case (machineId, _) => machineId == id }
        .map { case (_, value) =>
          ServerSentEvent(data = Some(s"Machine $id current value: $value"))
        }

    Ok(stream).map(_.withContentType(`Content-Type`(MediaType.`text/event-stream`)))
  }

  def getAllMessages()(using topic: TopicSSE) = {
    val stream = topic.subscribe(100)
      .map { case (machineId, value) =>
        ServerSentEvent(data = Some(s"Machine $machineId current value: $value"))
      }
    Ok(stream).map(_.withContentType(`Content-Type`(MediaType.`text/event-stream`)))
  }
}
