package org.scalasapporo.gamecenter.room

case class Room private(
  roomName: String,
  gameType: GameType,
  members: Seq[Member]
)
object Room {
  def apply(roomName: String, gameType: GameType, members: Seq[Member]): Room =
    new Room(roomName, gameType, members)
}

sealed trait Member
case class Owner() extends Member
case class Guest() extends Member

sealed trait GameType
case object GameTypeTetrics extends GameType
