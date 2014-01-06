package com.busymachines.prefab.party

import scala.concurrent.ExecutionContext
import com.busymachines.commons.elasticsearch.ESIndex
import com.busymachines.prefab.authentication.elasticsearch.ESAuthenticationDao
import com.busymachines.prefab.authentication.elasticsearch.ESCredentialsDao
import com.busymachines.prefab.authentication.logic.AuthenticationConfig
import com.busymachines.prefab.party.api.v1.AuthenticationApiV1
import com.busymachines.prefab.party.api.v1.PartiesApiV1
import com.busymachines.prefab.party.api.v1.UsersApiV1
import com.busymachines.prefab.party.db.PartyDao
import com.busymachines.prefab.party.db.UserDao
import com.busymachines.prefab.party.logic.PartyManager
import com.busymachines.prefab.party.logic.UserAuthenticator
import akka.actor.ActorSystem
import com.busymachines.commons.elasticsearch.ESSequenceDao
import com.busymachines.prefab.party.logic.PartyFixture
import com.busymachines.prefab.party.logic.PartyCache

trait PartyAssembly {

  // dependencies
  implicit def actorSystem : ActorSystem
  implicit def executionContext : ExecutionContext
  def index : ESIndex
  
  // default configuration
  def partyIndex = index
  def authenticationIndex = index
  def sequenceIndex = index
  def authenticationConfigBaseName = "authentication"
  def authenticationConfig = new AuthenticationConfig(authenticationConfigBaseName)

  // components
  lazy val sequenceDao = new ESSequenceDao(sequenceIndex)
  lazy val partyDao = new PartyDao(partyIndex)
  lazy val userDao = new UserDao(partyDao)
  lazy val credentialsDao = new ESCredentialsDao(partyIndex)
  lazy val authenticationDao = new ESAuthenticationDao(authenticationIndex)
  lazy val userAuthenticator = new UserAuthenticator(authenticationConfig, partyDao, credentialsDao, authenticationDao)
  lazy val partyCache = new PartyCache(partyDao, userAuthenticator)
  lazy val partyManager = new PartyManager(partyDao, userDao, credentialsDao, userAuthenticator)
  lazy val authenticationApiV1 = new AuthenticationApiV1(userAuthenticator)
  lazy val usersApiV1 = new UsersApiV1(partyManager, userAuthenticator)
  lazy val partiesApiV1 = new PartiesApiV1(partyManager, userAuthenticator)
  
  // services
  def createPartyFixture() = PartyFixture.create(partyDao, credentialsDao)
}