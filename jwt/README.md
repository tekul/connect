Sample JWT usage.
------------------

1. Create a signed token

    scala> import jwt._
    import jwt._

    scala> import crypto.sign._
    import crypto.sign._

    scala> val signer = MacSigner("mackey".getBytes)
    signer: crypto.sign.SignerVerifier = MacSigner: HMACSHA256

    scala> val token = Jwt("""{"iss":"http:\/\/server.example.com","user_id":"248289761001","aud":"http:\/\/client.example.com","exp":1311281970}""", signer)
    token: jwt.JwtImpl = {"alg":"HS256"} {"iss":"http:\/\/server.example.com","user_id":"248289761001","aud":"http:\/\/client.example.com","exp":1311281970} [32 crypto bytes]

2. (continuation) Decode a signed token and verify the signature.

    scala> val tokenString = new String(token.bytes, "UTF-8")
    tokenString: java.lang.String = eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOlwvXC9zZXJ2ZXIuZXhhbXBsZS5jb20iLCJ1c2VyX2lkIjoiMjQ4Mjg5NzYxMDAxIiwiYXVkIjoiaHR0cDpcL1wvY2xpZW50LmV4YW1wbGUuY29tIiwiZXhwIjoxMzExMjgxOTcwfQ.bxFIt5xQ6Pa_LZF5oEwNF1WsjR22BTBdnaHhLKEuz6M

    scala> val token = Jwt(tokenString)
    token: jwt.JwtImpl = {"alg":"HS256"} {"iss":"http:\/\/server.example.com","user_id":"248289761001","aud":"http:\/\/client.example.com","exp":1311281970} [32 crypto bytes]

    scala> token.verifySignature(signer)

3. Retrieve the claims content for JSON parsing

    scala> token.claims
    res0: String = {"iss":"http:\/\/server.example.com","user_id":"248289761001","aud":"http:\/\/client.example.com","exp":1311281970}
