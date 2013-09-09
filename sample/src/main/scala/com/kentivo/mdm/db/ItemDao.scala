package com.kentivo.mdm.db

import scala.concurrent.ExecutionContext
import com.busymachines.commons.elasticsearch.ESRootDao
import com.busymachines.commons.elasticsearch.ESSearchCriteria
import com.busymachines.commons.elasticsearch.ESIndex
import com.busymachines.commons.elasticsearch.ESProperty.toPath
import com.busymachines.commons.elasticsearch.ESType
import com.busymachines.commons.domain.Id
import com.kentivo.mdm.domain.DomainJsonFormats._
import com.kentivo.mdm.domain.Item
import com.kentivo.mdm.domain.Property

case class HasValueForProperty(propertyId : Id[Property], value : Option[String] = None, locale : Option[Option[String]] = None, unit : Option[Unit] = None) extends ESSearchCriteria.Delegate (
  ItemMapping.values / PropertyValueMapping.property equ propertyId
)


class ItemDao(index : ESIndex)(implicit ec: ExecutionContext) extends ESRootDao[Item](index, ESType("item", ItemMapping))