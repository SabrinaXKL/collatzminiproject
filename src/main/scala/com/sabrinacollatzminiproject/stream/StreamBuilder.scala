package com.sabrinacollatzminiproject.stream

import cats.effect.*
import com.sabrinacollatzminiproject.collatzCalculator.CollatzCalculator
import fs2.*

import scala.concurrent.duration.*
import fs2.concurrent.SignallingRef
import org.http4s.{MediaType, Message, Response, ServerSentEvent}
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

import scala.collection.immutable.HashMap

object StreamBuilder extends CollatzCalculator {
  type FiberIO = Fiber[IO, Throwable, Unit]
  type InternalMachineState = SignallingRef[IO, Int]
  type InternalMachineStateAndFiber = (FiberIO, InternalMachineState)
  type MapOfAllMachinesById = HashMap[String, InternalMachineStateAndFiber]

  val mapOfAllMachinesByIdRef: Ref[IO, MapOfAllMachinesById] =
    Ref.unsafe[IO, MapOfAllMachinesById](HashMap.empty[String, InternalMachineStateAndFiber])

  
  def createMachine(id: String, startNumber: Int): IO[Response[IO]] =
    def createStream(startNumber: Int, signallingRef: InternalMachineState): Stream[IO, Unit] = {
      Stream.eval(signallingRef.set(startNumber)) ++ Stream
        .awakeEvery[IO](1.second)
        .evalMap(_ =>
          signallingRef.get.flatMap(signallingRefVal =>
            if signallingRefVal == 1 then signallingRef.set(startNumber)
            else calculateNextCollatzNumber(signallingRefVal).flatMap(nextCollatzNumber =>
              signallingRef.set(nextCollatzNumber)
            )
          )
        )
    }

    for {
      internalMachineState <- SignallingRef[IO, Int](startNumber)
      fiber <- createStream(startNumber, internalMachineState).compile.drain.start
      creationAttempt <- mapOfAllMachinesByIdRef.modify { mapVal =>
        if (mapVal.contains(id)) {
          (mapVal, false)
        } else {
          (mapVal + (id -> (fiber, internalMachineState)), true)
        }
      }
      response <- if (creationAttempt) {
        Created(s"Machine created with id: $id")
      } else {
        fiber.cancel >> IO.raiseError(throw new IllegalArgumentException("Machine ID already exists"))
      }
    } yield response

  def incrementMachine(id: String, inputInt: Int): IO[Response[IO]] = for {
    refOption <- mapOfAllMachinesByIdRef.get
    result <- refOption.get(id) match {
      case Some(fiber, internalMachineState) =>
        internalMachineState.update(_ + inputInt) >> Ok(s"machine with id: $id updated by $inputInt")
      case _ => NotFound(s"Could not find machine with id: $id")
    }
  } yield result

  def destroyMachine(id: String): IO[Response[IO]] = for {
    mapVal <- mapOfAllMachinesByIdRef.get
    result <- mapVal.get(id) match {
      case Some(fiber, _) =>
        fiber.cancel >> mapOfAllMachinesByIdRef.update(mapVal => mapVal - id) >> Ok(s"Machine with id: $id destroyed")
      case _ => NotFound(s"Could not find machine with id: $id")
    }
  } yield result

  def getMessageFromId(id: String): IO[Response[IO]] = for {
    mapVal <- mapOfAllMachinesByIdRef.get
    result <- mapVal.get(id) match {
      case Some(_, internalMachineState) =>
        val stream = internalMachineState.discrete.map { value =>
          ServerSentEvent(data = Some(s"Machine $id current value: $value"))
        }
        Ok(stream).map(_.withContentType(`Content-Type`(MediaType.`text/event-stream`)))
      case _ => NotFound(s"Could not find machine with id: $id")
    }
  } yield result

  def getAllMessages = (for {
    mapVal <- mapOfAllMachinesByIdRef.get
    result <-
      val streamOfStreams = mapVal.map(a => a._2._2.discrete.map { value =>
          ServerSentEvent(data = Some(s"Machine ${a._1} current value: $value"))
      }).reduce(_.merge(_))
      Ok(streamOfStreams).map(_.withContentType(`Content-Type`(MediaType.`text/event-stream`)))
  } yield result).handleErrorWith(e => NotFound("Something went wrong"))


}
