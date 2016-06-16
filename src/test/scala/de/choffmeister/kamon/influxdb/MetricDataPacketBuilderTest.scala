package de.choffmeister.kamon.influxdb

import java.net.InetSocketAddress

import akka.io.Udp.{NoAck, Send}
import akka.util.ByteString

import scala.concurrent.duration._

class MetricDataPacketBuilderTest extends UnitTest with TestActorSystem {
  val nowNanos = (System.currentTimeMillis() / 1000L) * 1000000000L

  "MetricDataPacketBuilder" should "properly flush measurements as UDP packets" in {
    val remote = new InetSocketAddress("localhost", 1234)
    val builder = new MetricDataPacketBuilder(64L, self, remote)

    builder.appendMeasurement("test1", Map.empty, Map("value" -> 1L), nowNanos)
    builder.appendMeasurement("test2", Map.empty, Map("value" -> 2L), nowNanos)
    builder.appendMeasurement("test3", Map.empty, Map("value" -> 3L), nowNanos)

    expectMsg(100.millis, Send(ByteString(s"test1, value=1 $nowNanos\n"), remote, NoAck))
    expectMsg(100.millis, Send(ByteString(s"test2, value=2 $nowNanos\n"), remote, NoAck))
    expectNoMsg(100.millis)

    builder.appendMeasurement("test4", Map.empty, Map("value" -> 4L), nowNanos)

    expectMsg(100.millis, Send(ByteString(s"test3, value=3 $nowNanos\n"), remote, NoAck))
  }
}
