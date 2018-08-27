package org.scalasapporo.gamecenter.app

case class App(
    state: AppState,
    player: Player,
    game: Option[Game]
)

sealed trait AppState
case object SpacingOut extends AppState
case object WaitingGuest extends AppState
case object PlayingGame extends AppState

sealed trait AppAction
case object CreateRoom extends AppAction
case object RemoveRoom extends AppAction
case object JoinRoom extends AppAction
case object StartGame extends AppAction

// FIXME 暫定でここに置く
sealed trait Player
case object AnonymousePlayer extends Player

// FIXME 暫定でここに置く
trait Game