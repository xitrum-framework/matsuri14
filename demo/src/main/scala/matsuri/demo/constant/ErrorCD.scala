package matsuri.demo.constant

trait ErrorCD {
  val STATUS_SUCCESS = 0
  val STATUS_FAIL    = 1

  val USER_NOT_FOUND        = 101
  val USER_ALREADY_EXISTS   = 102

  val INVALID_CMD           = 201
  val INVALID_TAG           = 202

}

object ErrorCD extends ErrorCD