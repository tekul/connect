
/**
 * OAuth 2/OpenID Connect sample client app.
 * <p>
 * Based on the OAuth1 sample at https://github.com/softprops/unfiltered-oauth-client.g8.git
 */
object Client {
  val resources = new java.net.URL(getClass.getResource("/web/robots.txt"), ".")
  val port = 8081

  def main(args: Array[String]) {
    unfiltered.jetty.Http(port)
      .resources(resources)
      .filter(new App()).run
  }
}
