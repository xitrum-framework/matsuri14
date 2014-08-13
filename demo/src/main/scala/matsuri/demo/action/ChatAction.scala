package matsuri.demo.action

import xitrum.SockJsAction
import xitrum.annotation.{GET, SOCKJS}

import matsuri.demo.filter.LoginFilter

@GET("chat")
class ChatIndex extends DefaultLayout with LoginFilter {
  def execute() {
    respondView()
  }
}

@SOCKJS("connect")
class Chat extends SockJsAction {
  def execute() {
  }
}

