package matsuri.demo.session

import xitrum.SessionVar

object SVar {
  object isAdmin   extends SessionVar[Boolean]
  object userName  extends SessionVar[String]
}