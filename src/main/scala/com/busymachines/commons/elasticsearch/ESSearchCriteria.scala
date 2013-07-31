package com.busymachines.commons.elasticsearch

import com.busymachines.commons.dao.SearchCriteria
import com.busymachines.commons.domain.HasId
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import spray.json.JsonWriter
import spray.json.JsValue
import spray.json.JsString

trait ESSearchCriteria[A] extends SearchCriteria[A] {
  def toFilter : FilterBuilder
  def && (other : ESSearchCriteria[A]) = 
    ESSearchCriteria.And(other)
}
 
object JsValueConverters {
  implicit val stringConverter = new JsValueConverter[String] {
    def convert(value : JsValue) : String = 
    value match {
    	case JsString(s) => s
    	case other => other.toString
    }
  }
}


trait JsValueConverter[T] {
    def convert(value : JsValue) : T
}

object ESSearchCriteria {
  class Delegate[A](criteria : => ESSearchCriteria[A]) extends ESSearchCriteria[A] {
    def toFilter = criteria.toFilter 
  }
  case class And[A](children : ESSearchCriteria[A]*) extends ESSearchCriteria[A] {
    override def && (other : ESSearchCriteria[A]) = 
      And((children.toSeq :+ other):_*)
    def toFilter = FilterBuilders.andFilter(children.map(_.toFilter):_*) 
  }
  case class Equals[A, T, V](path : Path[A, T], value : V) extends ESSearchCriteria[A] {
    def toFilter = 
      path.properties match {
        case p :: Nil => FilterBuilders.termFilter(p.mappedName, value)
        case property :: rest =>
          val names = path.properties.map(_.mappedName)
          FilterBuilders.nestedFilter(names.dropRight(1).mkString("."), FilterBuilders.termFilter(names.mkString("."), value))
        case _ => FilterBuilders.matchAllFilter
    }
  }
  case class Equals2[A, T, V](path : Path[A, T], value : V)(implicit writer : JsonWriter[V], jsConverter : JsValueConverter[T]) extends ESSearchCriteria[A] {
	def toFilter = 
	  path.properties match {
	  case p :: Nil => FilterBuilders.termFilter(p.mappedName, value)
	  case property :: rest =>
	  val names = path.properties.map(_.mappedName)
	  FilterBuilders.nestedFilter(names.dropRight(1).mkString("."), FilterBuilders.termFilter(names.mkString("."), value))
	  case _ => FilterBuilders.matchAllFilter
    }
  }
}