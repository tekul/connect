import sbt._
import Keys._

object BuildSettings {
  val buildOrganization = "connect"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.9.1"

  import Resolvers._

  val getJars = TaskKey[Unit]("get-jars")
  val getJarsTask = getJars <<= (target, managedClasspath in Runtime) map { (target, cp) =>
    println("Target path is: " + target)
    println("Full classpath is: " + cp.map(_.data).mkString(":"))
  }

  val buildSettings = Defaults.defaultSettings ++ Seq (
    scalaVersion := buildScalaVersion,
    resolvers ++= Seq(mavenLocalRepo, scalaToolsSnapshots),
    transitiveClassifiers := Seq("sources"),
    organization := buildOrganization,
    version      := buildVersion,
    getJarsTask
  )
}

object Resolvers {
  val mavenLocalRepo = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
  val scalaToolsSnapshots = "Scalatools Snaps" at "http://scala-tools.org/repo-snapshots/"
//  val springSnapshotRepo = "Spring Snapshot Repo" at "http://maven.springframework.org/snapshot"
}

object Dependencies {
  val springSecurityVersion = "3.1.0.CI-SNAPSHOT"
  val springSecurityOAuthVersion = "1.0.0.BUILD-SNAPSHOT"
  val logbackVersion = "0.9.28"
  val slf4jVersion   = "1.6.1"

//  def springSecurity(name: String) = "org.springframework.security" % "spring-security-%s".format(name) % springSecurityVersion
//
//  val springSecurityCore = springSecurity("core")
//  val springSecurityWeb = springSecurity("web")
//  val springSecurityConfig = springSecurity("config")
//
//  val springSecurityOauth2 = "org.springframework.security.oauth" % "spring-security-oauth2" % springSecurityOAuthVersion

  val scalaTest  = "org.scalatest" %% "scalatest" % "1.6.1" % "test->default"
  val mockito    = "org.mockito" % "mockito-all" % "1.8.5" % "test->default"
  val junit      = "junit" % "junit" % "4.8.2" % "test"

//  val jetty6     = "org.mortbay.jetty" % "jetty" % "6.1.26" % "jetty"
//  val jetty7     = "org.eclipse.jetty" % "jetty-webapp" % "7.5.0.v20110901" % "jetty"

  val slf4j      = "org.slf4j" % "slf4j-api" % slf4jVersion
  val logback    = "ch.qos.logback" % "logback-classic" % logbackVersion % "runtime->default"
  val jcl        = "org.slf4j" %  "jcl-over-slf4j" % slf4jVersion % "runtime->default"

  val cglib      = "cglib" % "cglib-nodep" % "2.2.2" % "runtime->default"

  val lift_json  = "net.liftweb" %% "lift-json" % "2.4-SNAPSHOT"

  val ufversion = "0.5.0-OAUTH2"

  val ufDeps = Seq(
    "net.databinder" %% "unfiltered-filter" % ufversion,
    "net.databinder" %% "unfiltered-json" % ufversion intransitive(),
    "net.databinder" %% "unfiltered-oauth2" % ufversion,

  // Add jetty for compile time servletapi dep
    "net.databinder" %% "unfiltered-jetty" % ufversion,
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

//  val springSecDeps = Seq(springSecurityCore, springSecurityWeb, springSecurityConfig, springSecurityOauth2)
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
    settings = buildSettings ++ Seq(
      libraryDependencies ++= testDeps ++ loggingDeps ++ ufDeps ++ Seq(lift_json) ++ integrationTestDeps
    )
  ) dependsOn(jwt)

  lazy val client = Project("client",
    file("client"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= testDeps ++ loggingDeps ++ Seq(lift_json) ++ dispatchDeps ++ ufDeps
    )
  ) dependsOn(jwt)

}
