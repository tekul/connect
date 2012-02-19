package connect

import connect.oauth2.{Client, ClientStore}

case class AppClient(id: String, secret: String, redirectUri: String) extends Client

class Clients extends ClientStore {
  val clients = new java.util.HashMap[String, Client] {
    put(
      "exampleclient",
       AppClient("exampleclient", "secret", "http://localhost:8081")
    )
  }
  def client(clientId: String, secret: Option[String]) = clients.get(clientId) match {
    case null => None
    case c => Some(c)
  }
}
