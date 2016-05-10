package shops

import java.time.LocalDateTime

import com.google.inject.Inject
import models.{Game, Money, PriceEntry}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import org.jsoup.Jsoup
import play.api.libs.json.Json

import scala.math.BigDecimal.RoundingMode
import scala.util.parsing.json.JSON
import scalaj.http.{Http, HttpResponse}

trait ShopScraper {
  var url : String
  val browser = JsoupBrowser()
  @Inject var euroToPln : Double = _
  def asSearchUrl: String => String = url + _
  def getGames(query: String): List[Game]
  def priceStrToPriceEntry(_price: String) : PriceEntry = {
    val pattern = """(\d+,*\d*).*""".r
    var price = "0.00"
    pattern.findAllIn(_price).matchData foreach {
      m => {
        price = m.group(1).replace(",", ".")
        if (!_price.contains("zł")) price = (price.toDouble * euroToPln).toString
      };
    }
    new PriceEntry(LocalDateTime.now(), new Money(BigDecimal(price).setScale(2, RoundingMode.HALF_UP), "PLN"))
  }

  def shortenUrl(url: String) : String = {
     url.length() > 90  match {
       case false =>  url
       case true =>
         val apiKey = "R_e29aa9c6e2144e19a52df028f146c899"
         val login = "test508"
         val response: HttpResponse[String] = Http("https://api-ssl.bitly.com/v3/shorten")
           .params(Seq("apiKey" -> apiKey, "login" -> login, "longUrl" -> url)).asString
         val json = JSON.parseFull(response.body)
         json match {
           case _map : Some[Map[String, Any]] => _map.get("data").asInstanceOf[Map[String,_]]("url").asInstanceOf[String]
           case None => url
         }
     }
  }
}

class SteamScraper extends ShopScraper {
  override var url = "http://store.steampowered.com/search/?snr=1_4_4__12&term="
  override def getGames(query: String): List[Game] = {
    val doc = browser.get(asSearchUrl(query))
    val searchItems: List[Element] = doc >> elementList(".search_result_row")
    val itemTitles: List[(String, String, String)] = searchItems.map(_ >> (text(".title"), text(".search_price"),
      attr("src")("img")))
    var result: List[Game] = List()
    itemTitles.foreach{case (title, price, imgUrl) => result ++= List(new Game(title, shortenUrl(imgUrl),
      Map("steam" -> List(priceStrToPriceEntry(price))), LocalDateTime.now() ))}
    result
  }
}

class OriginScraper  extends ShopScraper {
  override var url = "https://www.origin.com/pl-pl/store/browse?q="
  override def getGames(query: String): List[Game] = {
    val doc = browser.get(asSearchUrl(query))
    val searchItems: List[Element] = doc >> elementList(".background-gradient")
    val priceInfo: List[Element] = doc >> elementList(".price")
    val prices: List[String] = priceInfo.map(_ >> attr("data-defaultprice")("span"))
    val items: List[(String, String)] = searchItems.map(_ >> (text("h5 a"), attr("src")("a img")))

    for ( (item, price) <- items zip prices)
      yield new Game(item._1, shortenUrl(item._2), Map("origin" -> List(priceStrToPriceEntry(price))), LocalDateTime
        .now())

  }
}

class GOGScraper extends ShopScraper {
  override var url = "https://www.gog.com/games/ajax/filtered?mediaType=game&search="

  override def getGames(query: String): List[Game] = {
    val doc = Jsoup.connect(url + query).ignoreContentType(true).execute().body()
    val products = Json.parse(doc) \ "products"

    val titles = (products \\ "title").map(title => title.as[String])
    val prices = (products \\ "price").map(price => s"${(price \ "finalAmount").as[String]}zł")
    val imageUrls = (products \\ "image").map(imageUrl => shortenUrl("http:" + imageUrl.as[String] + ".jpg"))

    for (item <- (titles, imageUrls, prices).zipped.toList)
      yield new Game(item._1, item._2, Map("gog" -> List(priceStrToPriceEntry(item._3))), LocalDateTime.now())
  }
}




