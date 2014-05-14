package com.busymachines.commons.implicits

import _root_.spray.json.{JsValue, JsonFormat}

import com.busymachines.commons.Implicits._

class RichAny[A](val a: A) extends AnyVal {
  def toOption(f : A => Boolean) = 
    if (f(a)) Some(a)
    else None

  def replaceWithGeneratedIds(implicit format : JsonFormat[A]) : A =
    format.read(format.write(a).replaceWithGeneratedIds)

  def setField(field : String, value : JsValue)(implicit format : JsonFormat[A]) : A =
    format.read(format.write(a).setField(field, value))

  def unsetField(field : String)(implicit format : JsonFormat[A]) : A =
    format.read(format.write(a).unsetField(field))
}