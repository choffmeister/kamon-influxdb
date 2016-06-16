package de.choffmeister.kamon.influxdb

import com.typesafe.config._

import scala.collection.JavaConverters._

class RichConfig(val config: Config) extends AnyVal {
  def getStringMap(path: String): Map[String, String] = if (config.hasPath(path)) {
    val list: Iterable[ConfigObject] = config.getObjectList(path).asScala
    (for {
      item <- list
      entry <- item.entrySet().asScala
      key = entry.getKey
      value = entry.getValue.unwrapped().toString
    } yield (key, value)).toMap
  } else {
    Map.empty
  }
}
