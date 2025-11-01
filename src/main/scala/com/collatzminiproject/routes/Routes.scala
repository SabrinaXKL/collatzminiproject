package com.collatzminiproject.routes

import cats.*
import cats.data.Kleisli
import cats.effect.*
import com.collatzminiproject.errors.Errors.ResponseMessages.somethingWentWrong
import com.collatzminiproject.stream.StreamBuilder.{createMachine, destroyMachine, getAllMessages, getMessageFromId, incrementMachine}
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object Routes {
  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  val logger = LoggerFactory[IO].getLogger

  private def validateInteger(numberToValidate: String, errorMessage: String): IO[Int] = {
    numberToValidate.toIntOption match {
      case Some(validatedNumber) if validatedNumber > 0 =>
        IO.pure(validatedNumber)
      case _ =>
        val errorString = errorMessage
        logger.error(errorString) >> IO.raiseError(new IllegalArgumentException(errorString))
    }
  }

  def postCreateRoute(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / id / startNumber =>
        (for {
          _ <- logger.info(s"[POST] Create a machine with start number: $startNumber and id: $id")
          startNumberValidated <- validateInteger(startNumber,s"[ERROR- POST CREATE] could not validate startNumber $startNumber on id $id")
          createMachineResult <- createMachine(id, startNumberValidated)
        } yield createMachineResult).handleErrorWith {
          case e: IllegalArgumentException => NotFound(somethingWentWrong)
          case e: IllegalStateException => NotFound(somethingWentWrong)
          case e => logger.error(e.getMessage) >> NotFound(somethingWentWrong)
        }
    }
  }

  def postIncrementRoute(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / id / amount => (for {
        _ <- logger.info(s"[POST] Increment the amount by $amount and id: $id")
        amountValidated <- validateInteger(amount, s"[ERROR- POST INCREMENT] could not validate amount $amount on id $id")
        result <- incrementMachine(id, amountValidated)
      } yield result).handleErrorWith(e => NotFound(somethingWentWrong))
    }
  }

  def postDestroyRoute(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / id => (for {
        _ <- logger.info(s"[POST] Destroy the machine with id: $id")
        result <- destroyMachine(id)
      } yield result).handleErrorWith(e => NotFound(somethingWentWrong))
    }
  }

  def getMessagesOfId(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / id => (for {
        _ <- logger.info(s"[GET] Messages with id: $id")
        result <- getMessageFromId(id)
      } yield result).handleErrorWith(e => NotFound(somethingWentWrong))
      case GET -> Root => (for {
        _ <- logger.info(s"[GET] all messages across all machines")
        result <- getAllMessages
      } yield result).handleErrorWith(e => NotFound(somethingWentWrong))
    }
  }

  val apis: Kleisli[IO, Request[IO], Response[IO]] = Router(
    "/create" -> postCreateRoute(),
    "/increment" -> postIncrementRoute(),
    "/destroy" -> postDestroyRoute(),
    "/messages" -> getMessagesOfId()
  ).orNotFound

}
