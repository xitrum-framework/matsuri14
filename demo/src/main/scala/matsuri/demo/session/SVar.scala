package matsuri.demo.session

import xitrum.SessionVar

object SVar {
  object isAdmin extends SessionVar[Boolean]
  object userId  extends SessionVar[Int]
}