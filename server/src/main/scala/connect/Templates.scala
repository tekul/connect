package connect


object Templates {
  import unfiltered.response._
  import unfiltered.oauth2.{RequestBundle}

  def page(body: scala.xml.NodeSeq) = Html(
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title>OpenID Connect Provider</title>
        <link href="/css/app.css" type="text/css" rel="stylesheet" />
      </head>
      <body>
        <div id="container">
          <h1><a href="/">OpenID Connect Provider</a></h1>
          {body}
        </div>
      </body>
    </html>
  )

  def index(urlBase: String, currentUser: Option[User]) = page(
    <div>{
        currentUser match {
          case Some(user) => <div>
            <p>welcome {user.id}. <a href="/logout">log out</a></p>
            <p>view your <a href="/connections">connections</a></p>
          </div>
          case _ => <p><a href="/login">log in</a>.</p>
        }
      }

      <a href="/authorize?client_id=exampleclient&amp;redirect_uri=http://localhost:8081/&amp;response_type=code%20id_token&amp;scope=openid">OpenID Authorization Code request</a>
      <br />
    </div>
  )

  def loginForm() = page(
    <div>
     <form action="/login" method="POST">
      <input type="text" name="user" value="user" />
      <input type="password" name="password" value="password" />
      <input type="submit" value="login"/>
     </form>
    </div>
  )

  def loginForm[T](bundle: RequestBundle[T]) = page(
    <div>
     <form action="/login" method="POST">
      <p>Sign in. A 3rd party application would like access to your data</p>
      <input type="hidden" name="response_type" value={bundle.responseTypes.mkString(" ")} />
      <input type="hidden" name="client_id" value={bundle.client.id} />
      <input type="hidden" name="redirect_uri" value={bundle.redirectUri} />

      <input type="text" name="user" value="user" />
      <input type="password" name="password" value="password" />
      <input type="submit" value="login"/>
     </form>
    </div>
  )

  def authorizationForm[T](bundle: RequestBundle[T], approve: String, deny: String) = page(
    <div>
    <form action="/authorize" method="POST">
        <p>
          A 3rd party application named <strong>{bundle.client.id}</strong> has requested access to your data.
        </p>

        <input type="hidden" name="response_type" value={bundle.responseTypes.mkString(" ")} />
        <input type="hidden" name="client_id" value={bundle.client.id} />
        <input type="hidden" name="redirect_uri" value={bundle.redirectUri} />

        <div id="oauth-opts">
          <input type="submit" name="submit" value={approve} />
          <input type="submit" name="submit" value={deny} />
        </div>
      </form>
    </div>
  )

}
