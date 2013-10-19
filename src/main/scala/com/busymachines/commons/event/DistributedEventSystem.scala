package com.busymachines.commons.event

import scala.collection.mutable.ListBuffer

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck

class DistributedEventBus(actorSystem: ActorSystem, topic: String = "all") extends EventBus {

  private val localSubscribers: ListBuffer[ActorRef] = 
    scala.collection.mutable.ListBuffer[ActorRef]()

  private val distributedSubscriber = actorSystem.actorOf(Props(classOf[DistributedSubscriber], topic, {
    event: BusEvent =>
      localSubscribers.map { 
        case actor => actor ! event 
      }
  }))

  private val publisher = 
    actorSystem.actorOf(Props(classOf[DistributedPublisher], topic))

  def subscribe(f: BusEvent => Any): Unit = 
    localSubscribers += actorSystem.actorOf(Props(classOf[DistributedEventBusEndpointActor], f))

  def publish(event: BusEvent):Unit = 
    publisher ! event
}

class DistributedSubscriber(topic: String, onReceiveCompletion: BusEvent => Any) extends Actor with ActorLogging {
  val mediator = DistributedPubSubExtension(context.system).mediator
  // subscribe to the topic  
  mediator ! Subscribe(topic, self)

  def receive = {
    case SubscribeAck(Subscribe(topic, `self`)) =>
      context become ready
  }

  def ready: Actor.Receive = {
    case e: BusEvent =>
      onReceiveCompletion(e.asInstanceOf[BusEvent])
  }
}

class DistributedPublisher(topic: String) extends Actor {
  // activate the extension
  val mediator = DistributedPubSubExtension(context.system).mediator

  def receive = {
    case e: BusEvent =>
      mediator ! Publish(topic, e)
  }
}

class DistributedEventBusEndpointActor(f:BusEvent => Any) extends Actor {
  def receive = {
    case event: BusEvent => f(event)
  }
}