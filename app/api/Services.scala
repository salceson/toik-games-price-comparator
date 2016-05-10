package api

import models._

import scala.concurrent.Future

trait GamesService {
  def search(query: String): Future[List[Game]]

  def get(gameName: String): Future[Option[Game]]
}

trait UserService {
  def getByEmail(email: String): Future[Option[User]]

  def updateUser(newUser: User): Future[Boolean]

  def getUsers: Future[List[User]]

  def registerUser(newUser: User): Future[Boolean]
}

trait ShopService {
  def getGames(query: String): Future[List[Game]]
}
