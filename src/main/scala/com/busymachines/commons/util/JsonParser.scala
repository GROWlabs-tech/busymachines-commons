package com.busymachines.commons.util

import scala.collection.mutable.ListBuffer
import spray.json._

/**
 * Replacement for spray-json parser.
 *
 * Created by Ruud Diterwich on 17/04/14.
 */
object JsonParser {
  def parse(s: String) =
    (new JsonParser).parse(s)

  def parse(s: Array[Char]) =
    (new JsonParser).parse(s)
}

class JsonParser {

  private var s: Array[Char] = Array.empty
  private var i: Int = 0
  private var c: Char = 0
  private val sb = new StringBuilder

  def parse(s: String): JsValue =
    parse(s.toCharArray)

  def parse(s: Array[Char]): JsValue = {
    this.s = s
    this.i = -1
    next()
    whitespace()
    val result = jsonValue()
    whitespace()
    if (c != 0)
      exception("expected end of document")
    result
  }

  private def jsonObject(): Option[JsObject] = {
    if (c == '{') {
      next()
      val ab = new ListBuffer[(String, JsValue)]
      whitespace()
      while (c != '}') {
        whitespace()
        string() match {
          case Some(name) =>
            whitespace()
            if (c != ':')
              exception("Expected ':'")
            next()
            whitespace()
            val v = jsonValue()
            ab += name -> v
          case None =>
            exception("Expected name")
        }
        whitespace()
        if (c == ',') next()
        else if (c != '}')
          exception("expected '}'")
      }
      next()
      Some(new JsObject(ab.toMap))
    }
    else None
  }

  private def jsonArray(): Option[JsArray] = {
    if (c == '[') {
      next()
      val lb = new ListBuffer[JsValue]
      whitespace()
      while (c != ']') {
        whitespace()
        lb += jsonValue()
        whitespace()
        if (c == ',')
          next()
        else if (c != ']')
          exception("expected ']'")
      }
      next()
      Some(new JsArray(lb.toList))
    }
    else None
  }

  private def jsonValue(): JsValue =
    jsonString() orElse
      jsonNumber() orElse
      jsonObject() orElse
      jsonArray() orElse
      jsonConstant() getOrElse exception("value expected")

  private def jsonString(): Option[JsString] =
    string().map(JsString(_))

  private def jsonConstant(): Option[JsValue] = {
    if (c == 't') {
      for (cc <- "true")
        if (c == cc) next() else exception("expected 'true'")
      Some(JsTrue)
    }
    else if (c == 'f') {
      for (cc <- "false")
        if (c == cc) next() else exception("expected 'false'")
      Some(JsFalse)
    }
    else if (c == 'n') {
      for (cc <- "null")
        if (c == cc) next() else exception("expected 'null'")
      Some(JsNull)
    }
    else None
  }

  private def string(): Option[String] = {
    if (c == '"') {
      next()
      sb.clear()
      while (c != 0 && c != '"') {
        if (c == '\\') {
          next() match {
            case '"' => sb.append('"'); next()
            case '\\' => sb.append('\\'); next()
            case '/' => sb.append('/'); next()
            case 'b' => sb.append('\b'); next()
            case 'f' => sb.append('\f'); next()
            case 'n' => sb.append('\n'); next()
            case 'r' => sb.append('\r'); next()
            case 't' => sb.append('\t'); next()
            case 'u' =>
              next()
              val code =
                (hexDigit() << 12) +
                (hexDigit() << 8) +
                (hexDigit() << 4) +
                hexDigit()
              sb.append(code.asInstanceOf[Char])
            case _ => exception("expected escape char")
          }
        } else {
          sb.append(c)
          next()
        }
      }
      if (c != '"')
        exception("expected '\"'")
      next()
      Some(sb.toString())
    }
    else None
  }

  private def jsonNumber(): Option[JsNumber] = {
    sb.clear()
    if (c == '-') {
      sb.append(c)
      next()
    }
    while (c >= '0' && c <= '9') {
      sb.append(c)
      next()
    }
    if (c == '.') {
      sb.append(c)
      next()
      while (c >= '0' && c <= '9') {
        sb.append(c)
        next()
      }
    }
    if (c == 'e' || c == 'E') {
      sb.append(c)
      next()
      if (c == '-' || c == '+') {
        sb.append(c)
        next()
      }
      while (c >= '0' && c <= '9') {
        sb.append(c)
        next()
      }
    }
    if (sb.nonEmpty) Some(JsNumber(sb.toString()))
    else None
  }

  private def whitespace() =
    while (Character.isWhitespace(c))
      next()

  private def hexDigit(): Int = {
    val m = c
    if (c >= 'a' && c <= 'f') {
      next()
      m - 'a' + 10
    }
    else if (c >= 'A' && c <= 'F') {
      next()
      m - 'A' + 10
    }
    else if (c >= '0' && c <= '9') {
      next()
      m - '0'
    }
    else exception("Hex digit expected")
  }

  private def next() = {
    i += 1
    if (i < s.length) {
      c = s(i)
    } else {
      c = 0
    }
    c
  }

  def exception(message: String) =
    throw new JsonParseException(new String(s), i, message)
}

class JsonParseException(val s: String, val pos: Int, val msg: String) extends Exception(s"Json parse exception at position $pos: $msg")
