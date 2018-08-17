package org.scalasapporo.gamecenter.room

case class Room private(members: Seq[Member])
object Room {
    def apply(members: Seq[Member]): Room = new Room(members)
}

sealed trait Member
case class Owner() extends Member
case class Guest() extends Member