package com.busymachines.commons.elasticsearch

import spray.json.JsonFormat
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.client.Client
import com.busymachines.commons.dao.NestedDao
import com.busymachines.commons.domain.HasId
import com.busymachines.commons.domain.Id
import com.busymachines.commons.dao.Versioned
import com.busymachines.commons.dao.IdAlreadyExistsException
import com.busymachines.commons.dao.NonExistentEntityException

abstract class ESNestedDao[P <: HasId[P], T <: HasId[T] : JsonFormat](implicit ec: ExecutionContext) extends ESDao[T] with NestedDao[P, T] {

  protected def parentDao : ESDao[P]

  protected def findEntity(parent : P, id : Id[T]) : Option[T]
  protected def createEntity(parent : P, entity : T) : P
  protected def modifyEntity(parent : P, id : Id[T], found : Found, modify : T => T) : P
  protected def deleteEntity(parent : P, id : Id[T], found : Found) : P
  
  protected class Found {
    var entity : Option[T] = None
    def apply(t : T) : T = { entity = Some(t); t }
    def apply[A](t : T, a : A) : A = { entity = Some(t); a }
  }
  
  def retrieve(id: Id[T]): Future[Option[Versioned[T]]] = {
    retrieveParent(id) map {
      case Some(Versioned(parent, version)) =>
        findEntity(parent, id).map(Versioned(_, version))
      case None =>
        None
    }
  }

  def create(id : Id[P], entity: T, refreshAfterMutation : Boolean): Future[Versioned[T]] = {
    retrieve(entity.id) flatMap {
      case Some(Versioned(entity, _)) => 
        throw new IdAlreadyExistsException(entity.id.toString, typeName)
      case None =>
	    parentDao.retrieve(id) flatMap {
	      case Some(Versioned(parent, version)) =>
	        val modifiedParent = createEntity(parent, entity)
	        parentDao.update(Versioned(modifiedParent, version), refreshAfterMutation) map {
	          case Versioned(parent, version) => Versioned(entity, version)
	        }
	      case None =>
	        throw new NonExistentEntityException(id.toString, parentDao.typeName)   
	    }
	  }
  }
  
  def modify(id : Id[T], refreshAfterMutation : Boolean)(f : T => T) : Future[Versioned[T]] = {
    retrieveParent(id) flatMap {
      case None => throw new NonExistentEntityException(id.toString, typeName)
      case Some(Versioned(parent, version)) =>
        val found = new Found
        val modifiedParent = modifyEntity(parent, id, found, f)
        found.entity match {
          case Some(entity) =>
            parentDao.update(Versioned(modifiedParent, version), refreshAfterMutation)
            	.map(_.copy(entity = entity))
          case None =>
            throw new NonExistentEntityException(id.toString, typeName)
        }
    }
  }
  
  def update(entity: Versioned[T], refreshAfterMutation : Boolean): Future[Versioned[T]] = {
    retrieveParent(entity.entity.id) flatMap {
      case None => throw new NonExistentEntityException(entity.entity.id.toString, typeName)
      case Some(Versioned(parent, _)) =>
        val found = new Found
        val modifiedParent = modifyEntity(parent, entity.entity.id, found, _ => entity.entity)
        found.entity match {
          case Some(modifiedEntity) =>
          	parentDao.update(Versioned(modifiedParent, entity.version), refreshAfterMutation).map(_.copy(entity = modifiedEntity))
          case None =>
          	throw new NonExistentEntityException(entity.entity.id.toString, typeName)
        }
    }
  }

  def delete(id : Id[T], refreshAfterMutation : Boolean) : Future[Unit] = {
    retrieveParent(id) flatMap {
      case None => throw new NonExistentEntityException(id.toString, typeName)
      case Some(Versioned(parent, version)) =>
        val found = new Found
        val modifiedParent = deleteEntity(parent, id, found)
        found.entity match {
          case Some(_) =>
          	parentDao.update(Versioned(modifiedParent, version), refreshAfterMutation).map(_ => Unit)
          case None =>
          	throw new NonExistentEntityException(id.toString, typeName)
        }
    }
  }
}
