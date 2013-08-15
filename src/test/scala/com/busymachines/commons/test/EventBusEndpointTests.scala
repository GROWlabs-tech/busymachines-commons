package com.busymachines.commons.test

import org.scalatest.FlatSpec
import akka.actor.ActorSystem
import com.busymachines.commons.event.LocalEventBus
import com.busymachines.commons.event.BusEvent

class EventBusEndpointTests extends FlatSpec  {
  "EventBusEndpoint" should "be able to accept multiple registrations & complete them all" in {

    var enpointReceived1 = false
    var enpointReceived2 = false

    val actorSystem = ActorSystem("aurum")
    val localEventBus = new LocalEventBus(actorSystem)

    localEventBus subscribe {
      event: BusEvent =>
        enpointReceived1 = true
    }

    localEventBus subscribe {
      event: BusEvent =>
        enpointReceived2 = true
    }

    localEventBus.publish(new BusEvent {})

    Thread.sleep(1000)
    
    assert(enpointReceived1)
    assert(enpointReceived2)
    
  }
}