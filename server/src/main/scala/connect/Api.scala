package connect

import unfiltered.filter.request.ContextPath
import unfiltered.request._
import unfiltered.response._
import net.liftweb.json.JsonDSL._

class Api extends unfiltered.filter.Plan {
  def intent = {
    case ContextPath(_, Seg("hello" :: id)) => {
      ResponseString("Hello %s, I'm an API" format(id))
    }
  }
}
