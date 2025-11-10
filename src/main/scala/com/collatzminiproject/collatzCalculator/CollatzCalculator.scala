package com.collatzminiproject.collatzCalculator

import cats.effect.IO

trait CollatzCalculator {
  def calculateNextCollatzNumber(inputValue: Int): Int =
    inputValue match {
      case input if input < 1 => throw new RuntimeException(s"[ERROR - EO3] Integer is negative")
      case input if (input & 1) == 0 => input >> 1
      case input =>
        try {
          Math.addExact(Math.multiplyExact(input, 3), 1)
        } catch {
          case _: ArithmeticException =>
            throw new RuntimeException("[ERROR - EO3] Integer Overflow error")
        }
    }
}
