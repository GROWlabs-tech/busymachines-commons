package com.busymachines.commons.mail

import com.busymachines.commons.Implicits._
import com.busymachines.commons.CommonConfig

case class IncomingMailConfig(baseName: String) extends CommonConfig(baseName) {
  def ssl = boolean("ssl")
  def protocol = string("protocol")
  def host = string("host") 
  def port = int("port") 
  def userName = string("userName") 
  def password = string("password") 
}