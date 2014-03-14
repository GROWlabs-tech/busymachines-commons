package com.busymachines.commons.logging

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import com.busymachines.commons.elasticsearch.ESConfig
import com.busymachines.commons.elasticsearch.ESClient
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import com.busymachines.commons.domain.Id
import com.busymachines.commons.domain.HasId
import com.busymachines.commons.domain.CommonJsonFormats
import com.busymachines.commons.elasticsearch.ESMapping
import com.busymachines.commons.elasticsearch.ESRootDao
import com.busymachines.commons.elasticsearch.ESType
import scala.concurrent.ExecutionContext.Implicits.global
import com.busymachines.commons.elasticsearch.ESIndex
import com.busymachines.commons.event.DoNothingEventSystem

object DomainJsonFormats extends CommonJsonFormats {
  implicit val logMessageFormat = format9(LogMessage)
}
import DomainJsonFormats._

case class LogMessage(
  id: Id[LogMessage] = Id.generate[LogMessage],
  message: String,
  fields: Option[String] = None,
  source: String,
  sourceHost: String,
  sourcePath: String,
  tags: String,
  `type`: String,
  timestamp: DateTime) extends HasId[LogMessage]

object LogMessageMapping extends ESMapping[LogMessage] {
  val id = "_id" -> "id" :: String.as[Id[LogMessage]] & NotAnalyzed
  val message = "@message" :: String & Analyzed & IncludeInAll
  val fields = "@fields" :: Object & NotAnalyzed & IncludeInAll
  val source = "@source" :: String & NotAnalyzed & IncludeInAll
  val sourceHost = "@source_host" :: String & NotAnalyzed & IncludeInAll
  val sourcePath = "@source_path" :: String & NotAnalyzed & IncludeInAll
  val tags = "@tags" :: String & NotAnalyzed & IncludeInAll
  val `type` = "@type" :: String & NotAnalyzed & IncludeInAll
  val timestamp = "@timestamp" :: Date & IncludeInAll
}

object LogstashAppender extends App {

  val fmt = DateTimeFormat.forPattern("'logstash-'yyyy.MM.d")
  val logstashIndexName = fmt.print(DateTime.now)

  lazy val esConfig = new ESConfig("loggger.db.elasticsearch") {
    def indexName = logstashIndexName
  }
  lazy val esClient = new ESClient(esConfig)
  lazy val esIndex = new ESIndex(esClient, esConfig.indexName, new DoNothingEventSystem)

  val dao = new ESRootDao[LogMessage](esIndex, ESType("log", LogMessageMapping))
  for (i <- 1 to 1000) {
    val ex = new Exception("Exeption message" + i)
    Await.result(dao.create(LogMessage(message = ex.getMessage, source = LogHelper.stackTracesToString(ex.getStackTrace), sourceHost = java.net.InetAddress.getLocalHost.getHostName, sourcePath = "", tags = "", `type` = "ERROR", timestamp = DateTime.now)), 30 seconds)
  }

}