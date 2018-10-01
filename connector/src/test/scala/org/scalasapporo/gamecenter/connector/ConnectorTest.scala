package org.scalasapporo.gamecenter.connector

import utest._

object ConnectorTest extends TestSuite {
  val tests = Tests {
    "http" - {
      import com.softwaremill.sttp._
      // implicit val backend = SttpBackendStub.synchronous
      implicit val backend = SttpBackendStub.asynchronous
      implicit val ctx = HttpConnectorContext("http://example.com/")
      HttpConnector.execute("test".getBytes)
    }
  }
}
