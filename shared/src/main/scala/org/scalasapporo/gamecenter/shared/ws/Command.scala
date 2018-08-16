package org.scalasapporo.gamecenter.shared.ws

import boopickle.CompositePickler
import boopickle.Default._

sealed trait Command
case class SendMessage(body: String) extends Command

object Command {
  implicit val commandPickler: CompositePickler[Command] = CompositePickler[Command]
    .addConcreteType[SendMessage]
}
