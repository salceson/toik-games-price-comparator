package databus

import java.time.LocalDateTime

import api._
import com.google.inject.Inject
import databus.mongoconnection.MongoConnectivity
import models._
import play.api.Logger
import play.api.libs.json.OFormat
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.Future

class GamesServiceImpl @Inject()(val shopService: ShopService,
                                 val reactiveMongoApi: ReactiveMongoApi)
  extends GamesService with MongoConnectivity[Game] {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  override def collectionName: String = "games"

  override def idFieldName: String = "name"

  override implicit def formats: OFormat[Game] = JSONHelpers.gameFormat

  override def search(query: String): Future[List[Game]] =
    shopService.getGames(query) flatMap { games: List[Game] =>
      Logger.debug(s"Got games: ${games.length}")
      val updatedGames = games map { gameFromShop: Game =>
        mongoConnection.get(gameFromShop.name) flatMap {
          case Some(gameFromMongo) =>
            Logger.debug(s"Game ${gameFromShop.name} is in mongo")
            val updatedGame = updateGame(gameFromMongo, gameFromShop.pricesPerShop)
            mongoConnection.update(updatedGame) map { result =>
              updatedGame
            }
          case None =>
            Logger.debug(s"Game ${gameFromShop.name} is not  in mongo")
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
          shopService.getGames(gameName) flatMap { (games: List[Game]) =>
            val gameFromMongoOption = games.find((g: Game) => g.name == gameName)
            gameFromMongoOption match {
              case None =>
                Logger.warn(s"No entries for $gameName but earlier $gameName was found!")
                Future(gameOption)
              case Some(gameFromMongo) =>
                val newPrices = gameFromMongo.pricesPerShop
                val newGame = updateGame(gameFromMongo, newPrices)
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
