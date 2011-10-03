import sbt._
import Keys._
import com.github.siasia.WebPlugin._

object BuildSettings {
  import Resolvers._

  val getJars = TaskKey[Unit]("get-jars")
  val getJarsTask = getJars <<= (target, managedClasspath in Runtime) map { (target, cp) =>
    println("Target path is: " + target)
    println("Full classpath is: " + cp.map(_.data).mkString(":"))
  }

  val buildSettings = Defaults.defaultSettings ++ Seq (
    scalaVersion := "2.9.1",
    organization := "connect",
    version      := "0.1-SNAPSHOT",
    resolvers ++= Seq(mavenLocalRepo, scalaToolsSnapshots),
    transitiveClassifiers := Seq("sources"),
    getJarsTask
//    publishTo := Some(localMaven)
  )
}

object Resolvers {
//  val localMaven = Resolver.file("my-test-repo", file("/Users/luke/test"))(Resolver.mavenStylePatterns)
  val mavenLocalRepo = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
  val scalaToolsSnapshots = "Scalatools Snaps" at "http://scala-tools.org/repo-snapshots/"
}

object Dependencies {
  val springSecurityVersion = "3.1.0.CI-SNAPSHOT"
  val springSecurityOAuthVersion = "1.0.0.BUILD-SNAPSHOT"
  val logbackVersion = "0.9.28"
  val slf4jVersion   = "1.6.1"

  val scalaTest  = "org.scalatest" %% "scalatest" % "1.6.1" % "test"
  val mockito    = "org.mockito" % "mockito-all" % "1.8.5" % "test"
  val junit      = "junit" % "junit" % "4.8.2" % "test"

  val jetty7     = "org.eclipse.jetty" % "jetty-webapp" % "7.5.0.v20110901" % "jetty"

  val slf4j      = "org.slf4j" % "slf4j-api" % slf4jVersion
  val logback    = "ch.qos.logback" % "logback-classic" % logbackVersion % "runtime"
  val jcl        = "org.slf4j" %  "jcl-over-slf4j" % slf4jVersion % "runtime"

  val lift_json  = "net.liftweb" %% "lift-json" % "2.4-M4"

  val ufversion = "0.5.1-SNAPSHOT"

  val ufDeps = Seq(
    "net.databinder" %% "unfiltered-filter" % ufversion,
    "net.databinder" %% "unfiltered-json" % ufversion intransitive(),
    "net.databinder" %% "unfiltered-oauth2" % ufversion,

    "net.databinder" %% "unfiltered-jetty" % ufversion % "runtime",
    "net.databinder" %% "unfiltered-spec" % ufversion % "test"
  )

  val dispatchVersion = "0.8.5"
  val dispatchDeps = Seq(
    "net.databinder" %% "dispatch-core" % dispatchVersion,
    "net.databinder" %% "dispatch-mime" % dispatchVersion,
    "net.databinder" %% "dispatch-http" % dispatchVersion,
    "net.databinder" %% "dispatch-json" % dispatchVersion,
    "net.databinder" %% "dispatch-lift-json" % dispatchVersion intransitive())

  val integrationTestDeps = dispatchDeps map { _ % "test" }
}

object ConnectBuild extends Build {
  import Dependencies._
  import BuildSettings._

  val testDeps = Seq(junit, scalaTest, mockito)
  val loggingDeps = Seq(slf4j, jcl, logback)

  lazy val connect = Project("connect",
    file("."),
    settings = buildSettings
  ) aggregate (jwt, server, client)

  lazy val jwt = Project("jwt",
    file("jwt"),
    settings = buildSettings ++ Seq (
      libraryDependencies ++= testDeps ++ loggingDeps ++ Seq(lift_json)
    )
  )

  lazy val server = Project("server",
    file("server"),
    settings = buildSettings ++ com.github.siasia.WebPlugin.webSettings ++ Seq(
      libraryDependencies ++= testDeps ++ loggingDeps ++ ufDeps ++ Seq(lift_json) ++ integrationTestDeps ++ Seq(jetty7),
      excludeFilter in prepareWebapp := ("jetty*" | "commons-*" | "dispatch*" | "httpclient*" | "servlet-api*" | "httpcore*")
    )
  ) dependsOn(jwt)

  lazy val client = Project("client",
    file("client"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= testDeps ++ loggingDeps ++ Seq(lift_json) ++ dispatchDeps ++ ufDeps
    )
  ) dependsOn(jwt)

}
