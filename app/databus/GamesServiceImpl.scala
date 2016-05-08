package databus

import java.time.LocalDateTime

import api._
import com.google.inject.{Inject, Singleton}
import databus.mongoconnection.MongoConnectivity
import models._
import play.api.libs.json.OFormat
import play.api.{Application, Logger}

import scala.concurrent.Future

@Singleton
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
    shopService.getGames(query) flatMap { games: List[Game] =>
      val updatedGames = games map { gameFromShop: Game =>
        mongoConnection.get(gameFromShop.name) flatMap {
          case Some(gameFromMongo) =>
            val updatedGame = updateGame(gameFromMongo, gameFromShop.pricesPerShop)
            mongoConnection.update(updatedGame) map { result =>
              updatedGame
            }
          case None =>
            mongoConnection.save(gameFromShop) map { result =>
              gameFromShop
            }
        }
      }
      Future.sequence(updatedGames)
    }

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
              val newGame = updateGame(game, newPrices mapValues { pe: PriceEntry => List(pe) })
              mongoConnection.update(newGame) map {
                (_) => Some(newGame)
              }
            }
          }
        }
      case _ => Future(None)
    }

  private def updateGame(oldGame: Game, newData: Map[String, List[PriceEntry]]): Game = {
    val updatedPrices = newData.foldLeft(oldGame.pricesPerShop) {
      case (current, (shop, newPrice)) =>
        current.updated(shop, newPrice ++ current.getOrElse(shop, List()))
    }
    oldGame.copy(pricesPerShop = updatedPrices)
  }
}
