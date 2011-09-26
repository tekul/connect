package openid


trait UserInfo

trait UserInfoService {
  def userInfo(id: String, scopes: Seq[String]): Option[UserInfo]
}

case class UserProfile(id: String,
                       name: String,
                       given_name: String,
                       family_name: String,
                       middle_name: Option[String],
                       nickname: String,
                       profile: Option[String],
                       picture: Option[String],
                       website: Option[String],
                       email: String,
                       verified: Boolean,
                       gender: String,
                       birthday: Option[String],
                       zoneinfo: String,
                       locale: String,
                       phone_number: Option[String],
                       address: Option[Address],
                       updated_time: String
                        ) extends UserInfo

case class Address(street_address: String,
                   locality: String,
                   region: Option[String],
                   postal_code: Option[String],
                   country: String,
                   formatted: Option[String])
