package org.scalasapporo.check

import org.scalasapporo.check.ShellConnector.Input
import org.scalasapporo.gamecenter.connector.ProcessConnector

object Main {
  def main(args: Array[String]): Unit = {
    println(ShellConnector.execute(Input(100, "check")))
  }
}

object ShellConnector extends ProcessConnector {
  case class Input(id: Int, description: String)
  case class Output(result: String)
  override type Request = Input
  override type Response = Output
  override def program: String = "./check/scripts/check.sh"
  override def req2str(request: Input): String = s"""${request.id},"${request.description}""""
  override def str2res(result: String): Output = Output(result)
}
