name := "GamesPriceComparator"

version := "1.0"

lazy val gamesPriceComparator = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  json,
  "org.reactivemongo" %% "reactivemongo" % "0.11.11",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.11",
  "org.reactivemongo" %% "reactivemongo-play-json" % "0.11.11",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.50.3",
  "net.ruippeixotog" %% "scala-scraper" % "1.0.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "net.codingwell" %% "scala-guice" % "4.0.1"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
