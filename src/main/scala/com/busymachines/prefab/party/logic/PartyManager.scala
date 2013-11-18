package com.busymachines.prefab.party.logic

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import com.busymachines.commons.domain.Id
import com.busymachines.commons.dao.Versioned
import com.busymachines.commons.elasticsearch.implicits._
import spray.caching.LruCache
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.busymachines.prefab.authentication.elasticsearch.ESCredentialsDao
import com.busymachines.commons.Logging
import com.busymachines.prefab.authentication.model.Credentials
import com.busymachines.prefab.authentication.model.PasswordCredentials
import com.busymachines.prefab.party.domain.Party
import com.busymachines.prefab.party.db.PartyDao
import com.busymachines.prefab.party.domain.PartyDomainJsonFormats._
import com.busymachines.prefab.party.db.UserDao
import com.busymachines.prefab.party.service.SecurityContext
import com.busymachines.prefab.party.domain.User
import com.busymachines.prefab.party.service.PartyService

class PartyManager(partyDao: PartyDao, userDao : UserDao, credentialsDao : ESCredentialsDao, userAuthenticator : UserAuthenticator)(implicit ec: ExecutionContext) extends PartyService with Logging {

  private val partyCache = LruCache[Option[Party]](2000, 50, 7 days, 8 hours)

  def setLoginNamePassword(userId: Id[User], loginName : String, password: String): Future[Credentials] =
    userDao.retrieve(userId) flatMap {
      case None => throw new Exception(s"Non existent user with id $userId")
      case Some(user) =>
        credentialsDao.getOrCreateAndModify(user.credentials)(Credentials(user.credentials)) {
          credentials =>
          credentials.copy(passwordCredentials = PasswordCredentials(loginName, password) :: Nil)
        }
    }
  
  def listParties(implicit sc: SecurityContext): Future[List[Party]] = 
    partyDao.retrieveAll		  
  

  /**
   * Create a party based on specific fields received.
   */
  def createParty(party: Party)(implicit sc: SecurityContext): Future[Party] = {
    partyDao.create(party).map(_.entity)
  }

  /**
   * Find a specific party by id.
   */
  def getParty(partyId: Id[Party])(implicit sc: SecurityContext): Future[Option[Party]] = {
    partyCache(partyId, () => partyDao.retrieve(partyId).map(_.map(_.entity)))
  }

  /**
   * Delete a specific party based on its id.
   */
  def deleteParty(entityId: Id[Party])(implicit sc: SecurityContext): String = {
    ""
  }

  def updateUser(id: Id[User], user : User)(implicit sc: SecurityContext): String = 
    ""
    
  def findUser(id: Id[User])(implicit sc: SecurityContext): Future[Option[User]] = 
    Future.successful(None)

  /**
   * To check if user has enough rights to use a specific party id for specific operations (eg. to create a location for this partyId) we have to
   * check if that party is the party of current user OR if it's a child party.
   */
  def userHasEnoughRights(partyId: Id[Party], user: User) = {
    false
  }
}