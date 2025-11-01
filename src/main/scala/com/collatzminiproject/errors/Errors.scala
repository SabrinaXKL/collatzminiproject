package com.collatzminiproject.errors

object Errors
 val failedSetupError = "FAILED SETUP WITH ERROR: "
  def startNumberError(id: String, startNumber: String) = s"[ERROR- POST CREATE] could not validate startNumber $startNumber on id $id"

object RoutesLogging:
  def postCreateLog(id: String, startNumber: String) = s"[POST] Create a machine with start number: $startNumber and id: $id"

object ResponseMessages:
  val somethingWentWrong = "Something went wrong"