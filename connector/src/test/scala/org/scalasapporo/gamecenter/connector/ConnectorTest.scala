package org.scalasapporo.gamecenter.connector

import utest._

class ConnectorTest extends TestSuite {
  val tests = Tests {

    /*
    "http" - {
      import com.softwaremill.sttp._
      implicit val backend = SttpBackendStub.synchronous
      val connector = new HttpConnector[Id] with Base64Serializer {
        override def uri: Uri = uri"http://example.com/"
      }
      connector.execute("test".getBytes)
    }
    */
  }
}
