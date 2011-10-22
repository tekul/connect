package connect

import openid.OpenIDProvider
import unfiltered.jetty.Server
import unfiltered.request._
import unfiltered.oauth2._
import connect.util.ReplPlan
import unfiltered.filter.Plan
import unfiltered.response.Pass

class RequestDumper extends Plan {
  def intent = {
    case r =>
      println(r.underlying)
      Pass
  }
}

/**
 * Sample OpenID connect server, based on the OAuth2 sample at
 * https://github.com/softprops/unfiltered-oauth2-server.g8.git
 */
object ConnectServer {
  val resources = new java.net.URL(getClass.getResource("/web/robots.txt"), ".")
  val port = 8080
  val config = new ConnectComponentRegistry

  def main(args: Array[String]) {
    new java.util.Timer().schedule(new java.util.TimerTask() {
      def run() { unfiltered.util.Browser.open("http://localhost:%s/" format port) }
    }, 1000)

    import config._

    unfiltered.jetty.Http(port)
      .resources(ConnectServer.resources)
      .filter(new RequestDumper)
      .context("/repl") {
        _.filter(new ReplPlan(config))
      }
      .filter(oauth2Plan)
      .filter(authenticationPlan) // Login etc
      .context("/connect") {
        _.filter(tokenAuthorizationPlan)
        .filter(userInfoPlan)
      }.run()
  }
}
