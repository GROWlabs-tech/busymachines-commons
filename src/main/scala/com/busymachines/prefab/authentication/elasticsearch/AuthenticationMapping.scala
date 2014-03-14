package com.busymachines.prefab.authentication.elasticsearch

import com.busymachines.commons.elasticsearch.ESMapping
import com.busymachines.prefab.authentication.model.Authentication
import com.busymachines.prefab.authentication.model.SecurityJsonFormats._
import spray.json.JsObject
import scala.concurrent.duration._
import com.busymachines.commons.domain.Id

object AuthenticationMapping extends ESMapping[Authentication] {
  ttl = Some(7.days)
  val id = "_id" -> "id" :: String.as[Id[Authentication]] & NotAnalyzed
  val principal = "principal" :: String & NotAnalyzed
  val expirationTime = "expirationTime" :: Date
}