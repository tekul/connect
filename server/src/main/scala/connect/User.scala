package connect

case class User(id: String, password: Option[String]) extends connect.oauth2.ResourceOwner
