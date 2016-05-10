package shops

import api.ShopService
import com.google.inject.Inject
import models.Game

import scala.concurrent.Future
import scala.collection.immutable

import play.api.Logger

class ShopServiceImpl @Inject()(shopScrapers: immutable.Set[ShopScraper]) extends ShopService {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def getGames(query: String): Future[List[Game]] = {
    Future {
      Logger.info(s"Scrapping games for $query...")
      shopScrapers.foldLeft(List[Game]()) {
        case (results, scraper) =>
          val games: List[Game] = scraper.getGames(query)
          Logger.info(s"Scrapped $scraper, got ${games.length} games.")
          results ++ games
      }
    }
  }
}
