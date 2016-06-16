package de.choffmeister.kamon.influxdb

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKitBase}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait TestActorSystem extends Suite with BeforeAndAfterAll with TestKitBase with ImplicitSender {
  implicit lazy val system: ActorSystem = ActorSystem()

  override def afterAll(): Unit = {
    shutdown(system)
    super.afterAll()
  }
}
