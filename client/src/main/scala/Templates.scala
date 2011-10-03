import unfiltered.response.Html

trait Templates {

  def page(body: scala.xml.NodeSeq) = Html(
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title>OAuth2/OpenIDConnect Client</title>
        <link href="/css/app.css" type="text/css" rel="stylesheet"/>
      </head>
      <body>
        <div id="container">
          <h1><a href="/">OAuth2/OpenIDConnect Client</a></h1>
          {body}
        </div>
      </body>
    </html>
  )

//  def index = page(<a href={"http://localhost:%s/" format Client.port} >Connect with Provider</a>)

  def userInfo(response: String) = page(
    <div><a href="/disconnect">Disconnect</a></div>
    <div>
      <h2>User Info</h2>
      <pre>{response}</pre>
    </div>
  )

  def tokenList(toks: Iterable[AccessToken]) = page(
    <div>
      <p>
        <a href={"http://localhost:%s/connect" format Client.port} class="btn" >connect with provider</a>
      </p>
      { if(toks.isEmpty) <p>No oauth tokens in sight</p>}
      {  toks.map { t => t match {
          case AccessToken(value, idToken) =>
            <p>
            access_token <strong>{ value }</strong>
            </p>
            <p>
            id_token {idToken.getOrElse("None")}
            </p>
      } } }
    </div>
  )
}
