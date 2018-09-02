package org.scalasapporo.gamecenter.interface

import scala.scalajs.js

@js.native
trait Message[P <: js.Any] extends js.Object {
  val messageType: String = js.native
  val payload: P = js.native
}
