package models

import java.time.LocalDateTime

case class Game(name: String, description: String, images: List[String],
                pricesPerShop: Map[String, List[PriceEntry]], lastUpdate: LocalDateTime)

case class PriceEntry(date: LocalDateTime, price: Money)

case class User(email: String, wishList: List[Wish])

case class Wish(gameName: String, priceThreshold: Money)

case class Money(value: BigDecimal, currency: String)
