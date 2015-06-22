organization := "com.github.dnvriend"

name := "elastic-searching"

version := "1.0.1"

scalaVersion := "2.11.6"

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-actor"                     % "2.3.11",
  "io.spray"            %% "spray-json"                     % "1.3.2",
  "com.github.dnvriend" %% "akka-elasticsearch"             % "1.0.3",
  "org.scalatest"       %% "scalatest"                      % "2.1.4" % "test"
)
