package databus.mongoconnection

import com.typesafe.config.ConfigFactory
import models.MongoModel
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{MongoConnection => RMongoConnection}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import play.api.Logger

import scala.concurrent.Future

trait MongoConnection[T <: MongoModel] {
  def get(id: String): Future[Option[T]]

  def find(query: JsObject): Future[List[T]]

  def update(obj: T): Future[Boolean]

  def save(obj: T): Future[Boolean]
}

class MongoConnectionImpl[T <: MongoModel](collectionName: String, idFieldName: String)
                                          (implicit formats: OFormat[T], connection: RMongoConnection)
  extends MongoConnection[T] {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  val configuration = ConfigFactory.load()
  val dbName = configuration.getString("mongodb.db")
  val collection = connection(dbName).collection[JSONCollection](collectionName)

  override def get(id: String): Future[Option[T]] = {
    collection.find(Json.obj(idFieldName -> id)).cursor[T]().collect[List]().map {
      l: List[T] => l.isEmpty match {
        case false => Some(l.head)
        case true => None
      }
    }
  }

  override def find(query: JsObject): Future[List[T]] =
    collection.find(query).cursor[T]().collect[List]()

  override def update(obj: T): Future[Boolean] = {
    Logger.debug(s"Updating $obj...")
    collection.update(Json.obj(idFieldName -> obj.getId), obj).map { result =>
      Logger.debug(s"Update ok: ${result.ok}, errors: ${result.errmsg}")
      result.ok
    }
  }

  override def save(obj: T): Future[Boolean] =
    collection.insert[T](obj).map { result =>
      Logger.debug(s"Saving ok: ${result.ok}, errors: ${result.errmsg}")
      result.ok
    }
}

trait MongoConnectivity[T <: MongoModel] {
  implicit def formats: OFormat[T]

  val reactiveMongoApi: ReactiveMongoApi

  private implicit val connection = reactiveMongoApi.connection

  def collectionName: String

  def idFieldName: String

  val mongoConnection: MongoConnection[T] =
    new MongoConnectionImpl[T](collectionName, idFieldName)
}
