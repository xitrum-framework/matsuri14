package matsuri.demo.action

import xitrum.Action
import xitrum.annotation.GET


trait LoginFilter {
  this: Action =>

  beforeFilter {
    if (SVar.userName.isDefined) {
      true
    } else {
      redirectTo[LoginIndex]()
      false
    }
  }
}
