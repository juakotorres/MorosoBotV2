name := "MorosoBot"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "info.mukel" %% "telegrambot4s" % "3.0.14"

libraryDependencies += "org.scalikejdbc" %% "scalikejdbc" % "3.1.0"

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.7.2"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"

mainClass in (Compile, run) := Some("Main")

assemblyMergeStrategy in assembly := {
    case PathList("reference.conf") => MergeStrategy.concat
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
}