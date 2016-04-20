package de.choffmeister.kamon.influxdb

import akka.actor.{ActorRef, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.event.Logging
import com.typesafe.config.Config
import kamon.Kamon
import kamon.metric.TickMetricSnapshotBuffer
import kamon.util.ConfigTools._

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

object InfluxDB extends ExtensionId[InfluxDBExtension] with ExtensionIdProvider {
  override def lookup(): ExtensionId[_ <: Extension] = InfluxDB
  override def createExtension(system: ExtendedActorSystem): InfluxDBExtension = new InfluxDBExtension(system)
}

class InfluxDBExtension(system: ExtendedActorSystem) extends Kamon.Extension {
  implicit val as = system

  val log = Logging(system, classOf[InfluxDBExtension])
  log.info("Starting the Kamon(InfluxDB) extension")

  private val config = system.settings.config
  private val influxDBConfig = config.getConfig("kamon.influxdb")
  val metricsExtension = Kamon.metrics

  val tickInterval = metricsExtension.settings.tickInterval
  val flushInterval = influxDBConfig.getFiniteDuration("flush-interval")
  val maxPacketSizeInBytes = influxDBConfig.getBytes("max-packet-size")

  val statsDMetricsListener = buildMetricsListener(tickInterval, flushInterval, config)

  val subscriptions = influxDBConfig.getConfig("subscriptions")
  subscriptions.firstLevelKeys.foreach { subscriptionCategory =>
    subscriptions.getStringList(subscriptionCategory).asScala.foreach { pattern =>
      metricsExtension.subscribe(subscriptionCategory, pattern, statsDMetricsListener, permanently = true)
    }
  }

  def buildMetricsListener(tickInterval: FiniteDuration, flushInterval: FiniteDuration, config: Config): ActorRef = {
    assert(flushInterval >= tickInterval, "InfluxDB flush-interval needs to be equal or greater to the tick-interval")

    val metricsSender = system.actorOf(InfluxDBMetricsSender.props(
      influxDBConfig.getString("hostname"),
      influxDBConfig.getInt("port"),
      maxPacketSizeInBytes), "influxdb-metrics-sender")

    if (flushInterval == tickInterval) {
      // No need to buffer the metrics, let's go straight to the metrics sender.
      metricsSender
    } else {
      system.actorOf(TickMetricSnapshotBuffer.props(flushInterval, metricsSender), "statsd-metrics-buffer")
    }
  }
}
