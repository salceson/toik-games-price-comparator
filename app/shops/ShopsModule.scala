package shops

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

class ShopsModule extends AbstractModule with ScalaModule  {
  def configure: Unit = {
    val scrapersBinding = ScalaMultibinder.newSetBinder(binder, classOf[ShopScraper])
    scrapersBinding.addBinding.toInstance(new OriginScraper)
    scrapersBinding.addBinding.toInstance(new SteamScraper)
  }
}
