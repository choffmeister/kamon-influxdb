package de.choffmeister.kamon.influxdb

import java.net.InetSocketAddress
import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.util.Locale

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import kamon.metric.SubscriptionsDispatcher.TickMetricSnapshot
import kamon.metric.instrument.{Counter, Histogram}

class InfluxDBMetricsSender(influxDBAddress: InetSocketAddress, maxPacketSizeInBytes: Long, tags: Map[String, String]) extends Actor with UdpExtensionProvider {
  import context.system

  def now = (System.currentTimeMillis() / 1000L) * 1000000000L

  def escape(s: String) = s.flatMap {
    case '=' => '\\' :: '=' :: Nil
    case ',' => '\\' :: ',' :: Nil
    case c => c :: Nil
  }.mkString

  udpExtension ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady =>
      context.become(ready(sender))
  }

  def ready(udpSender: ActorRef): Receive = {
    case tick: TickMetricSnapshot => writeMetricsToRemote(tick, udpSender)
  }

  def writeMetricsToRemote(tick: TickMetricSnapshot, udpSender: ActorRef): Unit = {
    val packetBuilder = new MetricDataPacketBuilder(maxPacketSizeInBytes, udpSender, influxDBAddress)

    for (
      (entity, snapshot) <- tick.metrics;
      (metricKey, metricSnapshot) <- snapshot.metrics
    ) {
      val timestamp = now

      val baseTags = tags ++ entity.tags.map { case (key, value) => escape(key) -> value }
      val (baseName, customTags: Map[String, String]) = entity.category match {
        case "counter" | "histogram" =>
          (escape(entity.name), Map.empty)
        case "akka-actor" | "akka-router" =>
          val actorNameParts = escape(entity.name).split("/", 3).toSeq
          (escape(entity.category) + "." + escape(metricKey.name), Map(
            "actor-system" -> actorNameParts.head,
            "actor-guardian" -> actorNameParts(1),
            "actor-name" -> actorNameParts(2)))
        case _ =>
          (escape(entity.category) + "." + escape(entity.name) + "." + escape(metricKey.name), Map.empty)
      }

      metricSnapshot match {
        case hs: Histogram.Snapshot =>
          hs.recordsIterator.foreach { record =>
            packetBuilder.appendMeasurement(
              baseName + ".histogram",
              baseTags ++ customTags,
              Map("value" -> record.level, "count" -> record.count),
              timestamp)
          }

        case cs: Counter.Snapshot =>
          packetBuilder.appendMeasurement(
            baseName + ".counter",
            baseTags ++ customTags,
            Map("count" -> cs.count),
            timestamp)
      }
    }

    packetBuilder.flush()
  }
}

object InfluxDBMetricsSender {
  def props(influxDBAddress: InetSocketAddress, maxPacketSize: Long, tags: Map[String, String]): Props =
    Props(new InfluxDBMetricsSender(influxDBAddress, maxPacketSize, tags))
}

trait UdpExtensionProvider {
  def udpExtension(implicit system: ActorSystem): ActorRef = IO(Udp)
}

object MetricDataPacketBuilder {
  private val symbols = DecimalFormatSymbols.getInstance(Locale.US)
  // Just in case there is some weird locale config we are not aware of
  symbols.setDecimalSeparator('.')

  // Absurdly high number of decimal digits, let the other end lose precision if it needs to
  val samplingRateFormat = new DecimalFormat("#.################################################################", symbols)

  val measurementSeparator = "\n"
}

class MetricDataPacketBuilder(maxPacketSizeInBytes: Long, udpSender: ActorRef, remote: InetSocketAddress) {
  import MetricDataPacketBuilder._

  var buffer = new StringBuilder()

  def appendMeasurement(name: String, tags: Map[String, String], values: Map[String, Long], timestamp: Long): Unit = {
    val tagsStr = tags.map { case (key, value) => key + "=" + value } mkString ","
    val valuesStr = values.map { case (key, value) => key + "=" + value.toString } mkString ","
    val line = name + "," + tagsStr + " " + valuesStr + " " + timestamp.toString + measurementSeparator

    if (fitsOnBuffer(line)) {
      buffer.append(line)
    } else {
      flushToUDP(buffer.toString())
      buffer.clear()
      buffer.append(line)
    }
  }

  def fitsOnBuffer(data: String): Boolean = (buffer.length + data.length) <= maxPacketSizeInBytes

  private def flushToUDP(data: String): Unit = udpSender ! Udp.Send(ByteString(data), remote)

  def flush(): Unit = {
    flushToUDP(buffer.toString)
    buffer.clear()
  }
}
