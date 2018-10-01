package org.scalasapporo.gamecenter.connector

import scala.language.higherKinds
import scala.concurrent.Future

trait Connector[R[_]] {
  type Request <: ConnectorRequest
  type Response = Request#Payload
  def execute(payload: Request#Payload)
    (implicit ctx: Request#Context, f: Request#Payload => Request): R[Response] = execute(f(payload))
  def execute(request: Request)(implicit ctx: Request#Context): R[Response]
}

object Connector {
  type Id[T] = T
}

trait ConnectorRequest {
  type Payload
  type Context
  def payload(implicit ctx: Context): Payload
}

trait ProcessConnector extends Connector[Connector.Id] {
  type Request = ProcessConnectorRequest
  def execute(request: ProcessConnectorRequest)(implicit ctx: ProcessConnectorRequest#Context): Connector.Id[Response] = {
    import scala.sys.process._
    Process(Seq(ctx.cmd, request.payload)).!!
  }
}
object ProcessConnector extends ProcessConnector

trait ProcessConnectorRequest extends ConnectorRequest {
  type Payload = String
  type Context = ProcessConnectorContext
}
object ProcessConnectorRequest {
  type R = ProcessConnectorRequest
  implicit val gen: String => R = (payload: String) => (_: R#Context) => payload
}
case class ProcessConnectorContext(cmd: String) extends AnyVal

trait HttpConnector extends Connector[Future] {
  import com.softwaremill.sttp._
  override def execute(request: HttpConnectorRequest)(implicit ctx: HttpConnectorContext): Future[Response] = {
    sttp
      .body(request.payload)
      .post(uri"${ctx.uri}")
      .send()
  }
}
object HttpConnector extends HttpConnector

trait HttpConnectorRequest extends ConnectorRequest {
  type Payload = Array[Byte]
  type Context = HttpConnectorContext
}
object HttpConnectorRequest {
  type R = HttpConnectorRequest
  implicit val gen: Array[Byte] => R = (payload: Array[Byte]) => (_: R#Context) => payload
}
case class HttpConnectorContext(uri: String) extends AnyVal
