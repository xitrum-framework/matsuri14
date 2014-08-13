package matsuri.demo.filter

import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED

import xitrum.Action

import matsuri.demo.Config
import matsuri.demo.session.SVar


trait AdminFilter {
  this: Action =>
  beforeFilter {
    val ret = SVar.isAdmin.isDefined
    if (!ret) {

      // For API client: Enable pass name and password via HTTPHeader
      val headerBasicAuthName     = HttpHeaders.getHeader(request, "X-BasicAuthName")
      val headerBasicAuthPassword = HttpHeaders.getHeader(request, "X-BasicAuthPassword")

      val authenticated =
        // For API client:
        if (headerBasicAuthName != null && headerBasicAuthPassword != null) {
          if (headerBasicAuthName == Config.basicAuth.name && headerBasicAuthPassword == Config.basicAuth.pass) {
            true
          } else {
            response.setStatus(UNAUTHORIZED)
            respondText("Wrong name or password")
            false
          }
        } else {
          // For Human(Browser): Use basic auth and save session
          basicAuth(Config.basicAuth.realm) { (username, password) =>
          if (username == Config.basicAuth.name && password == Config.basicAuth.pass) {
            SVar.isAdmin.set(true)
            true
          } else {
            false
          }
        }
      }
      authenticated
    } else {
      true
    }
  }
}
