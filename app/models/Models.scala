package models

import java.time.LocalDateTime

import play.api.libs.json.Json

sealed abstract class MongoModel {
  def getId: String
}

case class Game(name: String, images: List[String],
                pricesPerShop: Map[String, List[PriceEntry]], lastUpdate: LocalDateTime)
  extends MongoModel {
  override def getId: String = name
}

case class PriceEntry(date: LocalDateTime, price: Money)

case class User(email: String, wishList: List[Wish])
  extends MongoModel {
  override def getId: String = email
}

case class Wish(gameName: String, priceThreshold: Money)

case class Money(value: BigDecimal, currency: String)

object JSONHelpers {
  implicit lazy val moneyFormat = Json.format[Money]
  implicit lazy val wishFormat = Json.format[Wish]
  implicit lazy val userFormat = Json.format[User]
  implicit lazy val priceEntryFormat = Json.format[PriceEntry]
  implicit lazy val gameFormat = Json.format[Game]
}
