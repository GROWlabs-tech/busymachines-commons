package com.kentivo.mdm.api

import com.kentivo.mdm.domain.User
import com.busymachines.commons.http.AbstractAuthenticator
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import com.busymachines.commons.domain.Id
import com.kentivo.mdm.domain.Party
import com.kentivo.mdm.logic.AuthenticationData
import com.kentivo.mdm.logic.PartyService

class UserAuthenticator(partyService : PartyService)(implicit executionContext: ExecutionContext) extends AbstractAuthenticator[AuthenticationData] {

  def authenticateUser(email: String, password: String) = 
    partyService.authenticate(email, password)
} 