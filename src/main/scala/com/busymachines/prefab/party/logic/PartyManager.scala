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
import com.busymachines.prefab.party.implicits._
import com.busymachines.prefab.party.db.UserDao
import com.busymachines.prefab.party.service.SecurityContext
import com.busymachines.prefab.party.domain.User
import com.busymachines.prefab.party.service.PartyService
import com.busymachines.commons.implicits._

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



  def listChildParties(implicit sc: SecurityContext): Future[List[Party]] =
    partyDao.retrieve(sc.partyId).flatMap(party=>partyDao.retrieve(party.get.entity.relations.map(_.relatedParty)).map(_.map(_.entity)))


  def listParties(implicit sc: SecurityContext): Future[List[Party]] = 
    partyDao.retrieveAll		  
  

  /**
   * Create a party based on specific fields received.
   */
  def createParty(party: Party)(implicit sc: SecurityContext): Future[Party] =
    partyDao.create(party).map(_.entity)


  /**
   * Find a specific party by id.
   */
  def getParty(partyId: Id[Party])(implicit sc: SecurityContext): Future[Option[Party]] =
    partyCache(partyId, () => partyDao.retrieve(partyId).map(_.map(_.entity)))

  def getPartyByEmail(email:String)(implicit sc: SecurityContext): Future[Option[Party]] =
    partyDao.findByEmailId(email)

  /**
   * Delete a specific party based on its id.
   */
  def deleteParty(entityId: Id[Party])(implicit sc: SecurityContext): Future[Unit] =
    partyDao.delete(entityId).map(_=>partyCache.remove(entityId))


  def updateUser(id: Id[User], user : User)(implicit sc: SecurityContext): Future[Unit] =
   userDao.modify(id)(_user=> user).map(_=>Unit)


  def findUser(id: Id[User])(implicit sc: SecurityContext): Future[Option[User]] = 
    userDao.retrieve(id)

  /**
   * To check if user has enough rights to use a specific party id for specific operations (eg. to create a location for this partyId) we have to
   * check if that party is the party of current user OR if it's a child party.
   */
  def userHasEnoughRights(partyId: Id[Party], user: User): Future[Boolean] = partyDao.findUserById(user.id) map {
    case Some(tup) => tup._1.id == partyId.value ||( tup._1.owner !=None && tup._1.owner.get == partyId)
    case None => false
  }
}