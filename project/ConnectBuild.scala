import sbt._
import Keys._

object BuildSettings {
  val buildOrganization = "scalasec"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.9.1"

  import Resolvers._

  val getJars = TaskKey[Unit]("get-jars")
  val getJarsTask = getJars <<= (target, managedClasspath in Runtime) map { (target, cp) =>
    println("Target path is: " + target)
    println("Full classpath is: " + cp.map(_.data).mkString(":"))
  }

  val buildSettings = Defaults.defaultSettings ++ Seq (
    resolvers ++= Seq(mavenLocalRepo, springSnapshotRepo),
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion,
    getJarsTask
  )
}

object Resolvers {
  val mavenLocalRepo = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
  val springSnapshotRepo = "Spring Snapshot Repo" at "http://maven.springframework.org/snapshot"
}

object Dependencies {
  val springSecurityVersion = "3.1.0.CI-SNAPSHOT"
  val springSecurityOAuthVersion = "1.0.0.BUILD-SNAPSHOT"
  val logbackVersion = "0.9.28"
  val slf4jVersion   = "1.6.1"

  def springSecurity(name: String) = "org.springframework.security" % "spring-security-%s".format(name) % springSecurityVersion

  val springSecurityCore = springSecurity("core")
  val springSecurityWeb = springSecurity("web")
  val springSecurityConfig = springSecurity("config")

  val springSecurityOauth2 = "org.springframework.security.oauth" % "spring-security-oauth2" % springSecurityOAuthVersion

  val servletapi = "javax.servlet" % "servlet-api" % "2.5"

  val scalaTest  = "org.scalatest" %% "scalatest" % "1.6.1" % "test->default"
  val mockito    = "org.mockito" % "mockito-all" % "1.8.5" % "test->default"
  val junit      = "junit" % "junit" % "4.8.2" % "test"

  val jetty6     = "org.mortbay.jetty" % "jetty" % "6.1.26" % "jetty"
  val jetty7     = "org.eclipse.jetty" % "jetty-webapp" % "7.5.0.v20110901" % "jetty"

  val slf4j      = "org.slf4j" % "slf4j-api" % slf4jVersion
  val logback    = "ch.qos.logback" % "logback-classic" % logbackVersion % "runtime->default"
  val jcl        = "org.slf4j" %  "jcl-over-slf4j" % slf4jVersion % "runtime->default"

  val cglib      = "cglib" % "cglib-nodep" % "2.2.2" % "runtime->default"

  val lift_json  = "net.liftweb" %% "lift-json" % "2.4-M4"
}

object ConnectBuild extends Build {
  import Dependencies._
  import BuildSettings._

  val springSecDeps = Seq(springSecurityCore, springSecurityWeb, springSecurityConfig, springSecurityOauth2)
  val testDeps = Seq(junit, scalaTest, mockito)
  val loggingDeps = Seq(slf4j, jcl, logback)

  lazy val connect = Project("connect",
    file("."),
    settings = buildSettings
  ) aggregate (jwt)

  lazy val jwt = Project("jwt",
    file("jwt"),
    settings = buildSettings ++ Seq (
      libraryDependencies ++= testDeps ++ loggingDeps ++ Seq(lift_json)
    )
  )

}
