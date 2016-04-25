package databus

import java.time.LocalDateTime

import api._
import com.google.inject.Inject
import databus.mongoconnection.MongoConnectivity
import models._
import play.api.libs.json.{Json, OFormat}
import play.api.{Application, Logger}

import scala.concurrent.Future

class GamesServiceImpl @Inject()(val shopService: ShopService,
                                 override val application: Application)
  extends GamesService
    with MongoConnectivity[Game] {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  val logger = Logger(getClass)

  override def collectionName: String = "games"

  override def idFieldName: String = "name"

  override implicit def formats: OFormat[Game] = JSONHelpers.gameFormat

  override def search(query: String): Future[List[Game]] =
    mongoConnection.find(Json.obj("name" -> query))

  override def get(gameName: String): Future[Option[Game]] =
    mongoConnection.get(gameName) flatMap {
      case gameOption@Some(game) =>
        if (game.lastUpdate.isAfter(LocalDateTime.now().minusHours(1))) {
          Future(gameOption)
        } else {
          shopService.getPrices(gameName) flatMap { (newPrices: Map[String, PriceEntry]) =>
            if (newPrices.isEmpty) {
              logger.warn(s"No entries for $gameName but earlier $gameName was found!")
              Future(gameOption)
            } else {
              val updatedPrices = newPrices.foldLeft(game.pricesPerShop) {
                case (current, (shop, newPrice)) =>
                  current.updated(shop, newPrice :: current.getOrElse(shop, List()))
              }
              val newGame = game.copy(pricesPerShop = updatedPrices)
              mongoConnection.update(newGame) map {
                (_) => Some(newGame)
              }
            }
          }
        }
      case _ => Future.failed(new IllegalStateException("Please first search for the game, then get it."))
    }
}
