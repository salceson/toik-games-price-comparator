package api

import models._

import scala.concurrent.Future

trait GamesService {
  def search(query: String): Future[List[Game]]

  def get(gameName: String): Future[Game]
}

trait UserService {
  def getByEmail(email: String): Future[User]

  def updateUser(email: String, newUser: User): Future[Unit]

  def getUsers: Future[List[User]]
}

trait ShopService {
  def getGames(query: String): Future[List[Game]]

  def getPrices(gameName: String): Future[Map[String, PriceEntry]]
}
