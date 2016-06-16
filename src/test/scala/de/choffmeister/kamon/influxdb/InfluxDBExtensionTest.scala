package de.choffmeister.kamon.influxdb

import java.lang.management.ManagementFactory

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import kamon.Kamon

class InfluxDBExtensionTest extends UnitTest {
  Kamon.start()
  val hostname = ManagementFactory.getRuntimeMXBean.getName.split('@')(1)

  "InfluxDBExtension" should "add host tag if not given explicitly" in {
    val system = ActorSystem("InfluxDBExtensionTest1")
    val influxDB = InfluxDB(system)

    influxDB.tags("host") should be(hostname)

    system.terminate()
  }

  it should "read tags from config" in {
    val config = ConfigFactory.parseString(
      """kamon.influxdb {
        |  tags = [
        |    { host: "foobar" }
        |    { apple: "pie" }
        |  ]
        |}
      """.stripMargin)
    val system = ActorSystem("InfluxDBExtensionTest2", config)
    val influxDB = InfluxDB(system)

    influxDB.tags("host") should be("foobar")
    influxDB.tags("apple") should be("pie")

    system.terminate()
  }
}
