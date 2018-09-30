package org.scalasapporo.gamecenter.connector

import scala.language.higherKinds

trait Connector[R[_]] {
  type Request <: ConnectorRequest
  type Response
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
  type Response = String
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

/*
trait HttpConnector[R[_]] extends Connector {
  import com.softwaremill.sttp._
  def uri: Uri
  override def execute(request: Request): Response = {
    sttp
      .body(serialize(request))
      .post(uri)
      .mapResponse(deserialize)
      .send()
  }
}
*/
