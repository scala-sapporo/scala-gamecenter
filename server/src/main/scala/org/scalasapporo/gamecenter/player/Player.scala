package org.scalasapporo.gamecenter.player

sealed trait Player

case class AuthenticatedPlayer() extends Player
case object AnonymousPlayer