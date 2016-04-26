package databus

import api._
import com.google.inject.Inject
import databus.mongoconnection.MongoConnectivity
import models._
import play.api.Application
import play.api.libs.json.{Json, OFormat}

import scala.concurrent.Future

class UsersServiceImpl @Inject()(override val application: Application)
  extends UserService
    with MongoConnectivity[User] {

  override def idFieldName: String = "email"

  override def collectionName: String = "users"

  override implicit def formats: OFormat[User] = JSONHelpers.userFormat

  override def getByEmail(email: String): Future[Option[User]] =
    mongoConnection.get(email)

  override def getUsers: Future[List[User]] =
    mongoConnection.find(Json.obj())

  override def updateUser(email: String, newUser: User): Future[Boolean] =
    mongoConnection.update(newUser)
}
