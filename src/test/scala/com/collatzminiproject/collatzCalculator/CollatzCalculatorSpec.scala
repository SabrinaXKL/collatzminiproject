package com.collatzminiproject.collatzCalculator

import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.{Gen, Prop}

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
    Prop.forAll(evenIntGen) { x =>
      val nextNumber = calculatorImplemented.calculateNextCollatzNumber(x)
      assert(nextNumber == x / 2)
    }
  }

  test("Given a odd positive integer x and the next collatz conjecture number y doesnt cause a stack overflow, return y") {
    Prop.forAll(oddIntNotTooBigGen) { x =>
      val nextNumber = calculatorImplemented.calculateNextCollatzNumber(x)

      assert(nextNumber == x * 3 + 1)
    }
  }

  test("Given a negative integer x, CollatzCalculator should raise a RuntimeException with message Integer is negative") {
    Prop.forAll(negativeAndZeroIntGen) { x =>
      val ex = intercept[RuntimeException] {
        calculatorImplemented.calculateNextCollatzNumber(x)
      }
      assert(ex.getMessage.contains("Integer is negative"))
    }
  }

  test("Given an integer x which will cause a stack overflow, CollatzCalculator should raise a RuntimeException with message Integer Overflow error") {
    Prop.forAll(oddIntTooBigGen) { x =>
      val ex = intercept[RuntimeException] {
        calculatorImplemented.calculateNextCollatzNumber(x)
      }

      assert(ex.getMessage.contains("Integer Overflow error"))
    }
  }
}
