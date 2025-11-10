package com.collatzminiproject.routes

import cats.*
import cats.data.Kleisli
import cats.effect.*
import com.collatzminiproject.errors.Errors.ResponseMessages.somethingWentWrong
import com.collatzminiproject.models.{IOMapRefOptionVal, TopicSSE}
import com.collatzminiproject.stream.StreamBuilder.{createMachine, destroyMachine, getAllMessages, getMessageFromId, incrementMachine}
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import com.collatzminiproject.stream.{StreamBuilderFailure, StreamBuilderResponse, StreamBuilderSuccess}
import org.http4s.headers.`Content-Type`

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

  def postCreateRoute()(using machinesRef: IOMapRefOptionVal, topic: TopicSSE): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / id / startNumber =>
        (for {
          _ <- logger.info(s"[POST] Create a machine with start number: $startNumber and id: $id")
          startNumberValidated <- validateInteger(startNumber, s"[ERROR- POST CREATE] could not validate startNumber $startNumber on id $id")
          createMachineResult <- createMachine(id, startNumberValidated)
          response <- Ok(createMachineResult.secondInput.getOrElse("Machine created"))
        } yield response).handleErrorWith {
          case e: IllegalArgumentException => NotFound(somethingWentWrong)
          case e: IllegalStateException => NotFound(somethingWentWrong)
          case e => logger.error(e.getMessage) >> NotFound(somethingWentWrong)
        }
    }
  }

  def postIncrementRoute()(using machinesRef: IOMapRefOptionVal, topic: TopicSSE): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / id / amount => (for {
        _ <- logger.info(s"[POST] Increment the amount by $amount and id: $id")
        amountValidated <- validateInteger(amount, s"[ERROR- POST INCREMENT] could not validate amount $amount on id $id")
        result <- incrementMachine(id, amountValidated)
        response <- Ok(result.secondInput.getOrElse("Machine updated"))
      } yield response).handleErrorWith(e => NotFound(somethingWentWrong))
    }
  }

  def postDestroyRoute()(using machinesRef: IOMapRefOptionVal, topic: TopicSSE): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / id => (for {
        _ <- logger.info(s"[POST - destroy] Destroy the machine with id: $id")
        result <- destroyMachine(id)
        response <- result match {
          case StreamBuilderSuccess(id, secondInput) => Ok(secondInput.getOrElse("Machine destroyed"))
          case StreamBuilderFailure(id, secondInput) => logger.info(s"[POST - destroy] Failure to destroy machine of id: $id") >> NotFound(secondInput.getOrElse("Machine destruction could not be completed"))
        }
      } yield response).handleErrorWith(e => NotFound(somethingWentWrong))
    }
  }

  def getMessagesOfId()(using machinesRef: IOMapRefOptionVal, topic: TopicSSE): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / id => (for {
        _ <- logger.info(s"[GET] Messages with id: $id")
        result = getMessageFromId(id)
        response <- Ok(result).map(_.withContentType(`Content-Type`(MediaType.`text/event-stream`)))
      } yield response).handleErrorWith(e => NotFound(somethingWentWrong))
      case GET -> Root => (for {
        _ <- logger.info(s"[GET] all messages across all machines")
        result = getAllMessages()
        response <- Ok(result).map(_.withContentType(`Content-Type`(MediaType.`text/event-stream`)))
      } yield response).handleErrorWith(e => NotFound(somethingWentWrong))
    }
  }

  def apis()(using machinesRef: IOMapRefOptionVal, topic: TopicSSE): Kleisli[IO, Request[IO], Response[IO]] = Router(
    "/create" -> postCreateRoute(),
    "/increment" -> postIncrementRoute(),
    "/destroy" -> postDestroyRoute(),
    "/messages" -> getMessagesOfId()
  ).orNotFound

}
