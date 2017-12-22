package busymachines.rest_json_test.routes_to_test

import busymachines.rest._

import scala.concurrent.Future

/**
  *
  * You have several options of bringing in your json. Ordered from slowest compile time to fastest:
  * ==========================
  * 1:
  * {{{
  *   busymachines.json._
  *   class AuthenticatedRoutesRestAPIForTesting ... with JsonSupport
  * }}}
  *
  * ==========================
  * 2:
  * {{{
  *   import SomeTestDTOJsonCodec._ //this already extends JsonSupport
  * }}}
  * ==========================
  * 3:
  * {{{
  *   class AuthenticatedRoutesRestAPIForTesting ... with SomeTestDTOJsonCodec
  * }}}
  * ==========================
  *
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 07 Sep 2017
  *
  */
private[rest_json_test] class BearerAuthenticatedRoutesRestAPIForTesting
    extends JsonRestAPI with Directives with SomeTestDTOJsonCodec {

  //  Alternantively, if you remove SomeTestDTOJsonCodec mixing
  //  import busymachines.json._

  //  Alternatively, if none of the above:
  //  import SomeTestDTOJsonCodec._

  override protected def routeDefinition: Route = {
    pathPrefix("bearer_authentication") {
      RestAPIAuthentications.TokenBearer.authentication { basicAuth: String =>
        pathEndOrSingleSlash {
          get {
            complete((StatusCodes.OK, Future.successful(SomeTestDTOGet(int = 42, string = basicAuth, None))))

          }
        }
      }
    } ~ pathPrefix("bearer_opt_authentication") {
      RestAPIAuthentications.TokenBearer.optionalAuthentication { basicAuth: Option[String] =>
        pathEndOrSingleSlash {
          get {
            complete(
              (StatusCodes.OK,
               Future.successful(SomeTestDTOGet(int = 42, string = basicAuth.getOrElse("it's optional!"), None)))
            )
          }
        }
      }
    }
  }
}
