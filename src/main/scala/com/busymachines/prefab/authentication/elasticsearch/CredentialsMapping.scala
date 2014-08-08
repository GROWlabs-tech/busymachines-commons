package com.busymachines.prefab.authentication.elasticsearch

import com.busymachines.commons.Implicits._
import com.busymachines.commons.elasticsearch.ESMapping
import com.busymachines.prefab.authentication.model.Credentials
import com.busymachines.prefab.authentication.model.PasswordCredentials
import com.busymachines.prefab.authentication.Implicits._
import com.busymachines.commons.domain.Id
import com.busymachines.prefab.authentication.model.PasswordHint

object CredentialsMapping extends ESMapping[Credentials] {
  val id = "_id" -> "id" :: String.as[Id[Credentials]]
  val passwordCredentials = "passwordCredentials" :: Nested(PasswordCredentialsMapping)
  val passwordHints = "passwordHints" :: Nested(PasswordHintMapping)
}

object PasswordCredentialsMapping extends ESMapping[PasswordCredentials] {
  val login = "login" :: String
  val salt = "salt" :: String
  val passwordHash = "passwordHash" :: String
}

object PasswordHintMapping extends ESMapping[PasswordHint] {
  val securityQuestion = "securityQuestion" :: String & NotAnalyzed
  val securityAnswer = "securityAnswer" :: String & NotAnalyzed
}
