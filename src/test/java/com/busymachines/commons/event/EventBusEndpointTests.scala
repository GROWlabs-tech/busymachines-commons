package com.busymachines.commons.event

import akka.actor.ActorSystem
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EventBusEndpointTests extends FlatSpec  {
  "EventBusEndpoint" should "be able to accept multiple registrations & complete them all" in {

    var enpointReceived1 = false
    var enpointReceived2 = false

    val actorSystem = ActorSystem("aurum")
    val localEventBus = new LocalEventBus(actorSystem)

    localEventBus subscribe {
      case event: BusEvent =>
        enpointReceived1 = true
    }

    localEventBus subscribe {
      case event: BusEvent =>
        enpointReceived2 = true
    }

    localEventBus.publish(new BusEvent {})

    Thread.sleep(1000)
    
    assert(enpointReceived1)
    assert(enpointReceived2)
    
  }
}