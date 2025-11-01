package com.collatzminiproject.config

import cats.effect._
import com.typesafe.config.*
import com.comcast.ip4s.*

final case class AppConfig(
                            host: Ipv4Address,
                            port: Port,
                            maximumMachineLimit: Int,
                            calculationLimit: Int
                          )

object ConfigLoader {
  def fromConfig(config: Config): IO[AppConfig] = IO {
    val hostStr = config.getString("app.host")
    val portInt = config.getInt("app.port")
    val maximumMachineLimit = config.getInt("machines.maximumMachineLimit")
    val calculationLimit = config.getInt("machines.calculationLimit")
    val host = Ipv4Address
      .fromString(hostStr)
      .getOrElse(throw new IllegalArgumentException(s"Invalid host: $hostStr"))
    val port = Port
      .fromInt(portInt)
      .getOrElse(throw new IllegalArgumentException(s"Invalid port: $portInt"))
    AppConfig(host, port, maximumMachineLimit, calculationLimit)
  }
  
  def loadConfig(): IO[AppConfig] = IO(ConfigFactory.load()).flatMap(fromConfig)
}
