organization := "com.github.dnvriend"

name := "elastic-searching"

version := "1.0.2"

scalaVersion := "2.11.7"

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-actor"                         % "2.3.12",
  "com.typesafe.akka"   %% "akka-http-spray-json-experimental"  % "1.0-RC4",
  "com.github.dnvriend" %% "akka-elasticsearch"                 % "1.0.4",
  "org.scalatest"       %% "scalatest"                          % "2.2.4" % Test
)

licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

parallelExecution := false

// enable scala code formatting //
import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
    "scala" -> Apache2_0("2015", "Dennis Vriend"),
    "conf" -> Apache2_0("2015", "Dennis Vriend", "#")
)

// enable plugins //
lazy val root = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)