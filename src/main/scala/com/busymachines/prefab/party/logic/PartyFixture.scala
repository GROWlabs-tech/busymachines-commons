package com.busymachines.prefab.party.logic

import com.busymachines.commons.CommonConfig
import com.busymachines.commons.domain.Id
import com.busymachines.prefab.authentication.elasticsearch.ESCredentialsDao
import com.busymachines.prefab.authentication.model.Credentials
import com.busymachines.prefab.authentication.model.PasswordCredentials
import com.busymachines.prefab.party.db.PartyDao
import com.busymachines.prefab.party.db.UserDao
import com.busymachines.prefab.party.domain.Address
import com.busymachines.prefab.party.domain.Company
import com.busymachines.prefab.party.domain.Party
import com.busymachines.prefab.party.domain.Tenant
import com.busymachines.prefab.party.domain.User
import com.busymachines.prefab.party.service.PartyService

class PartyFixture(partyDao : PartyDao, credentialsDao : ESCredentialsDao) {

  def create {
    if (CommonConfig.devmode) {
      
      val testTenantId = Id.static[Tenant]("test-tenant-1")
      val testParty1Id = Id.static[Party]("test-party-1")
      val testUser1Id = Id.static[User]("test-user-1")
      val testUser1CredentialsId = Id.static[Credentials]("test-user-1-credentials")
      
      val user1 = User(
        id = testUser1Id,
        credentials = testUser1CredentialsId,
        firstName = Some("John"),
        lastName = Some("Doe"),
        addresses = Address(street = Some("Street 1")) :: Nil)
  
      partyDao.getOrCreateAndModify(testParty1Id)(Party(testParty1Id, testTenantId)) { party =>
        party.copy(tenant = testTenantId, users = user1 :: Nil, company = Some(Company("Test Company")))
      }
      
      credentialsDao.getOrCreateAndModify(user1.credentials)(Credentials(user1.credentials)) { credentials =>
        credentials.copy(passwordCredentials = PasswordCredentials("user1@test.com", "test") :: Nil)
      }
    }
  }
}