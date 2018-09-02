package org.scalasapporo.gamecenter.client

import org.scalajs.dom
import org.scalasapporo.gamecenter.app.AppView

object Client {
  def main(args: Array[String]): Unit = {
    AppView.component.apply().renderIntoDOM(dom.document.getElementById("react-stage"))
  }
}
