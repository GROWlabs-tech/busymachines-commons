package busymachines

import busymachines.core.exceptions.Failure

/**
  *
  * Use when you don't want to import the automatic derivation of encoders/decoders
  * of the [[json]] package. Everything else pretty much stays the same.
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 19 Oct 2017
  *
  */
package object jsonbare {
  type Encoder[A] = io.circe.Encoder[A]
  final val Encoder: io.circe.Encoder.type = io.circe.Encoder
  type ObjectEncoder[A] = io.circe.ObjectEncoder[A]
  final val ObjectEncoder: io.circe.ObjectEncoder.type = io.circe.ObjectEncoder

  type Decoder[A] = io.circe.Decoder[A]
  final val Decoder: io.circe.Decoder.type = io.circe.Decoder
  type ObjectDecoder[A] = io.circe.ObjectEncoder[A]
  final val ObjectDecoder: io.circe.ObjectEncoder.type = io.circe.ObjectEncoder

  type Configuration = io.circe.generic.extras.Configuration
  final val Configuration: io.circe.generic.extras.Configuration.type = io.circe.generic.extras.Configuration

  type Json = io.circe.Json
  val Json: io.circe.Json.type = io.circe.Json
  type JsonObject = io.circe.JsonObject
  val JsonObject: io.circe.JsonObject.type = io.circe.JsonObject
  type HCursor = io.circe.HCursor
  val HCursor: io.circe.HCursor.type = io.circe.HCursor

  type JsonDecodingResult[A] = Either[Failure, A]
  type JsonParsingResult = Either[Failure, Json]
}
