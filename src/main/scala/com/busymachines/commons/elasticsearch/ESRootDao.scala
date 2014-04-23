package com.busymachines.commons.elasticsearch

import collection.JavaConversions._
import com.busymachines.commons.Logging
import com.busymachines.commons.dao.Facet
import com.busymachines.commons.dao.IdNotFoundException
import com.busymachines.commons.dao.Page
import com.busymachines.commons.dao.RetryVersionConflictAsync
import com.busymachines.commons.dao.RootDao
import com.busymachines.commons.dao.SearchCriteria
import com.busymachines.commons.dao.SearchResult
import com.busymachines.commons.dao.SearchSort
import com.busymachines.commons.dao.VersionConflictException
import com.busymachines.commons.dao.Versioned
import com.busymachines.commons.dao.Versioned.toEntity
import com.busymachines.commons.domain.HasId
import com.busymachines.commons.domain.Id
import com.busymachines.commons.event.BusEvent
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.common.xcontent.XContentHelper
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.facet.FacetBuilder
import org.elasticsearch.search.facet.FacetBuilders
import org.elasticsearch.search.facet.histogram.HistogramFacet
import org.elasticsearch.search.facet.terms.TermsFacet
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.RemoteTransportException
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import spray.json._
import scala.Some
import com.busymachines.commons.dao.TermFacetValue
import com.busymachines.commons.dao.HistogramFacetValue

object ESRootDao {
  implicit def toResults[T <: HasId[T]](f: Future[SearchResult[T]])(implicit ec: ExecutionContext) = f.map(_.result)
}

class ESRootDao[T <: HasId[T]: JsonFormat: ClassTag](index: ESIndex, t: ESType[T])(implicit ec: ExecutionContext, tag: ClassTag[T]) extends ESDao[T](t.name) with RootDao[T] with Logging {

  val client = index.client
  val mapping = t.mapping

  println("Starting dao " + t.name)
  // Add mapping.
  index.onInitialize { () =>
    val mappingConfiguration = t.mapping.mappingDefinition(t.name).toString
    try {
      debug(s"Schema for ${index.name}/${t.name}: $mappingConfiguration")
      client.admin.indices.putMapping(new PutMappingRequest(index.name).`type`(t.name).source(mappingConfiguration)).get()
    }
    catch {
      case e : Throwable =>
      error(s"Invalid schema for ${index.name}/${t.name}: $mappingConfiguration: ${e.getMessage}", e)
      throw e
    }
  }

  /**
   * Escapes a string special ES characters as specified here : {@link http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters}
   */
  def escapeQueryText(queryText: String) =
    new StringBuilder(queryText).
      replaceAllLiterally("\\", "\\\\").
      replaceAllLiterally("/", "\\/").
      replaceAllLiterally(" ", "\\ ").
      replaceAllLiterally("+", "\\+").
      replaceAllLiterally("-", "\\-").
      replaceAllLiterally("&&", "\\&&").
      replaceAllLiterally("||", "\\||").
      replaceAllLiterally("!", "\\!").
      replaceAllLiterally("(", "\\(").
      replaceAllLiterally(")", "\\)").
      replaceAllLiterally("{", "\\{").
      replaceAllLiterally("}", "\\}").
      replaceAllLiterally("[", "\\[").
      replaceAllLiterally("]", "\\]").
      replaceAllLiterally("^", "\\^").
      replaceAllLiterally("\"", "\\\"").
      replaceAllLiterally("~", "\\~").
      replaceAllLiterally("*", "\\*").
      replaceAllLiterally("?", "\\?").
      replaceAllLiterally(":", "\\:").toString

  def allSearchText(searchText: String): ESSearchCriteria[T] =
    propertySearchText(mapping._all,searchText)

  def allSearchQuery(searchQuery: String): ESSearchCriteria[T] =
    propertySearchQuery(mapping._all,searchQuery)

  def propertySearchText(field: ESField[_,String],searchText: String): ESSearchCriteria[T] =
    propertySearchQuery(field,s"*${escapeQueryText(searchText)}*")

  def propertySearchQuery(property: ESField[_,String],searchQuery: String): ESSearchCriteria[T] =
    (property queryString searchQuery).asInstanceOf[ESSearchCriteria[T]]

  def retrieve(ids: Seq[Id[T]]): Future[List[Versioned[T]]] =
    search(new ESSearchCriteria[T] {
      def toFilter = FilterBuilders.idsFilter(typeName).addIds(ids.map(_.value):_*)
      def prepend[A0](path : ESPath[A0, T]) = ???
    }, page = Page.all).map(_.result)

  def retrieve(id: Id[T]): Future[Option[Versioned[T]]] = {
    val request = new GetRequest(index.name, t.name, id.toString)
    client.execute(request) map {
      case response if !response.isExists =>
        None
      case response if response.isSourceEmpty =>
        error(s"No source available for ${t.name} with id $id")
        None
      case response =>
        val json = response.getSourceAsString.asJson
        Some(Versioned(mapping.jsonFormat.read(json), response.getVersion))
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

  def reindexAll() = {
    index.refresh
  }
    
  def search(criteria: SearchCriteria[T], page: Page = Page.first, sort: SearchSort = defaultSort, facets: Seq[Facet] = Seq.empty): Future[SearchResult[T]] = {
    criteria match {
      case criteria: ESSearchCriteria[T] =>

        var request =
          client.javaClient.prepareSearch(index.name)
            .setTypes(t.name)
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

//        debug(s"Search ${index.name}/${t.name}: $request")
        client.execute(request.request).map { result =>
          SearchResult(result.getHits.hits.toList.map { hit =>
            val json = hit.sourceAsString.asJson
            Versioned(mapping.jsonFormat.read(json), hit.version)
          }, Some(result.getHits.getTotalHits),
            if (result.getFacets != null) convertESFacetResponse(facets, result) else Map.empty)
        }
      case _ =>
        throw new Exception("Expected ElasticSearch search criteria")
    }
  }

  def create(entity: T, refreshAfterMutation: Boolean, ttl: Option[Duration]): Future[Versioned[T]] = {
    val json = mapping.jsonFormat.write(entity)
    val request = new IndexRequest(index.name, t.name)
      .id(entity.id.toString)
      .create(true)
      .source(json.toString)
      .refresh(refreshAfterMutation)
      .ttl(ttl.map(_.toMillis).map(new java.lang.Long(_)).getOrElse(null))

    // Call synchronously, useful for debugging: proper stack trace is reported. TODO make config flag.       
    //      val response = client.execute(IndexAction.INSTANCE, request).get
    //      Future.successful(Versioned(entity, response.getVersion.toString))

    preMutate(entity).flatMap { entity : T =>
      client.execute(request).map(response => Versioned(entity, response.getVersion)) flatMap { storedEntity =>
        debug(s"Create ${index.name}/${t.name}/${entity.id}:\n${XContentHelper.convertToJson(request.source, true, true)}")
        postMutate(storedEntity) map { _ => storedEntity }
      }
    }.recover(convertException { e =>
      debug(s"Create ${index.name}/${t.name}/${entity.id} failed: $e:\n${XContentHelper.convertToJson(request.source, true, true)}")
      throw e
    })
  }

  def getOrCreate(id: Id[T], refreshAfterMutation: Boolean)(_create: => T): Future[Versioned[T]] = {
    retrieve(id) flatMap {
      case Some(entity) => Future.successful(entity)
      case None => create(_create, refreshAfterMutation)
    }
  }

  def getOrCreateAndModify(id: Id[T], refreshAfterMutation: Boolean)(_create: => T)(_modify: T => T): Future[Versioned[T]] = {
    RetryVersionConflictAsync(10) {
      retrieve(id).flatMap {
        case None => create(_modify(_create), refreshAfterMutation)
        case Some(Versioned(entity, version)) =>
          update(Versioned(_modify(entity), version), refreshAfterMutation)
      }
    }
  }

  def getOrCreateAndModifyOptionally(id: Id[T], refreshAfterMutation: Boolean)(_create: => T)(_modify: T => Option[T]): Future[Versioned[T]] = {
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

  def modify(id: Id[T], refreshAfterMutation: Boolean)(modify: T => T): Future[Versioned[T]] = {
    RetryVersionConflictAsync(10) {
      retrieve(id).flatMap {
        case None => throw new IdNotFoundException(id.toString, t.name)
        case Some(Versioned(entity, version)) =>
          update(Versioned(modify(entity), version), refreshAfterMutation)
      }
    }
  }

  def modifyOptionally(id: Id[T], refreshAfterMutation: Boolean)(modify: T => Option[T]): Future[Versioned[T]] = {
    RetryVersionConflictAsync(10) {
      retrieve(id).flatMap {
        case None => throw new IdNotFoundException(id.toString, t.name)
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

    val request = new IndexRequest(index.name, t.name)
      .refresh(refreshAfterMutation)
      .id(entity.entity.id.toString)
      .source(newJson.toString)
      .version(entity.version)

    // Call synchronously, useful for debugging: proper stack trace is reported. TODO make config flag.       
    //      val response = client.execute(IndexAction.INSTANCE, request).get
    //      Future.successful(Versioned(entity, response.getVersion.toString))
    preMutate(entity).flatMap { entity: T =>
      client.execute(request).map(response => Versioned(entity, response.getVersion)) flatMap { mutatedEntity =>
        debug(s"Update ${index.name}/${t.name}/${entity.id}:\n${XContentHelper.convertToJson(request.source, true, true)}")
        postMutate(mutatedEntity.entity) map { _ => mutatedEntity }
      }
    }.recover(convertException { e =>
      debug(s"Update ${index.name}/${t.name}/${entity.id} failed: $e:\n${XContentHelper.convertToJson(request.source, true, true)}")
      throw e
    })
  }
  
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

  def delete(id: Id[T], refreshAfterMutation: Boolean): Future[Unit] =
    retrieve(id) map (entity => {
      val request = new DeleteRequest(index.name, t.name, id.toString).refresh(refreshAfterMutation)
      (entity match {
        case None => Future.successful()
        case Some(e) => preMutate(e)
      }) flatMap { _ =>
        client.execute(request) map {
          response =>
            if (!response.isFound()) {
              throw new IdNotFoundException(id.toString, t.name)
            }
        }
      } flatMap { _ =>
        entity match {
          case None => Future.successful()
          case Some(e) => preMutate(e)
        }
      }
    })

//  def query(queryBuilder: QueryBuilder, page: Page): Future[SearchResult[T]] = {
//    val request = client.javaClient.prepareSearch(index.name).setTypes(t.name).setQuery(queryBuilder).addSort("_id", SortOrder.ASC).setFrom(page.from).setSize(page.size).setVersion(true).request
//    client.execute(request).map { result =>
//      SearchResult(result.getHits.hits.toList.map { hit =>
//        val json = hit.sourceAsString.asJson
//        Versioned(json.convertFromES(mapping), hit.getVersion)
//      }, Some(result.getHits.getTotalHits))
//    }
//  }
//
//  def queryWithString(queryStr: String, page: Page): Future[SearchResult[T]] =
//    query(new QueryStringQueryBuilder(queryStr), page)

  protected def doSearch(filter: FilterBuilder): Future[List[Versioned[T]]] = {
    val request = client.javaClient.prepareSearch(index.name).addSort("_id", SortOrder.ASC).setTypes(t.name).setPostFilter(filter).setVersion(true).request
    client.execute(request).map(_.getHits.hits.toList.map { hit =>
      val json = hit.sourceAsString.asJson
      Versioned(mapping.jsonFormat.read(json), hit.getVersion)
    })
  }

//  protected def retrieve(filter: FilterBuilder, error: => String): Future[Option[Versioned[T]]] = {
//    doSearch(filter).map {
//      case Nil => None
//      case entity :: Nil => Some(entity)
//      case entities => throw new Exception(error)
//    }
//  }

  def retrieveAll: Future[List[Versioned[T]]] =
    search(all, page = Page.all).map(_.result)


  def onChange(f: Id[T] => Unit) {
    index.bus.subscribe {
      case ESRootDaoMutationEvent(n, id) if n == eventName =>
        f(Id[T](id))
    }
  }

  protected val eventName = tag.runtimeClass.getName.replaceAllLiterally("$", "") + "_" + index.name + "_" + t.name

  protected def preMutate(entity: T): Future[T] =
    Future.successful(entity)

  protected def postMutate(entity: T): Future[Unit] =
    Future.successful(index.bus.publish(ESRootDaoMutationEvent(eventName, entity.id.toString)))

  implicit def toResults(f: Future[SearchResult[T]]) = ESRootDao.toResults(f)
}

case class ESRootDaoMutationEvent(eventName: String, id: String) extends BusEvent
