package org.scalasapporo.gamecenter.shared

import scala.concurrent.Future

trait Api {
  def echo(message: String): String
  def register(nickname: String): Future[String]
  def users(): Future[Seq[String]]
}
