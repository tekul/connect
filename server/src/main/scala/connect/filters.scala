package connect
import connect.boot.{ComponentRegistryFilter=>CRF, ComponentRegistry=> CR}
import connect.util.ReplPlan

/**
 * Filter definitions purely for web.xml's benefit.
 */
final class OAuth2Filter extends CRF[CR] {
  def delegate(cr: CR) = cr.asInstanceOf[ConnectComponentRegistry].oauth2Plan
}

final class AuthenticationFilter extends CRF[CR] {
  def delegate(cr: CR) = cr.asInstanceOf[ConnectComponentRegistry].authenticationPlan
}

final class TokenAuthorizationFilter extends CRF[ConnectComponentRegistry] {
  def delegate(cr: CR) = cr.asInstanceOf[ConnectComponentRegistry].tokenAuthorizationPlan
}

final class UserInfoFilter extends CRF[ConnectComponentRegistry] {
  def delegate(cr: CR) = cr.asInstanceOf[ConnectComponentRegistry].userInfoPlan
}

final class ReplFilter extends CRF[ConnectComponentRegistry] {
  def delegate(cr: CR) = new ReplPlan(cr)
}