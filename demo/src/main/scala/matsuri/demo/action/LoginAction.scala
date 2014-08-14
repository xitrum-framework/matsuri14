package matsuri.demo.action

import xitrum.Action
import xitrum.annotation.{GET, POST}
import xitrum.validator.Required

import matsuri.demo.model.User

@GET("login", "")
class LoginIndex extends DefaultLayout {
  def execute() {
    respondView()
  }
}

@POST("login")
class Login extends Action {
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

@GET("logout")
class Logout extends Action {
  def execute() {
    session.clear()
    redirectTo[LoginIndex]()
  }
}
