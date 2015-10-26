import sbt.Keys._



libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.4"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.11.7"


val scalaSettings = Seq(
  scalaVersion := "2.11.7"
  //, scalacOptions += "-Ymacro-debug-lite"
)

val projectSettings = Seq(
  name := "config-generator",
  organization := "com.stackstate",
  version := "1.1"
)

val dependencies = {
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-core" % "7.1.4",
    "com.typesafe" % "config" % "1.3.0",
    "org.scala-lang" % "scala-compiler" % "2.11.7"
  )
}

scalaSource in Compile := baseDirectory.value / "src"

lazy val root = project.in(file("."))
  .settings(projectSettings:_*)
  .settings(scalaSettings:_*)
  .settings(dependencies)
  .settings(compileOrder := CompileOrder.Mixed)