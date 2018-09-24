package org.scalasapporo.gamecenter.connector

trait Connector {
  type Request
  type Response
  def execute(request: Request): Response
}

trait ProcessConnector extends Connector {
  import scala.sys.process._
  def program: String
  def req2str(request: Request): String
  def str2res(result: String): Response
  override def execute(request: Request): Response = str2res {
    Process(Seq(program, req2str(request))).!!
  }
}
