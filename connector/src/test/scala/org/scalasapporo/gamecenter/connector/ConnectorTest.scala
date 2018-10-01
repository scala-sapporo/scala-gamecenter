package org.scalasapporo.gamecenter.connector

import utest._

object ConnectorTest extends TestSuite {
  val tests = Tests {
    "http" - {
      import com.softwaremill.sttp.testing.SttpBackendStub
      import scala.concurrent.ExecutionContext.Implicits.global
      val uri = "http://example.com/"
      implicit val backend = SttpBackendStub.asynchronousFuture
        .whenRequestMatches(_.uri.toString() == uri)
        .thenRespond("Success")
      implicit val ctx = HttpConnectorContext(uri)
      HttpConnector.execute("test".getBytes)
        .map(bytes => new String(bytes))
        .foreach(println)
    }
  }
}
