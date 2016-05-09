package shops

import api.ShopService
import com.google.inject.Inject
import models.Game

import scala.concurrent.Future
import scala.collection.immutable

class ShopServiceImpl @Inject() (shopScrapers: immutable.Set[ShopScraper]) extends ShopService {
  import scala.concurrent.ExecutionContext.Implicits.global
  override def getGames(query: String): Future[List[Game]] = {
    var results: List[Game] = List()
    shopScrapers.foreach{scraper => results = results ++ scraper.getGames(query)}
    Future { results }
  }

//  override def getPrices(gameName: String): Future[Map[String, PriceEntry]] = {
//    null
//  }

}
