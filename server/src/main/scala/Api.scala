import unfiltered.filter.request.ContextPath
import unfiltered.request._
import unfiltered.response._
import net.liftweb.json.JsonDSL._

class Api extends unfiltered.filter.Plan {
  def intent = {
    case ContextPath(_, Seg("users" :: id)) => {

      Json(("user" -> ("id" -> id) ~ ("name" -> "finnegan")))
    }
  }
}
