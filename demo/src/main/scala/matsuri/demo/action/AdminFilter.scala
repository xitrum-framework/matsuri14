package matsuri.demo.action

import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED
import xitrum.Action
import matsuri.demo.Config

trait AdminFilter {
  this: Action =>

  beforeFilter {
    if (SVar.isAdmin.isDefined) true else authApi().getOrElse(authBasic())
  }

  // For API client: Enable pass name and password via HTTPHeader
  private def authApi(): Option[Boolean] = {
    val headerBasicAuthName     = HttpHeaders.getHeader(request, "X-BasicAuthName")
    val headerBasicAuthPassword = HttpHeaders.getHeader(request, "X-BasicAuthPassword")

    if (headerBasicAuthName == null || headerBasicAuthPassword == null) return None

    if (headerBasicAuthName == Config.basicAuth.name && headerBasicAuthPassword == Config.basicAuth.pass) return Some(true)

    response.setStatus(UNAUTHORIZED)
    respondText("Wrong name or password")
    Some(false)
  }

  // For Human(Browser): Use basic auth and save session
  private def authBasic(): Boolean = {
    basicAuth(Config.basicAuth.realm) { (username, password) =>
      if (username == Config.basicAuth.name && password == Config.basicAuth.pass) {
        SVar.isAdmin.set(true)
        true
      } else {
        false
      }
    }
  }
}
