package controllers

import api.{GamesService, UserService}
import com.google.inject.Inject
import models._
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import play.api.routing._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future


class MainController @Inject()(val gamesService: GamesService,
                               val usersService: UserService,
                               val messagesApi: MessagesApi)
  extends Controller with I18nSupport {

  import MainController._


  def settings = Action.async { implicit request =>
    val mail = request.session.get("mail").get

    usersService.getByEmail(mail) flatMap {
      case None =>
        val usr = User(mail, List())
        usersService.registerUser(usr) map {
          case _ => Ok(views.html.settings(mail, List()))
        }
      case Some(usr) =>
        Future {
          Ok(views.html.settings(mail, usr.wishList))
        }
    }

  }

  def login = Action { implicit request =>
    Ok(views.html.login(userForm))
  }

  def logout = Action { implicit request =>
    Redirect(routes.MainController.login()).withNewSession.flashing(
      "success" -> "You've been logged out!"
    )
  }

  def authenticate = Action { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(userForm)),
      user => Redirect(routes.MainController.settings()).withSession("mail" -> user.mail)
    )
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.MainController.searchResult,
        routes.javascript.MainController.subscribe,
        routes.javascript.MainController.unsub
      )
    ).as("text/javascript")
  }


  def searchResult(text: String) = Action.async { implicit request =>
    gamesService.search(text) map {
      list: List[Game] =>
        Logger.debug(s"Got results: ${list.length} games...")
        val games = list flatMap { game: Game =>
          game.pricesPerShop map {
            case (shop, pricesList) =>
              val newestPrice = pricesList.max(new Ordering[PriceEntry] {
                override def compare(x: PriceEntry, y: PriceEntry): Int = x.date.compareTo(y.date)
              })
              (game, shop, newestPrice)
          }
        }
        val gamesToSend = games.filterNot({
          case(_, _, priceEntry) => priceEntry.price.value == 0
        }).sortBy({
          case (_, _, priceEntry) => priceEntry.price.value
        })
        Ok(JsArray(gamesToSend map {
          case (game, shop, priceEntry) =>
            Json.obj(
              "name" -> game.name,
              "shop" -> shop,
              "price" -> priceEntry.price.value.toString
            )
        }))
    }
  }

  def subscribe(text: String, price: String) = Action.async { implicit request =>
    Logger.debug(s"Subscribe: $text, $price")
    val mail = request.session.get("mail").get
    val money = Money(BigDecimal(price), "PLN")
    val wish = Wish(text, money)
    usersService.getByEmail(mail) flatMap {
      case None =>
        Future {
          Ok("Ok")
        }
      case Some(usr) =>
        val newUser = User(mail, wish :: usr.wishList)
        Logger.debug(s"User: $newUser")
        usersService.updateUser(newUser) map {
          case _ => Ok("Ok")
        }
    }
  }

  def unsub(game: String, value: String, currency: String) = Action.async { implicit request =>
    val mail = request.session.get("mail").get
    usersService.getByEmail(mail) flatMap {
      case None => Future {
        Ok("Ok")
      }
      case Some(usr) =>
        val money = Money(BigDecimal(value), currency)
        val wish = Wish(game, money)
        val newWishList = usr.wishList.filterNot(x => x == wish)
        val newUser = User(mail, newWishList)
        usersService.updateUser(newUser) map {
          case _ => Ok("Ok")
        }
    }
  }

}

object MainController {

  case class UserMail(mail: String)

  case class SearchString(text: String)

  case class GameSubscription(gameName: String, vendorName: String, price: Int)

  val userForm = Form(
    mapping(
      "mail" -> email
    )(UserMail.apply)(UserMail.unapply)
  )

  val searchForm = Form(
    mapping(
      "gamequery" -> text
    )(SearchString.apply)(SearchString.unapply)
  )

  val subscribeGameForm = Form(
    mapping(
      "game" -> text,
      "vendor" -> text,
      "price" -> number
    )(GameSubscription.apply)(GameSubscription.unapply)
  )
}