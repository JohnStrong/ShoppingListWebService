import sbt.Defaults

ThisBuild / scalaVersion := "3.3.7"   // use a recent stable Scala 3 version
ThisBuild / version := "0.1.0"
ThisBuild / organization := "org.myapps.shoppinglistservice"

lazy val FunctionalTest = config("functional") extend Test

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .configs(FunctionalTest)
  .settings(
    name := "SimpleShoppingListApp",
    libraryDependencies ++= Seq(
      guice,
      "org.scalatest" %% "scalatest" % "3.2.20" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.mockito" % "mockito-core" % "5.22.0" % Test
    ),
    // Functional test configuration - run with sbt functional:test
    inConfig(FunctionalTest)(Defaults.testSettings),
    FunctionalTest / sourceDirectory := baseDirectory.value / "functional-tests",
    FunctionalTest / scalaSource := baseDirectory.value / "functional-tests",
    FunctionalTest / resourceDirectory := baseDirectory.value / "functional-tests" / "resources"
  )