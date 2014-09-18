package com.busymachines.commons.logging

import org.apache.logging.log4j.{Level, LogManager}

trait AdditionalParameters {
  def apply: Seq[(String, String)]
}

object DefaultAdditionalParameters extends AdditionalParameters {
  def apply: Seq[(String, String)] = Nil
}

trait Logging {
  def loggerTag: Option[String] = None
  implicit def defaultAdditionalParameters: AdditionalParameters = DefaultAdditionalParameters
  val logger = new Logger(loggerTag,getClass)
}

sealed class Logger[T](tag: Option[String], name: Class[T]) {
  private lazy val logger = LogManager.getLogger(name)

  def isTraceEnabled = logger.isTraceEnabled

  def isDebugEnabled = logger.isDebugEnabled

  def isInfoEnabled = logger.isInfoEnabled

  def isWarnEnabled = logger.isWarnEnabled

  def isErrorEnabled = logger.isErrorEnabled

  def isFatalEnabled = logger.isFatalEnabled

  def trace(message: => String, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.TRACE, message, None, parameters)

  def trace(message: => String, cause: => Throwable, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.TRACE, message, Some(cause), parameters)

  def debug(message: => String, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.DEBUG, message, None, parameters)

  def debug(message: => String, cause: => Throwable, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.DEBUG, message, Some(cause), parameters)

  def info(message: => String, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.INFO, message, None, parameters)

  def info(message: => String, cause: => Throwable, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.INFO, message, Some(cause), parameters)

  def warn(message: => String, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.WARN, message, None, parameters)

  def warn(message: => String, cause: => Throwable, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.WARN, message, Some(cause), parameters)

  def error(message: => String, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.ERROR, message, None, parameters)

  def error(message: => String, cause: => Throwable, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.ERROR, message, Some(cause), parameters)

  def fatal(message: => String, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.FATAL, message, None, parameters)

  def fatal(message: => String, cause: => Throwable, parameters: (String, String)*)(implicit ap: AdditionalParameters) =
    log(Level.FATAL, message, Some(cause), parameters)

  private def log(level: Level, message: => String, cause: Option[Throwable], parameters: Seq[(String, String)])(implicit ap: AdditionalParameters) {
    if (logger.isEnabled(level)) {
      val commonsLogMessage = CommonsLoggerMessage(message, cause, parameters ++ ap.apply, tag)
      logger.log(level, commonsLogMessage, cause.orNull)
    }
  }
}