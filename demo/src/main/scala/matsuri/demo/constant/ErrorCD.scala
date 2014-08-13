package matsuri.demo.constant

trait ErrorCD {
  val STATUS_SUCCESS = 0
  val STATUS_FAIL    = 1

  val USER_NOT_FOUND        = 101
  val USER_ALREADY_EXISTS   = 102
}

object ErrorCD extends ErrorCD