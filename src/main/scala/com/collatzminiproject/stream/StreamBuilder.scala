package com.collatzminiproject.stream

import com.collatzminiproject.collatzCalculator.CollatzCalculator
import fs2.*

import scala.concurrent.duration.*
import fs2.concurrent.SignallingRef
import org.http4s.ServerSentEvent

import cats.effect.*

import com.collatzminiproject.models.{IOMapRefOptionVal, Machine, TopicSSE}

object StreamBuilder extends CollatzCalculator {

  def createMachine(id: String, startNumber: Int)(using machinesRef: IOMapRefOptionVal, topic: TopicSSE): IO[StreamBuilderSuccess] =
    def createStream(startNumber: Int, internalState: Ref[IO, Int]): Stream[IO, Unit] = {
      Stream.eval(internalState.set(startNumber)) ++ Stream
        .awakeEvery[IO](1.second)
        .evalMap(_ =>
          internalState.update { current =>
            if current == 1 then startNumber
            else calculateNextCollatzNumber(current)
          } >>
            internalState.get.flatMap { value =>
              topic.publish1((id, value)).void
            }
        )
    }

    for {
      internalMachineState <- Ref.of[IO, Int](startNumber)
      fiber <- createStream(startNumber, internalMachineState).compile.drain.start
      machineRef = machinesRef(id)
      inserted <- machineRef.modify {
        case None =>
          (Some(Machine(fiber, internalMachineState)), true)
        case someExisting =>
          (someExisting, false)
      }
      response <- if inserted then
        IO.pure(StreamBuilderSuccess(id, Some(s"Machine created with id: $id")))
      else
        fiber.cancel >> IO.raiseError(new IllegalArgumentException(s"Machine with id: $id already exists"))
    } yield response

  def incrementMachine(id: String, inputInt: Int)(using machinesRef: IOMapRefOptionVal): IO[StreamBuilderSuccess] = {
    val machineRef = machinesRef(id)
    machineRef.get.flatMap {
      case Some(machine) =>
        machine.state.update(_ + inputInt) >>
          IO.pure(StreamBuilderSuccess(id, Some(s"Updated by $inputInt")))
      case None =>
        IO.raiseError(new IllegalArgumentException(s"Could not find machine with id: $id"))
    }
  }

  def destroyMachine(id: String)(using machinesRef: IOMapRefOptionVal): IO[StreamBuilderResponse] = {
    val machineRef = machinesRef(id)
    machineRef.modify {
      case Some(machine) =>
        (None, Some(machine))
      case None =>
        (None, None)
    }.flatMap {
      case Some(machine) =>
        machine.fiber.cancel >> IO.pure(StreamBuilderSuccess(id, Some(s"Machine with id: $id destroyed")))
      case None =>
        IO.pure(StreamBuilderFailure(id, Some(s"Could not find machine with id: $id")))
    }
  }

  def getMessageFromId(id: String)(using topic: TopicSSE): Stream[IO, ServerSentEvent] = {
    topic
      .subscribe(100)
      .filter { case (machineId, _) => machineId == id }
      .map { case (_, value) =>
        ServerSentEvent(data = Some(s"Machine $id current value: $value"))
      }
  }

  def getAllMessages()(using topic: TopicSSE): Stream[IO, ServerSentEvent] = {
    topic.subscribe(100)
      .map { case (machineId, value) =>
        ServerSentEvent(data = Some(s"Machine $machineId current value: $value"))
      }
  }
}
