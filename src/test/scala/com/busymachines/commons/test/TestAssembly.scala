package com.busymachines.commons.test

import akka.actor.ActorSystem
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import com.busymachines.commons.HasConfiguration
import com.busymachines.commons.dao.elasticsearch.EsConfiguration
import com.busymachines.commons.dao.elasticsearch.ESClient

object TestAssembly extends HasConfiguration {
  lazy implicit val actorSystem = ActorSystem("BusymachinesCommonsTest")
  lazy implicit val executionContext = actorSystem.dispatcher
  
  lazy val client: Client = new ESClient(new EsConfiguration)
  
  lazy val index = new TestESIndex(client)
  lazy val itemDao = new ItemDao(index)  
}