package org.scalasapporo.gamecenter

import java.nio.ByteBuffer
import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, _}
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import boopickle.Default._
import com.typesafe.config.ConfigFactory
import org.scalasapporo.gamecenter.shared._
import org.scalasapporo.gamecenter.shared.ws._
import org.scalasapporo.gamecenter.ServerContext.{Closed, RegisterUser, Users}
import org.scalasapporo.gamecenter.shared.ws.{Event, MessageGotten, UserEntered, UserLeft}
import play.twirl.api.Html

import scala.concurrent.{Future, Promise}

object Server extends App with Directives {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val twirlHtmlMarshaller = twirlMarshaller[Html](`text/html`)
  val config = ConfigFactory.load()
  val context = system.actorOf(ServerContext.props(), "server-context")

  def wsFlow(uuid: String): Flow[Message, Message, Any] = {
    import Command.commandPickler
    import Event.eventPicker
    val sink = Flow[Message]
      .map {
        case message: TextMessage => (message, uuid)
        case message: BinaryMessage => (Unpickle[Command].fromBytes(message.getStrictData.asByteBuffer), uuid)
      }
      .to(Sink.actorRef(context, Closed(uuid)))
    val source = Source
      .actorRef[Event](16, OverflowStrategy.dropHead)
      .map {
        case MessageGotten(_, message) => TextMessage(message)
        case event => BinaryMessage(ByteString(Pickle.intoBytes[Event](event)))
      }
      // .map(event => BinaryMessage(ByteString(Pickle.intoBytes[Event](event))))
      .mapMaterializedValue(system.eventStream.subscribe(_, classOf[Event]))
    Flow.fromSinkAndSource(sink, source)
  }

  val route = pathSingleSlash {
    get {
      complete(view.html.index.render())
    }
  } ~
    pathPrefix("assets" / Remaining) { file =>
      encodeResponse {
        getFromResource("public/" + file)
      }
    } ~
    (post & path("api" / Segments)) { segments =>
      entity(as[ByteString]) { bs =>
        complete {
          AutowireServer.route[Api](new ApiImpl(context))(autowire.Core.Request(segments, Unpickle[Map[String, ByteBuffer]].fromBytes(bs.toByteBuffer)))
            .map(_.array())
        }
      }
    } ~
    path("ws" / Segments) {
      case uuid :: Nil =>
        handleWebSocketMessages(wsFlow(uuid))
      case _ => reject
    }

  Http().bindAndHandle(route, config.getString("app.server.interface"), config.getInt("app.server.port"))

  def twirlMarshaller[A](contentType: MediaType): ToEntityMarshaller[A] = {
    Marshaller.StringMarshaller.wrap(contentType)(_.toString)
  }
}

object AutowireServer extends autowire.Server[ByteBuffer, Pickler, Pickler] {
  override def read[R: Pickler](p: ByteBuffer): R = Unpickle[R].fromBytes(p)
  override def write[R: Pickler](r: R): ByteBuffer = Pickle.intoBytes(r)
}

class ServerContext extends Actor {
  var nicknames: Map[String, String] = Map.empty
  override def receive: Receive = {
    case RegisterUser(nickname, promise) =>
      val uuid = UUID.randomUUID().toString
      nicknames = nicknames.updated(uuid, nickname)
      publish(UserEntered(nickname))
      promise.success(uuid)
    case Users(promise) =>
      promise.success(nicknames.values.toSeq)
    case (command: Command, uuid: String) => executeCommand(command, uuid)
    case Closed(uuid) =>
      nicknames
        .get(uuid)
        .foreach { nickname =>
          publish(UserLeft(nickname))
          nicknames = nicknames - uuid
        }
    case (textMessage: TextMessage, id: String) => publish(MessageGotten(id, s"on text message: ${textMessage.getStrictText}"))
    case other => println(s"Unknown message: $other")
  }
  private def executeCommand(command: Command, uuid: String): Unit = command match {
    case SendMessage(body) =>
      nicknames
        .get(uuid)
        .foreach { nickname =>
          publish(MessageGotten(nickname, body))
        }
  }
  private def publish(event: Event): Unit = {
    context.system.eventStream.publish(event)
  }
}
object ServerContext {
  def props(): Props = Props(classOf[ServerContext])
  case class User(nickname: String, uuid: String)
  case class RegisterUser(nickname: String, promise: Promise[String])
  case class Users(promise: Promise[Seq[String]])
  case class Closed(uuid: String)
}

class ApiImpl(context: ActorRef) extends Api {
  override def echo(message: String): String = message
  override def register(nickname: String): Future[String] = {
    val promise = Promise[String]
    context ! RegisterUser(nickname, promise)
    promise.future
  }
  override def users(): Future[Seq[String]] = {
    val promise = Promise[Seq[String]]
    context ! Users(promise)
    promise.future
  }
}