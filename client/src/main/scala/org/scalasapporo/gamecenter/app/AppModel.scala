package org.scalasapporo.gamecenter.app

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import dom.raw.{HTMLIFrameElement, HTMLSelectElement}

import scala.scalajs.js.JSConverters._

case class AppModel(
  state: AppState,
  player: Player
)
object AppModel {
  def select(): AppModel = AppModel(SelectingGame, AnonymousPlayer)
  def play(game: Game): AppModel = AppModel(PlayingGame(game), AnonymousPlayer)
}

sealed trait AppState
case object SelectingGame extends AppState
case object SpacingOut extends AppState
case object WaitingGuest extends AppState
case class PlayingGame(game: Game) extends AppState

sealed trait AppAction
case object CreateRoom extends AppAction
case object RemoveRoom extends AppAction
case object JoinRoom extends AppAction
case object StartGame extends AppAction

// FIXME 暫定でここに置く
sealed trait Player
case object AnonymousPlayer extends Player

// FIXME 暫定でここに置く
trait Game {
  val setting: GameSetting
}
object Game {
  val all = Seq(GameTenTen, GameTenTenMatch)
}
case object GameTenTen extends Game {
  override val setting: GameSetting = GameSetting("TenTen", "4フィールド中2フィールドで10ライン消せばクリア", 520, 520)
}
case object GameTenTenMatch extends Game {
  override val setting: GameSetting = GameSetting("TenTen(Match)", "対戦型TenTen(実験中)", 520, 520, isMatch = true)
}
case class GameSetting(name: String, description: String, width: Int, height: Int, isMatch: Boolean = false)

sealed trait AppStateHandler {
  def setState(state: AppState): Callback
}

object AppView {
  private val gameSelectorView = GameSelectorView.component
  private val gameView = GameView.component
  private[app] class Backend(bs: BackendScope[Unit, AppModel]) extends AppStateHandler {
    override def setState(state: AppState): Callback = {
      bs.modState(_.copy(state = state))
    }
    def render(s: AppModel): VdomElement = {
      <.div(
        s.state match {
          case PlayingGame(game) => gameView(GameView.props(game))
          case _ => gameSelectorView(GameSelectorView.props(Game.all, this))
        }
      )
    }
  }
  private[gamecenter] val component = ScalaComponent.builder[Unit]("AppView")
    .initialState(AppModel.select())
    .renderBackend[Backend]
    .build
}
object HeaderView {
  case class Props private(player: Player)
  def props(player: Player) = Props(player)
  private[gamecenter] val component = ScalaComponent.builder[Props]("HeaderView")
    .render_P { p =>
      <.div()
    }.build
}
object GameSelectorView {
  case class State(game: Option[Game] = None)
  case class Props private(games: Seq[Game], appStateHandler: AppStateHandler)
  private val gameSelectRef = Ref[HTMLSelectElement]
  def props(games: Seq[Game], appStateHandler: AppStateHandler) = Props(games, appStateHandler)
  class Backend(bs: BackendScope[Props, State]) {
    private def onChange() = gameSelectRef.foreachCB { s =>
      bs.props.flatMap { p =>
        bs.modState(_.copy(Some(p.games(s.selectedIndex))))
      }
    }
    def init = onChange()
    def render(p: Props, s: State) = {
      <.div(
        <.select(
          ^.onChange --> onChange(),
          p.games.map(game => <.option(game.setting.name)).toTagMod
        ).withRef(gameSelectRef),
        <.button(
          ^.onClick --> gameSelectRef.foreachCB(s => p.appStateHandler.setState(PlayingGame(p.games(s.selectedIndex)))),
          "Start"
        ),
        s.game.map { g =>
          <.div(g.setting.description)
        }
      )
    }
  }
  private[gamecenter] val component = ScalaComponent.builder[Props]("GameSelectorView")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount(_.backend.init)
    .build
}
object GameView {
  case class Props private(game: Game)
  def props(game: Game) = Props(game)
  private[app] class Backend(bs: BackendScope[Props, Unit]) {
    private val iframeRef = Ref[HTMLIFrameElement]
    def init: Callback = {
      iframeRef.foreach { frm =>
        frm.onload = _ => {
          frm.focus()
          frm.contentWindow.postMessage("start", "https://lab.yuiwai.com")
        }
      }
    }
    def render(): VdomElement = {
      <.div(
        <.iframe(
          ^.src := "index.html",
          ^.style := Map(
            "width" -> "520px",
            "height" -> "520px",
            "border" -> "0"
          ).toJSDictionary
        ).withRef(iframeRef)
      )
    }
  }
  private[gamecenter] val component = ScalaComponent.builder[Props]("AppView")
    .renderBackend[Backend]
    .componentDidMount(_.backend.init)
    .build
}