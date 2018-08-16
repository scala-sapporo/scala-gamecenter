package org.scalasapporo.gamecenter.shared.ws

import boopickle.CompositePickler
import boopickle.Default._

sealed trait Event
final case class UserEntered(nickname: String) extends Event
final case class UserLeft(nickname: String) extends Event
final case class MessageGotten(nickname: String, message: String) extends Event

object Event {
  implicit val eventPicker: CompositePickler[Event] = CompositePickler[Event]
    .addConcreteType[UserEntered]
    .addConcreteType[UserLeft]
    .addConcreteType[MessageGotten]
}
