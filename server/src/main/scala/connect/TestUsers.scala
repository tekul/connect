package connect

import net.liftweb.json._
import openid.{UserInfo, UserProfile, UserInfoService}

object TestUsers extends UserInfoService {
  implicit val formats = DefaultFormats

  val JohnColtrane = """{
     "id": "john",
     "name": "John Coltrane",
     "given_name": "John",
     "family_name": "Coltrane",
     "middle_name": "William",
     "nickname": "Trane",
     "profile": "http://en.wikipedia.org/wiki/John_Coltrane",
     "picture": "http://www.johncoltrane.com/images/p5.jpg",
     "website": "http://www.johncoltrane.com/",
     "email": "jc@trane.jazz",
     "gender": "male",
     "birthday": "09/23/1926",
     "zoneinfo": "America/New_York",
     "locale": "en_US",
     "phone_number": "1234 5678",
     "updated_time": "2011-09-25T23:58:42+0000"
   }"""

//       "address": {
//         "street_address": "1511 North Thirty-third Street",
//         "locality:" "Philadelphia",
//         "country": "USA"
//       }

  val users = Map(
    "john" -> parse(JohnColtrane).extract[UserProfile]
  )

//        UserProfile("john", "John Coltrane", "John", "Coltrane", Some("William"), "Trane",
//        Some(),None, None, "", false,  "male",
//        Some("09/23/1926"), "America/New_York","en_US", None, Some(Address("1511 North Thirty-third Street", "Philadelphia", None, None, "USA", None)),
//        "2011-09-25T23:58:42+0000."))

  override def userInfo(id: String, scopes: Seq[String]): Option[UserInfo] = {
    users.get(id)
  }
}
