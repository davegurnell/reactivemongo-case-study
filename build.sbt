// Libraries ------------------------------------

val reactiveMongo: ModuleID =
  "org.reactivemongo" %% "reactivemongo" % "0.18.7"

val playReactiveMongo: ModuleID =
  "org.reactivemongo" %% "play2-reactivemongo" % "0.18.7-play27"

val logback: ModuleID =
  "ch.qos.logback" % "logback-classic" % "1.2.3"

val scalaCsv: ModuleID =
  "com.github.tototoshi" %% "scala-csv" % "1.3.6"

val cats: ModuleID =
  "org.typelevel" %% "cats-core" % "1.4.0"

// Common settings ------------------------------

def commonSettings(projectName: String) = {
  val baseSettings = Seq(
    name := projectName,
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.10"
  )

  val compilerOptions = Seq(scalacOptions ++= Seq(
    "-encoding", "UTF-8",   // source files are in UTF-8
    "-deprecation",         // warn about use of deprecated APIs
    "-unchecked",           // warn about unchecked type parameters
    "-feature",             // warn about misused language features
    "-language:higherKinds",// allow higher kinded types without `import scala.language.higherKinds`
    "-Xlint",               // enable handy linter warnings
    "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
  ))

  val compilerPlugins =
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)

  baseSettings ++ compilerOptions ++ compilerPlugins
}

// Projects -------------------------------------

lazy val snacks = project.in(file("snacks"))
  .settings(commonSettings("snacks") : _*)
  .settings(libraryDependencies ++= Seq(reactiveMongo, logback))

lazy val olympics = project.in(file("olympics"))
  .settings(commonSettings("olympics") : _*)
  .settings(libraryDependencies ++= Seq(reactiveMongo, logback, scalaCsv))

lazy val olympicsApi = project.in(file("olympicsApi"))
  .enablePlugins(PlayScala)
  .settings(commonSettings("olympicsApi") : _*)
  .settings(libraryDependencies ++= Seq(guice, playReactiveMongo))

lazy val root = project.in(file("."))
  .aggregate(snacks, olympics, olympicsApi)
