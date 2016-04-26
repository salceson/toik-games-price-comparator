package databus.mongoconnection

import java.time.LocalDateTime

import models._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.test.WithApplication
import utils.EmbeddedConnection

import scala.concurrent.duration._

class MongoConnectionImplSpec(implicit ee: ExecutionEnv) extends Specification with EmbeddedConnection {
  sequential

  import JSONHelpers.gameFormat

  "Mongo connection" should {
    "provide basic methods" in new WithApplication {
      val testGame1 = Game(
        name = "Test",
        description = "Test Game",
        images = List(),
        pricesPerShop = Map(
          "Steam" -> List(
            PriceEntry(
              LocalDateTime.now(),
              Money(200, "PLN")
            )
          )
        ),
        lastUpdate = LocalDateTime.now()
      )
      val testGame2 = Game(
        name = "Test",
        description = "Test Game",
        images = List(),
        pricesPerShop = Map(
          "Steam" -> List(
            PriceEntry(
              LocalDateTime.now(),
              Money(200, "PLN")
            )
          )
        ),
        lastUpdate = LocalDateTime.now()
      )
      implicit val connection = getConnection
      val instance = new MongoConnectionImpl[Game]("test", "name")
      instance.save(testGame1) must beTrue.awaitFor(5.seconds)
      instance.save(testGame2) must beTrue.awaitFor(5.seconds)
      instance.get("Test") must beEqualTo[Option[Game]](Some(testGame1)).awaitFor(5.seconds)
      instance.find(Json.obj("description" -> "Test Game")) must haveSize[List[Game]](2).awaitFor(5.seconds)
      instance.update(testGame2.copy(description = "Nope")) must beTrue.awaitFor(5.seconds)
      instance.find(Json.obj("description" -> "Test Game")) must haveSize[List[Game]](1).awaitFor(5.seconds)
    }
  }
}
