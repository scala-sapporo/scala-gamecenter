package org.scalasapporo.check

import org.scalasapporo.gamecenter.connector.{ProcessConnector, ProcessConnectorContext}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val ctx = ProcessConnectorContext("./check/scripts/check.sh")
    println(ProcessConnector.execute("check"))
  }
}
