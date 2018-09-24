package org.scalasapporo.gamecenter.connector

trait Connector {
  type Request
  type Response
  def execute(request: Request): Response
}

trait ProcessConnector extends Connector {
  import java.io.File
  val path: File
  override def execute(request: Request): Response = ???
}
