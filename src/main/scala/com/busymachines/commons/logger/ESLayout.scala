package com.busymachines.commons.logger

import java.util

import com.busymachines.commons.CommonException
import com.busymachines.commons.logger.domain.{CodeLocationInfo, CommonExceptionInfo, DefaultExceptionInfo, LogMessage}
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.plugins.{Plugin, PluginAttribute, PluginFactory}
import org.apache.logging.log4j.core.layout.AbstractLayout
import org.joda.time.format.DateTimeFormat

/**
 * Created by Alexandru Matei on 15.08.2014.
 */

object ESLayout{
  @PluginFactory
  def createLayout(
                    @PluginAttribute("locationInfo") locationInfo:Boolean,
                    @PluginAttribute("properties") properties:Boolean,
                    @PluginAttribute("complete") complete:Boolean,
                    @PluginAttribute(value ="withCodeLocation", defaultBoolean = false) withCodeLoc:Boolean) = new ESLayout(locationInfo, properties, complete, withCodeLoc)

}
@Plugin(name = "ESLayout", category = "Core", elementType = "layout", printObject = true)
class ESLayout(locationInfo:Boolean, properties:Boolean, complete: Boolean, withCodeLoc:Boolean) extends AbstractLayout[LogMessage](null,null) {

  //TODO ???? Find a better way to serialize this
  override def toByteArray(event: LogEvent): Array[Byte] = return toSerializable(event).toString.getBytes

  override def getContentFormat: util.Map[String, String] = new java.util.HashMap[String,String]()

  override def getContentType: String = "text/plain"

  override def toSerializable(event: LogEvent): LogMessage = {
    val cli: Option[CodeLocationInfo] = createCodeLocation(event)
    val (exceptionFormat: Option[DefaultExceptionInfo], commonExceptionFormat: Option[CommonExceptionInfo]) = createExceptionInfo(event)

    LogMessage(cli, exceptionFormat, commonExceptionFormat)
  }

  def createExceptionInfo(event: LogEvent): (Option[DefaultExceptionInfo], Option[CommonExceptionInfo]) = {
    val (exceptionFormat, commonExceptionFormat) = event.getThrown() match {
      case null => (None, None)
      case e: CommonException => {
        val cExInfo = CommonExceptionInfo(
          message = Some(e.getMessage),
          cause = Some(e.getCause.toString),
          stackTrace = e.getStackTrace().toList.map(_.toString),
          errorId = Some(e.errorId),
          parameters = Some(e.parameters.mkString(",")))
        (None, Some(cExInfo))
      }
      case e: Throwable => {
        val exInfo = DefaultExceptionInfo(
          message = Some(e.getMessage),
          cause = Option(e.getCause()).map(_.toString),
          stackTrace = e.getStackTrace().toList.map(_.toString))
        (Some(exInfo), None)
      }
    }
    (exceptionFormat, commonExceptionFormat)
  }

  def createCodeLocation(event: LogEvent): Option[CodeLocationInfo] = {
    withCodeLoc match{
      case true => Some(CodeLocationInfo(
        level = Some(event.getLevel().toString()),
        thread = Some(event.getThreadName()),
        className = Some(event.getSource().getClassName()),
        fileName = Some(event.getSource().getFileName()),
        methodName = Some(event.getSource().getMethodName()),
        lineNumber = Some(event.getSource().getLineNumber()),
        time = Some(DateTimeFormat.longDateTime().print(event.getTimeMillis())),
        message = Some(event.getMessage().getFormattedMessage())))
      case false => None
    }
  }
}
