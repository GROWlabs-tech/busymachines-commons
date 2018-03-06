package busymachines

import busymachines.core._

object InspectAnomalies {
  final case object SingletonIIFWithNoID extends InvalidInputFailure("SingletonIIFWithNoID_msg")

  final case class IIFWithNoID(msg: String) extends InvalidInputFailure(s"IIFWithNoID: $msg")

}
