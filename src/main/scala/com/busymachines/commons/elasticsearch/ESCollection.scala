package com.busymachines.commons.elasticsearch

import collection.JavaConversions._
import scala.concurrent.{Future, ExecutionContext}
import com.busymachines.commons.dao._
import com.busymachines.commons.Logging
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders}
import org.elasticsearch.action.get.GetRequest
import com.busymachines.commons.util.JsonParser
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.search.facet.{FacetBuilders, FacetBuilder}
import org.elasticsearch.search.facet.histogram.HistogramFacet
import org.elasticsearch.search.facet.terms.TermsFacet
import scala.concurrent.duration.Duration
import org.elasticsearch.action.index.IndexRequest
import com.busymachines.commons.domain.Id
import org.elasticsearch.transport.RemoteTransportException
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.action.delete.DeleteRequest
import java.util.UUID
import spray.json.{JsString, JsValue, JsObject}

/**
 * Collection of documents represented by type.
 */
class ESCollection[T](index: ESIndex, typeName: String, mapping: ESMapping[T])(implicit ec: ExecutionContext) extends Logging {

  val client = index.client
  val indexName = index.name
  val eventName = mapping.mappingName + "_" + indexName + "_" + typeName
  val all: SearchCriteria[T] = ESSearchCriteria.All[T]()
  val defaultSort: SearchSort = ESSearchSort.asc("_id")

  // Add mapping.
  index.onInitialize { () =>
    val mappingConfiguration = mapping.mappingDefinition(typeName).toString
    try {
      debug(s"Schema for $indexName/$typeName: $mappingConfiguration")
      client.javaClient.admin.indices.putMapping(new PutMappingRequest(indexName).`type`(typeName).source(mappingConfiguration)).get()
    }
    catch {
      case e : Throwable =>
        error(s"Invalid schema for $indexName/$typeName: $mappingConfiguration: ${e.getMessage}", e)
        throw e
    }
  }

  def retrieve(id: String): Future[Option[Versioned[T]]] = {
    val request = new GetRequest(indexName, typeName, id.toString)
    client.execute(request) map {
      case response if !response.isExists =>
        None
      case response if response.isSourceEmpty =>
        error(s"No source available for $typeName with id $id")
        None
      case response =>
        val json = JsonParser.parse(response.getSourceAsString)
        Some(Versioned(mapping.jsonFormat.read(json), response.getVersion))
    }
  }

  def retrieve(ids: Seq[String]): Future[List[Versioned[T]]] =
    search(new ESSearchCriteria[T] {
      def toFilter = FilterBuilders.idsFilter(typeName).addIds(ids:_*)
      def prepend[A0](path : ESPath[A0, T]) = ???
    }, page = Page.all).map(_.result)

  def retrieveAll: Future[List[Versioned[T]]] =
    search(all, page = Page.all).map(_.result)

  def search(criteria: SearchCriteria[T], page: Page = Page.first, sort: SearchSort = defaultSort, facets: Seq[Facet] = Seq.empty): Future[SearchResult[T]] = {
    criteria match {
      case criteria: ESSearchCriteria[T] =>

        var request =
          client.javaClient.prepareSearch(indexName)
            .setTypes(typeName)
            .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), criteria.toFilter))
            .setSearchType(SearchType.DFS_QUERY_AND_FETCH)
            .setFrom(page.from)
            .setSize(page.size)
            .setVersion(true)

        // Sorting
        sort match {
          case esSearchOrder:ESSearchSort =>
            request = request.addSort(esSearchOrder.field, esSearchOrder.order)
        }

        // Faceting
        val requestFacets = toESFacets(facets)
        for (facet <- requestFacets) {
          request = request.addFacet(facet._2)
        }

        client.execute(request.request).map { result =>
          SearchResult(result.getHits.hits.toList.map { hit =>
            val json = JsonParser.parse(hit.sourceAsString)
            Versioned(mapping.jsonFormat.read(json), hit.version)
          }, Some(result.getHits.getTotalHits),
            if (result.getFacets != null) convertESFacetResponse(facets, result) else Map.empty)
        }
      case _ =>
        throw new Exception("Expected ElasticSearch search criteria")
    }
  }

  def create(entity: T, refreshAfterMutation: Boolean, ttl: Option[Duration] = None): Future[Versioned[T]] = {
    val json = mapping.jsonFormat.write(entity)
    val request = new IndexRequest(indexName, typeName)
      .id(getIdFromJson(json))
      .create(true)
      .source(json.toString)
      .refresh(refreshAfterMutation)
      .ttl(ttl.map(_.toMillis).map(new java.lang.Long(_)).getOrElse(null))

    // Call synchronously, useful for debugging: proper stack trace is reported. TODO make config flag.
    //      val response = client.execute(IndexAction.INSTANCE, request).get
    //      Future.successful(Versioned(entity, response.getVersion.toString))

      client.execute(request).map(response => Versioned(entity, response.getVersion))
    .recover(convertException { e =>
      //      debug(s"Create ${indexName}/${typeName}/${entity.id} failed: $e:\n${XContentHelper.convertToJson(request.source, true, true)}")
      throw e
    })
  }

  def getOrCreate(id: String, refreshAfterMutation: Boolean)(_create: => T): Future[Versioned[T]] = {
    retrieve(id) flatMap {
      case Some(entity) => Future.successful(entity)
      case None => create(_create, refreshAfterMutation)
    }
  }

  def getOrCreateAndModify(id: String, refreshAfterMutation: Boolean)(_create: => T)(_modify: T => T): Future[Versioned[T]] = {
    RetryVersionConflictAsync(10) {
      retrieve(id).flatMap {
        case None => create(_modify(_create), refreshAfterMutation)
        case Some(Versioned(entity, version)) =>
          update(Versioned(_modify(entity), version), refreshAfterMutation)
      }
    }
  }

  def getOrCreateAndModifyOptionally(id: String, refreshAfterMutation: Boolean)(_create: => T)(_modify: T => Option[T]): Future[Versioned[T]] = {
    RetryVersionConflictAsync(10) {
      retrieve(id).flatMap {
        case None =>
          val created : T = _create
          create(_modify(created).getOrElse(created), refreshAfterMutation)
        case Some(Versioned(entity, version)) =>
          _modify(entity) match {
            case Some(newEntity) => update(Versioned(newEntity, version), refreshAfterMutation)
            case None => Future.successful(Versioned(entity, version))
          }
      }
    }
  }

  def modify(id: String, refreshAfterMutation: Boolean)(modify: T => T): Future[Versioned[T]] = {
    RetryVersionConflictAsync(10) {
      retrieve(id).flatMap {
        case None => throw new IdNotFoundException(id.toString, typeName)
        case Some(Versioned(entity, version)) =>
          update(Versioned(modify(entity), version), refreshAfterMutation)
      }
    }
  }

  def modifyOptionally(id: String, refreshAfterMutation: Boolean)(modify: T => Option[T]): Future[Versioned[T]] = {
    RetryVersionConflictAsync(10) {
      retrieve(id).flatMap {
        case None => throw new IdNotFoundException(id.toString, typeName)
        case Some(Versioned(entity, version)) =>
          modify(entity) match {
            case Some(newEntity) => update(Versioned(newEntity, version), refreshAfterMutation)
            case None => Future.successful(Versioned(entity, version))
          }
      }
    }
  }

  /**
   * @throws VersionConflictException
   */
  def update(entity: Versioned[T], refreshAfterMutation: Boolean): Future[Versioned[T]] = {
    val newJson = mapping.jsonFormat.write(entity.entity)

    val request = new IndexRequest(indexName, typeName)
      .refresh(refreshAfterMutation)
      .id(getIdFromJson(newJson))
      .source(newJson.toString)
      .version(entity.version)

    // Call synchronously, useful for debugging: proper stack trace is reported. TODO make config flag.
    //      val response = client.execute(IndexAction.INSTANCE, request).get
    //      Future.successful(Versioned(entity, response.getVersion.toString))
      client.execute(request).map(response => Versioned(entity.entity, response.getVersion))
    .recover(convertException { e =>
      //      debug(s"Update ${indexName}/${typeName}/${entity.id} failed: $e:\n${XContentHelper.convertToJson(request.source, true, true)}")
      throw e
    })
  }

  def delete(id: String, refreshAfterMutation: Boolean): Future[Unit] = {
    val request = new DeleteRequest(indexName, typeName, id.toString).refresh(refreshAfterMutation)
    client.execute(request) map {
      response =>
        if (!response.isFound) {
          throw new IdNotFoundException(id, typeName)
        }
    }
  }

  def reindexAll() = {
    index.refresh
  }

  def onChange(f: Id[T] => Unit) {
    index.eventBus.subscribe {
      case ESRootDaoMutationEvent(n, id) if n == eventName =>
        f(Id[T](id))
    }
  }

  /**
   * Escapes a string special ES characters as specified here : {@link http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters}
   */
  private[elasticsearch] def escapeQueryText(s: String) = {
    var b0: StringBuilder = null
    def b = { if (b0 == null) b0 = new StringBuilder; b0 }
    var p = 0
    val l = s.length
    while (p < l) {
      val c = s.charAt(p)
      p += 1
      c match {
        case '+' => b.append("\\+")
        case '-' => b.append("\\-")
        case '!' => b.append("\\!")
        case '(' => b.append("\\(")
        case ')' => b.append("\\)")
        case '{' => b.append("\\{")
        case '}' => b.append("\\}")
        case '[' => b.append("\\[")
        case ']' => b.append("\\]")
        case '^' => b.append("\\^")
        case '\"' => b.append("\\\"")
        case '~' => b.append("\\~")
        case '*' => b.append("\\*")
        case '?' => b.append("\\?")
        case ':' => b.append("\\:")
        case '\\' => b.append("\\\\")
        case ' ' => b.append("\\ ")
        case '&' if p < l && s.charAt(p) == '&' => b.append("\\&")
        case '|' if p < l && s.charAt(p) == '|' => b.append("\\|")
        case c => b.append(c)
      }
      if (b0 == null) s
      else b0.toString
    }
  }

  private def toESFacets(facets: Seq[Facet]): Map[Facet, FacetBuilder] =
    facets.map {
      case historyFacet: ESHistoryFacet =>
        (historyFacet.keyScript,historyFacet.valueScript,historyFacet.interval) match {
          case (Some(keyScript),Some(valueScript),Some(interval)) =>
            historyFacet ->  FacetBuilders.histogramScriptFacet(historyFacet.name).facetFilter(historyFacet.searchCriteria.asInstanceOf[ESSearchCriteria[_]].toFilter).keyScript(keyScript).valueScript(valueScript).interval(interval)
          case _ => throw new Exception(s"Only script histogram facets are supported for now")
        }

      // TODO : Investigate if this is generic enough : http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-facets.html#_all_nested_matching_root_documents
      case termFacet: ESTermFacet =>
        val fieldList = termFacet.fields.map(_.toString)
        val firstFacetField = fieldList.head
        val pathComponents = firstFacetField.split("\\.").toList
        val facetbuilder = pathComponents.size match {
          case s if s <= 1 => FacetBuilders.termsFacet(termFacet.name)
          case s if s > 1 => FacetBuilders.termsFacet(termFacet.name).nested(pathComponents.head)
        }

        termFacet -> facetbuilder.size(termFacet.size).facetFilter(termFacet.searchCriteria.asInstanceOf[ESSearchCriteria[_]].toFilter).fields(fieldList: _*)
      case _ => throw new Exception(s"Unknown facet type")
    }.toMap

  private def convertESFacetResponse(facets: Seq[Facet], response: org.elasticsearch.action.search.SearchResponse) =
    response.getFacets.facetsAsMap.entrySet.map { entry =>
      val facet = facets.collectFirst { case x if x.name == entry.getKey => x }.getOrElse(throw new Exception(s"The ES response contains facet ${entry.getKey} that were not requested"))
      entry.getValue match {
        case histogramFacet: HistogramFacet =>
          facet.name -> histogramFacet.getEntries.map(histogramEntry =>
            HistogramFacetValue(key = histogramEntry.getKey,
              count = histogramEntry.getCount,
              min = histogramEntry.getMin,
              max = histogramEntry.getMax,
              total = histogramEntry.getTotal,
              total_count = histogramEntry.getTotalCount,
              mean = histogramEntry.getMean)
          ).toList
        case termFacet: TermsFacet =>
          facet.name -> termFacet.getEntries.map(termEntry => TermFacetValue(termEntry.getTerm.string, termEntry.getCount)).toList
        case _ => throw new Exception(s"The ES reponse contains unknown facet ${entry.getValue}")
      }
    }.toMap

  private def convertException(f : Throwable => Versioned[T]) : PartialFunction[Throwable, Versioned[T]] = {
    case t : Throwable =>
      val cause = t match {
        case t : RemoteTransportException => t.getCause
        case t2 => t2
      }
      val converted = cause match {
        case t: VersionConflictEngineException => new VersionConflictException(t)
        case t2 => t2
      }
      f(converted)
  }

  private def getIdFromJson(json: JsValue): String =
    json match {
      case JsObject(fields) =>
        fields.get("_id") match {
          case Some(JsString(id)) => id.toString
          case _ => UUID.randomUUID.toString
        }
      case _ => UUID.randomUUID.toString
    }
}
