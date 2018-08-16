package org.scalasapporo.gamecenter.client

import java.nio.ByteBuffer

import autowire._
import boopickle.Default._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.vdom.html_<^._
import monocle.Lens
import monocle.macros.GenLens
import org.scalasapporo.gamecenter.shared.ws.MessageGotten
import org.scalajs.dom
import org.scalajs.dom.{MessageEvent, html}
import org.scalajs.dom.experimental.{Notification, NotificationOptions}
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{CloseEvent, Event, WebSocket}
import org.scalasapporo.gamecenter.client.RootModel._
import org.scalasapporo.gamecenter.shared._
import org.scalasapporo.gamecenter.shared.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

object Client {
  def main(args: Array[String]): Unit = {
    RootView().renderIntoDOM(dom.document.getElementById("react-stage"))
  }
}

@JSExport
object Notifier {
  val Granted = "granted"
  private var useNotification: Boolean = false
  private var timeout: Int = 1000
  def granted: Boolean = Notification.permission == Granted
  def usable: Boolean = !js.isUndefined(Notification) && granted && useNotification
  @JSExport
  def use(timeout: Int): Unit = {
    if (granted) {
      Notification.requestPermission { result: String =>
        if (result == Granted) {
          useNotification = true
          this.timeout = timeout
        }
      }
    }
  }
  def emmit(title: String, body: String): Unit = {
    if (usable) {
      val notification = new Notification(title, NotificationOptions(body = body))
      dom.window.setTimeout(() => notification.close(), timeout)
    }
  }
}

case class RootModel(wsMessageBox: MessageBox, apiMessageBox: MessageBox, wsState: WebSocketState = NotYet)
object RootModel {
  sealed trait WebSocketState
  sealed trait NoConnection extends WebSocketState
  case object Registering extends WebSocketState
  final case class Connecting(ws: WebSocket) extends WebSocketState
  final case class Connected(ws: WebSocket) extends WebSocketState
  case object NotYet extends NoConnection
  case object Closed extends NoConnection
}
case class Message(body: String)
case class MessageBox(messages: List[Message], limit: Int = 100) {
  def lines(limit: Int): String = messages.take(limit).map(_.body).mkString("\n")
  def add(message: String): MessageBox = copy((Message(message) :: messages).take(limit))
}
object MessageBox {
  def empty = MessageBox(Nil)
}

object AppContext {
  val wsMessageBox: Lens[RootModel, MessageBox] = GenLens[RootModel](_.wsMessageBox)
  val apiMessageBox: Lens[RootModel, MessageBox] = GenLens[RootModel](_.apiMessageBox)
  val wsState: Lens[RootModel, WebSocketState] = GenLens[RootModel](_.wsState)
  def addWsMessage(model: RootModel, message: String): RootModel = wsMessageBox.modify(_.add(message))(model)
  def addApiMessage(model: RootModel, message: String): RootModel = apiMessageBox.modify(_.add(message))(model)
  def registering(model: RootModel, nickname: String): RootModel = wsState.set(Registering)(model)
  def connecting(model: RootModel, ws: WebSocket): RootModel = wsState.set(Connecting(ws))(model)
  def connected(model: RootModel, ws: WebSocket): RootModel = addWsMessage(wsState.set(Connected(ws))(model), "接続しました")
  def closed(model: RootModel): RootModel = addWsMessage(wsState.set(Closed)(model), "接続が切れました")
}

object RootView {
  class Backend(bs: BackendScope[Unit, RootModel]) {
    private val apiClient = AutowireClient[Api]
    private val nicknameRef = Ref[html.Element]
    private val nicknamePattern = "^(.{1,10})$".r
    private val messageRef = Ref[html.Element]
    def render(m: RootModel): VdomElement =
      <.div(
        ^.className := "container",
        <.div(
          ^.className := "row",
          <.p("Scala-Gamecenter")
        ),
        <.div(
          ^.className := "form-group",
          <.label("WebSocket"),
          MessageBoxView(m.wsMessageBox),
          m.wsState match {
            case _: NoConnection =>
              <.div(
                ^.className := "form-inline",
                <.input(
                  ^.`type` := "text",
                  ^.className := "form-control",
                  ^.placeholder := "Nickname (1～10文字)",
                  ^.onKeyUp ==> { e: ReactKeyboardEvent =>
                    if (e.keyCode == KeyCode.Enter) register()
                    else Callback.empty
                  }
                ).withRef(nicknameRef),
                <.button(
                  ^.className := "btn btn-default",
                  ^.onClick --> register(),
                  "Connect WebSocket"
                )
              )
            case Registering => "connecting..."
            case _: Connecting => "connecting..."
            case _: Connected =>
              <.div(
                ^.className := "form-inline",
                <.input(
                  ^.`type` := "text",
                  ^.className := "form-control",
                  ^.placeholder := "message",
                  ^.onKeyUp ==> { e: ReactKeyboardEvent =>
                    if (e.keyCode == KeyCode.Enter) send()
                    else Callback.empty
                  }
                ).withRef(messageRef),
                <.button(
                  ^.className := "btn btn-default",
                  ^.onClick --> send(),
                  "Send"
                )
              )
          }
        ),
        <.div(
          ^.className := "form-group",
          <.label("WebAPI"),
          MessageBoxView(m.apiMessageBox),
          <.div(
            ^.className := "btn-group",
            <.button(
              ^.className := "btn btn-default",
              ^.onClick --> echo,
              "Echo"
            ),
            <.button(
              ^.className := "btn btn-default",
              ^.onClick --> users,
              "Users"
            )
          )
        )
      )
    def init: Callback = nicknameRef.foreach(_.asInstanceOf[Input].focus())
    def connect(uuid: String): Unit = {
      val ws = new WebSocket(s"ws://${dom.window.location.host}/ws/$uuid")
      ws.binaryType = "arraybuffer"
      ws.onopen = { _: Event =>
        if (Notifier.usable) {
          dom.window.setInterval(() => ws.send(""), 2000)
        }
        (bs.modState(AppContext.connected(_, ws)) >> messageRef.foreach(_.asInstanceOf[Input].focus())).runNow()
      }
      ws.onmessage = (e: MessageEvent) => {
        import org.scalasapporo.gamecenter.shared.ws.{Event => WsEvent}
        import WsEvent.eventPicker
        Unpickle[WsEvent].fromBytes(TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])) match {
          case MessageGotten(nickname, message) =>
            val body = s"$nickname : $message"
            Notifier.emmit("Chat", body)
            bs.modState(AppContext.addWsMessage(_, body)).runNow()
          case UserEntered(nickname) =>
            bs.modState(AppContext.addWsMessage(_, s"${nickname}さんが入室しました")).runNow()
          case UserLeft(nickname) =>
            bs.modState(AppContext.addWsMessage(_, s"${nickname}さんが退室しました")).runNow()
        }
      }
      ws.onclose = { _: CloseEvent =>
        (bs.modState(AppContext.closed _) >> nicknameRef.foreach(_.asInstanceOf[Input].focus())).runNow()
      }
    }
    def register(): Callback = nicknameRef.foreachCB(_.asInstanceOf[Input].value match {
      case nicknamePattern(nickname) =>
        apiClient.register(nickname)
          .call()
          .foreach(connect)
        bs.modState(AppContext.registering(_, nickname))
      case _ => Callback.alert("Invalid nickname!")
    })
    def send(): Callback = bs.state.flatMap {
      _.wsState match {
        case Connected(ws) =>
          messageRef.get.map { i =>
            import Command.commandPickler
            ws.send(Pickle.intoBytes[Command](SendMessage(i.asInstanceOf[Input].value)).arrayBuffer())
            i.asInstanceOf[Input].value = ""
          }
        case _ => Callback.empty
      }
    }
    def echo: Callback = Callback.future {
      apiClient.echo("Hello Scala-JS")
        .call()
        .map(message => bs.modState(AppContext.addApiMessage(_, message)))
    }
    def users: Callback = Callback.future {
      apiClient.users()
        .call()
        .map(users => bs.modState(AppContext.addApiMessage(_, users.mkString(","))))
    }
  }
  private val component = ScalaComponent.builder[Unit]("RootView")
    .initialState(RootModel(MessageBox.empty, MessageBox.empty))
    .renderBackend[Backend]
    .componentWillMount(_.backend.init)
    .build
  def apply(): Scala.Unmounted[Unit, RootModel, RootView.Backend] = component()
}

object MessageBoxView {
  case class Props(messageBox: MessageBox, rowCount: Int, dataCount: Int)
  private val component = ScalaComponent.builder[Props]("MessageBoxView")
    .render_P(props =>
      <.textarea(
        ^.rows := props.rowCount,
        ^.className := "form-control",
        ^.value := props.messageBox.lines(props.dataCount)
      )
    )
    .build
  def apply(messageBox: MessageBox, rowCount: Int = 10, dataCount: Int = 10): Scala.Unmounted[Props, _, _] = component(Props(messageBox, rowCount, dataCount))
}

object AutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
  override def doCall(req: Request): Future[ByteBuffer] = {
    dom.ext.Ajax.post(
      url = "/api/" + req.path.mkString("/"),
      data = Pickle.intoBytes(req.args),
      responseType = "arraybuffer",
      headers = Map("Content-Type" -> "application/octet-stream")
    ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
  }
  override def read[Result: Pickler](p: ByteBuffer): Result = Unpickle[Result].fromBytes(p)
  override def write[Result: Pickler](r: Result): ByteBuffer = Pickle.intoBytes(r)
}
