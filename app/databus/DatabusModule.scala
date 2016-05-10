package databus

import api._
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class DatabusModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    val usersServiceBinding = bind[UserService].to[UsersServiceImpl]
    val gamesServiceBinding = bind[GamesService].to[GamesServiceImpl]
  }
}
