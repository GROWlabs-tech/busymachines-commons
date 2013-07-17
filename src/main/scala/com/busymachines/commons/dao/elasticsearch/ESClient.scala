package com.busymachines.commons.dao.elasticsearch

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress

class ESClient(configuration: EsConfiguration) extends TransportClient(ImmutableSettings.settingsBuilder().put("cluster.name", configuration.clusterName)) {
  addTransportAddresses((for (esHostName <- configuration.esHostNames) yield new InetSocketTransportAddress(esHostName, configuration.esPort)): _*)
} 
