package matsuri.demo.filter

import xitrum.Action

import matsuri.demo.action.LoginIndex
import matsuri.demo.session.SVar


trait LoginFilter {
  this: Action =>
  beforeFilter {
    if (!SVar.userName.isDefined) {
      redirectTo[LoginIndex]()
      false
    } else {
      true
    }
  }
}
