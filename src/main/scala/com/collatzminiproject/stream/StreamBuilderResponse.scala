package com.collatzminiproject.stream

sealed trait StreamBuilderResponse

case class StreamBuilderSuccess(id: String, secondInput: Option[String] = None) extends StreamBuilderResponse

case class StreamBuilderFailure(id: String, secondInput: Option[String] = None) extends StreamBuilderResponse

