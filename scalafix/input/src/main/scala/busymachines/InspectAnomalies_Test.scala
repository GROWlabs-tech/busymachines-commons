/*
rule = "class:busymachines.InspectAnomalies"
 */
package busymachines

import busymachines.core._

object InspectAnomalies_Test {
  final case object SingletonIIFWithNoID extends InvalidInputFailure("SingletonIIFWithNoID_msg")

  final case class IIFWithNoID(msg: String) extends InvalidInputFailure(s"IIFWithNoID: $msg")

}
