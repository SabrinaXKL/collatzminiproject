package com.collatzminiproject.stream

import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import fs2.concurrent.SignallingRef
import munit.CatsEffectSuite
import scala.concurrent.duration._

import com.collatzminiproject.collatzCalculator.CollatzCalculator

class StreamBuilderSpec extends CatsEffectSuite with CollatzCalculator {

  def takeValues(stateRef: SignallingRef[IO, Int], count: Int, delay: FiniteDuration): IO[List[Int]] =
    Stream.awakeEvery[IO](delay)
      .evalMap(_ => stateRef.get)
      .take(count.toLong)
      .compile
      .toList

  test("createMachine should initialise a machine and update Collatz sequence") {
    val id = "test-machine1"
    val start = 7

    for {
      resp <- StreamBuilder.createMachine(id, start)
      _ <- IO(assertEquals(resp.status.code, 200))
      map <- StreamBuilder.mapOfAllMachinesByIdRef.get
      entryOpt = map.get(id)
      _ <- IO(assert(entryOpt.isDefined, s"Machine with id $id should exist"))
      (_, stateRef) = entryOpt.get
      values <- takeValues(stateRef, count = 5, delay = 200.millis)
      _ = assertEquals(values.head, start)
      _ = assert(values.tail.exists(_ != start), s"Expected some Collatz progression, got $values")
      _ = assert(values.contains(1), s"Sequence should hit 1: $values")
      _ = assert(values.contains(start), s"After hitting 1 it should reset to start: $values")
    } yield ()
  }

//  test("multiple machines can run independently") {
//    val idA = "machineA"
//    val idB = "machineB"
//    val startA = 5
//    val startB = 10
//
//    for {
//      _ <- StreamBuilder.createMachine(idA, startA)
//      _ <- StreamBuilder.createMachine(idB, startB)
//      map <- StreamBuilder.mapOfAllMachinesByIdRef.get
//      _ <- IO(assert(map.contains(idA), s"map should contain $idA"))
//      _ <- IO(assert(map.contains(idB), s"map should contain $idB"))
//      (_, stateA) = map(idA)
//      (_, stateB) = map(idB)
//      valsA <- takeValues(stateA, count = 3, delay = 150.millis)
//      valsB <- takeValues(stateB, count = 3, delay = 150.millis)
//      _ = assertEquals(valsA.head, startA)
//      _ = assertEquals(valsB.head, startB)
//      _ = assert(valsA.tail.exists(_ != startA))
//      _ = assert(valsB.tail.exists(_ != startB))
//    } yield ()
//  }
}