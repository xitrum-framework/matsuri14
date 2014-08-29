package matsuri.demo.action

import xitrum.Action
import xitrum.annotation.{GET, POST, Swagger}
import xitrum.validator.Required

import matsuri.demo.model.User

@Swagger(
  Swagger.Resource("auth", "auth actions", 1)
)
trait AuthDocumentation extends Action {}

@Swagger(
  Swagger.Nickname("loginIndex"),
  Swagger.Summary("Display login form"),
  Swagger.Response(200, "show index")
)
@GET("login", "")
class LoginIndex extends DefaultLayout with AuthDocumentation {
  def execute() {
    respondView()
  }
}

@Swagger(
  Swagger.Nickname("loginAction"),
  Swagger.Summary("Authenticate user"),
  Swagger.Note("Success: request will be redirect to ChatIndex, Fail: request will be redirect to LoginIndex"),
  Swagger.StringForm("name"),
  Swagger.StringForm("password"),
  Swagger.Response(200, "redirect to chatIndex")
)
@POST("login")
class Login extends Action with AuthDocumentation {
  def execute() {
    session.clear()
    val name     = param("name")
    val password = param("password")

    User.authLogin(name, password) match {
      case Some(user) =>
        SVar.userName.set(user.name)
        redirectTo[ChatIndex]()

      case None =>
        flash(t(s"Invalid username or password"))
        redirectTo[LoginIndex]()
    }
  }
}

@Swagger(
  Swagger.Nickname("logoutAction"),
  Swagger.Note("All request will be redirect to LoginIndex"),
  Swagger.Response(200, "Success to logout")
)
@GET("logout")
class Logout extends Action with AuthDocumentation {
  def execute() {
    session.clear()
    redirectTo[LoginIndex]()
  }
}
