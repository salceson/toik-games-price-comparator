package utils

import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments
import reactivemongo.api.{MongoConnection, MongoDriver}

trait EmbeddedConnection extends Specification {
  val Port = 12345
  private val starter = MongodStarter.getDefaultInstance
  private val mongodConfig = new MongodConfigBuilder()
    .version(Version.Main.PRODUCTION)
    .net(new Net(Port, Network.localhostIsIPv6()))
    .build()
  private val mongodExecutable = starter.prepare(mongodConfig)

  override def map(fs: => Fragments) = startMongo ^ fs ^ stopMongo

  private def startMongo = step {
    mongodExecutable.start
    Thread.sleep(2000)
    success
  }

  private def stopMongo() = step {
    Thread.sleep(10000)
    mongodExecutable.stop()
    success
  }

  def getConnection: MongoConnection = {
    Thread.sleep(2000)
    val driver = new MongoDriver
    val connection = driver.connection("localhost:12345" :: Nil)
    Thread.sleep(2000)
    connection
  }
}
