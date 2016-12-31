import sbt.Keys._

isSnapshot := true

val scalaSettings = Seq(
  crossScalaVersions := Seq("2.10.6", "2.11.7")
)

val projectSettings = Seq(
  name := "config-generator",
  organization := "com.stackstate",
  version := {
    import scala.collection.JavaConversions._
    val git = new org.eclipse.jgit.api.Git(new org.eclipse.jgit.storage.file.FileRepositoryBuilder().findGitDir(baseDirectory.value).build)
    sys.env.getOrElse("bamboo_repository_git_branch", git.getRepository.getBranch).toLowerCase + "-" + git.log().call().toList.length + "-" + git.getRepository.resolve("HEAD").abbreviate(7).name()
  },
  publishTo := Some("Artifactory Realm" at "http://52.48.46.185/artifactory/libs"),
  credentials += Credentials(Path.userHome / ".sbt" / "artifactory.credentials")
)

val dependencies = {
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-core" % "7.2.7",
    "com.typesafe" % "config" % "1.3.0"
  )
}

scalaSource in Compile := baseDirectory.value / "src"

lazy val root = project.in(file("."))
  .settings(projectSettings:_*)
  .settings(scalaSettings:_*)
  .settings(dependencies)
  .settings(compileOrder := CompileOrder.Mixed)
