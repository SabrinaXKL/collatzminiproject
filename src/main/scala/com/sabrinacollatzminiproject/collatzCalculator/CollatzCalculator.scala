package com.sabrinacollatzminiproject.collatzCalculator

import cats.effect.IO

trait CollatzCalculator {
  def calculateNextCollatzNumber(inputValue: Int): IO[Int] =
    inputValue match {
      case input if input < 1 => IO.raiseError[Int](new RuntimeException(s"[ERROR - EO3]Integer is negative"))
      case input if (input & 1) == 0 => IO.pure(input >> 1)
      case input =>
        IO {
          val result = Math.addExact(Math.multiplyExact(input, 3), 1)
          result
        }.handleErrorWith(_ =>
          IO.raiseError(new RuntimeException("[ERROR - EO3]Integer Overflow error"))
        )
    }
}
