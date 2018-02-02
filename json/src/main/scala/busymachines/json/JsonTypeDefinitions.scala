/**
  * Copyright (c) 2017-2018 BusyMachines
  *
  * See company homepage at: https://www.busymachines.com/
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package busymachines.json

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 26 Jan 2018
  *
  */
trait JsonTypeDefinitions {
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
}
