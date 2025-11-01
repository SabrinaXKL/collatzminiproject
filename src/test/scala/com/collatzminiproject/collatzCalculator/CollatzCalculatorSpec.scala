package com.collatzminiproject.collatzCalculator

import cats.effect.IO
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF

class CollatzCalculatorSpec extends CatsEffectSuite with ScalaCheckEffectSuite {

  def evenIntGen: Gen[Int] = for {
    generatedInt <- Gen.chooseNum(1, Int.MaxValue)
    if generatedInt % 2 == 0
  } yield generatedInt

  def oddIntNotTooBigGen: Gen[Int] = for {
    generatedInt <- Gen.chooseNum(1, (Int.MaxValue - 1) / 3)
    if generatedInt % 2 != 0
  } yield generatedInt

  def negativeAndZeroIntGen: Gen[Int] = Gen.negNum

  def oddIntTooBigGen: Gen[Int] = for {
    generatedInt <- Gen.chooseNum((Int.MaxValue - 1) / 3 + 1, Int.MaxValue)
    if generatedInt % 2 != 0
  } yield generatedInt

  object calculatorImplemented extends CollatzCalculator

  test("Given an even positive integer x, return y the next collatz conjecture number") {
    PropF.forAllF(evenIntGen) { x =>
      calculatorImplemented.calculateNextCollatzNumber(x).start.flatMap(_.joinWithNever).map(a => assert(a == x / 2))
    }
  }

  test("Given a odd positive integer x and the next collatz conjecture number y doesnt cause a stack overflow, return y") {
    PropF.forAllF(oddIntNotTooBigGen) { x =>
      calculatorImplemented.calculateNextCollatzNumber(x).start.flatMap(_.joinWithNever).map(a => assert(a == x * 3 + 1))
    }
  }

  test("Given a negative integer x, CollatzCalculator should raise a RuntimeException with message Integer is negative") {
    PropF.forAllF(negativeAndZeroIntGen) { x =>
      calculatorImplemented.calculateNextCollatzNumber(x).attempt.map {
        case Left(e: RuntimeException) =>
          assert(e.getMessage.contains("Integer is negative"))
        case Left(e) => fail(s"Unexpected exception type: ${e.getClass}")
        case Right(_) => fail("Expected failure but got success")
      }
    }
  }
  
  test("Given an integer x which will cause a stack overflow, CollatzCalculator should raise a RuntimeException with message Integer Overflow error") {
    PropF.forAllF(oddIntTooBigGen) { x =>
      calculatorImplemented.calculateNextCollatzNumber(x).attempt.map {
        case Left(e: RuntimeException) =>
          assert(e.getMessage.contains("Integer Overflow error"))
        case Left(e) => fail(s"Unexpected exception type: ${e.getClass}")
        case Right(_) => fail("Expected failure but got success")
      }
    }
  }
}
