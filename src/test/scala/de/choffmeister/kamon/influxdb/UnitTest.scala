package de.choffmeister.kamon.influxdb

import org.scalatest.{FlatSpec, Matchers, OptionValues}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

abstract class UnitTest extends FlatSpec with Matchers with OptionValues {
  private val timeout = 10.seconds

  def await[T](f: => Future[T]): T = Await.result(f, timeout)
}
