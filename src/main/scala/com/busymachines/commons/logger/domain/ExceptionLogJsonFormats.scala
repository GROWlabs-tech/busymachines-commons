package com.busymachines.commons.logger.domain

import com.busymachines.commons.Implicits._

object Implicits extends ExceptionLogJsonFormats{}
trait ExceptionLogJsonFormats {
  implicit val codeLocationInfoFormat = format4(CodeLocationInfo)
  implicit val defaultExceptionFormat = format4(DefaultExceptionInfo)

  implicit val commonExceptionFormat = format6(CommonExceptionInfo)
  implicit val logMessageFormat = format8(LogMessage)
}
