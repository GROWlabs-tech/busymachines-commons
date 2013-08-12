package com.kentivo.mdm.api.v1

import com.kentivo.mdm.api.ApiDirectives
import com.kentivo.mdm.logic.AuthenticationToken
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes
import akka.actor.ActorRefFactory
import akka.actor.ActorSystem
import akka.actor.ActorContext
import spray.routing.RequestContext
import com.busymachines.commons.http.CommonHttpService
import com.kentivo.mdm.api.UserAuthenticator
import com.kentivo.mdm.logic.AuthenticationData
import scala.concurrent.duration._
import com.kentivo.mdm.domain.User

case class Credentials(
  email: String,
  password: String,
  partyName: Option[String])

object AuthenticationApiV1 {
  val tokenKey = "Auth-Token"
}

/**
 * Handling authentication before using API.
 */
class AuthenticationApiV1(authenticator: UserAuthenticator)(implicit actorRefFactory: ActorRefFactory) extends CommonHttpService with ApiDirectives {
  
  implicit val credentialsFormat = jsonFormat3(Credentials)
  
  def route: RequestContext => Unit =
    path("users" / "authentication") { 
      post {
       entity(as[Credentials]) { 
         case Credentials(email, password, _) =>
           authenticator.authenticateUser(email, password).map {
             case Some()
           }
       }
      }
    } ~
    path("users" / MatchId[User] / "authentication") { userId =>
      // Check if user is authenticated.
      get {
        authenticator.isAuthenticated(1.minute) {
          case Some(auth) if auth.userId == userId =>
            complete("OK")
          case _ =>
            reject
        }
      } ~
        // Log in a specific user. Password will be in the body, in json format.  
        post {
          entity(as[AuthenticationUser]) { authenticationUser =>
            authenticator.authenticateUser(userName, authenticationUser.password, authenticationUser.partyName) match {
              case Some(AuthenticationToken(token)) => {
                val message = "User %s has been succesfully logged in".format(userName)
                debug(message)
                respondWithHeader(RawHeader(AuthenticationApiV1.tokenKey, token)) {
                  complete {
                    // Return authenticated user.
                    Authentication.isAuthenticated(AuthenticationToken(token)).getOrElse(null)
                  }
                }
              }
              case None => {
                debug("Tried to log in user %s but received 'Invalid userName or password.'".format(userName))
                respondWithStatus(StatusCodes.NotFound) {
                  complete {
                    Map("message" -> "Invalid userName or password.")
                  }
                }
              }
            }
          }
        } ~
        // Log out a specific user.
        delete {
          headerValueByName(AuthenticationApiV1.tokenKey) { tokenValue =>
            Authentication.isAuthenticated(new AuthenticationToken(tokenValue)) match {
              case Some(user) => {
                Authentication.deAuthenticate(AuthenticationToken(tokenValue))
                val message = "User %s has been succesfully logged out".format(userName)
                debug(message)
                complete {
                  Map("message" -> message)
                }
              }
              case None => {
                val message = "User %s is not logged in".format(userName)
                debug(message)
                respondWithStatus(StatusCodes.NotFound) {
                  complete {
                    Map("message" -> message)
                  }
                }
              }
            }
          }
        }
    }
}