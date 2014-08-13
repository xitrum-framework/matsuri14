package matsuri.demo.action

import xitrum.{Action, SkipCsrfCheck}
import xitrum.annotation.{GET, POST}
import xitrum.validator.Required

import matsuri.demo.session.SVar
import matsuri.demo.model.User

@GET("login")
class LoginIndex extends DefaultLayout {
  def execute() {
    respondView()
  }
}

@POST("login")
class Login extends Action {
  def execute() {

    val name      = param("name")
    val password  = param("password")

    Required.exception("name", name)
    Required.exception("password", password)

    User.authLogin(name, password) match {
      case Some(user) =>
        SVar.userName.set(user.name)
        val a = 1
        redirectTo[ChatIndex]()
      case None    =>
        flash(t(s"Invalid username or password"))
        redirectTo[LoginIndex]()
    }
  }
}

@GET("logout")
class Logout extends Action with SkipCsrfCheck {
  def execute() {
    SVar.userName.remove
    redirectTo[LoginIndex]()
  }
}
