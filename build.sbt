organization := "com.github.dnvriend"

name := "elastic-searching"

version := "1.0.0"

scalaVersion := "2.11.2"

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

libraryDependencies ++=
  {
    val akkaV = "2.3.6"
    val sprayV = "1.3.1"
    val jsonV = "1.2.6"
    Seq(
      "org.scala-lang"       % "scala-library"                  % scalaVersion.value,
      "com.typesafe.akka"   %% "akka-kernel"                    % akkaV,
      "com.typesafe.akka"   %% "akka-actor"                     % akkaV,
      "com.typesafe.akka"   %% "akka-slf4j"                     % akkaV,
      "com.typesafe.akka"   %% "akka-cluster"                   % akkaV,
      "com.typesafe.akka"   %% "akka-contrib"                   % akkaV,
      "com.typesafe.akka"   %% "akka-persistence-experimental"  % akkaV,
      "io.spray"            %% "spray-http"                     % sprayV,
      "io.spray"            %% "spray-httpx"                    % sprayV,
      "io.spray"            %% "spray-routing-shapeless2"       % sprayV,
      "io.spray"            %% "spray-util"                     % sprayV,
      "io.spray"            %% "spray-io"                       % sprayV,
      "io.spray"            %% "spray-can"                      % sprayV,
      "io.spray"            %% "spray-client"                   % sprayV,
      "io.spray"            %% "spray-json"                     % jsonV,
      "ch.qos.logback"       % "logback-classic"                % "1.1.2",
      "com.github.dnvriend" %% "akka-elasticsearch"             % "1.0.0",
      "com.github.dnvriend" %% "akka-persistence-inmemory"      % "0.0.2" % "test",
      "com.typesafe.akka"   %% "akka-testkit"                   % akkaV   % "test",
      "io.spray"            %% "spray-testkit"                  % sprayV  % "test",
      "org.scalatest"       %% "scalatest"                      % "2.1.4" % "test"
    )
  }

autoCompilerPlugins := true

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

publishMavenStyle := true

publishArtifact in Test := false

fork := true

net.virtualvoid.sbt.graph.Plugin.graphSettings