package matsuri.demo.action

import xitrum.{Action, Config}
import xitrum.annotation.{GET, POST, PUT, DELETE, Swagger, CacheActionMinute}
import xitrum.validator.{Required, Range}

import matsuri.demo.constant.ErrorCD._
import matsuri.demo.model.{Msg, User}

@Swagger(
  Swagger.OptStringQuery("format",               "For API client: set `json`"),
  Swagger.OptStringHeader("X-BasicAuthName",     "For API client: set basicAuth user name"),
  Swagger.OptStringHeader("X-BasicAuthPassword", "For API client: set basicAuth user password"),
  Swagger.Response(401, "Unauthorized as admin user")
)
trait AdminAction extends DefaultLayout with AdminFilter {
  protected var format: String = _

  beforeFilter {
    format = paramo("format").getOrElse("html")
    true
  }
}

@Swagger(
  Swagger.Summary("List Users"),
  Swagger.Response(200, "Return all users list")
)
@GET("admin")
class AdminIndex extends AdminAction {
  def execute() {
    // Get all users
    val users = User.listAll

    if (format == "json") respondJson(users.map(_.toMap))
    else {
      // Share user with view template
      at("users") = users

      // Response respons view with template
      respondView()
    }
  }
}

@Swagger(
  Swagger.Summary("Create User"),
  Swagger.Response(200, "status = 0: Success to create user, status = 1: Failed to create user"),
  Swagger.Response(400, "Invalid request parameter"),
  Swagger.StringForm("name"),
  Swagger.StringForm("password"),
  Swagger.OptIntForm("age"),
  Swagger.OptStringForm("desc")
)
@POST("admin/user")
class AdminUserCreate extends AdminAction {
  def execute() {
    // Get request paramaters
    val name     = param("name")
    val password = param("password")
    // Optional parameters
    val age      = paramo[Int]("age")
    val desc     = paramo("desc")

    // Validate required parameters
    Required.exception("name",     name)
    Required.exception("password", password)

    // Create user and respond result as JSON format
    User.create(name, password, age, desc) match {
      case Left(errCd) =>
        if (format == "json") respondJson(Map("status" -> STATUS_FAIL,    "msg" -> t(s"Failed to create user: ${errCd}")))
        else {
          flash(t(s"Failed to create User: ${errCd}"))
          redirectTo[AdminIndex]()
        }
      case Right(Some(user)) =>
        if (format == "json") respondJson(Map("status" -> STATUS_SUCCESS, "user" -> user.toMap))
        else {
          flash(t("Success"))
          redirectTo[AdminIndex]()
        }
      case _ =>
        if (format == "json") respondJson(Map("status" -> STATUS_FAIL,    "msg" -> t(s"Created user not found")))
        else {
          flash(t(s"Created user not found"))
          redirectTo[AdminIndex]()
        }
    }
  }
}

@Swagger(
  Swagger.Summary("Update User"),
  Swagger.Response(200, "status = 0: Success to update user, status = 1: Failed to update user"),
  Swagger.Response(400, "Invalid request parameter"),
  Swagger.StringPath("name"),
  Swagger.OptStringForm("password"),
  Swagger.OptIntForm("age"),
  Swagger.OptStringForm("desc")
)
@PUT("admin/user/:name")
class AdminUserUpdate extends AdminAction {
  def execute() {
    val name     = param("name")
    val password = paramo("password")
    val age      = paramo[Int]("age")
    val desc     = paramo("desc")
    Required.exception("name", name)

    if (!password.isDefined && !age.isDefined && !desc.isDefined)
      respondJson(Map("status" -> STATUS_FAIL, "msg" -> t("Invalid parameter")))
    else {
      User.update(name, password, age, desc) match {
        case Left(errCd) =>
          if (format == "json") respondJson(Map("status" -> STATUS_FAIL,    "msg" -> t(s"Failed to update user: ${errCd}")))
          else {
            flash(t(s"Failed to update user: ${errCd}"))
            redirectTo[AdminUserShow](("name", name))
          }
        case Right(Some(user)) =>
          if (format == "json") respondJson(Map("status" -> STATUS_SUCCESS, "user" -> user.toMap))
          else {
            flash(t(s"Success"))
            redirectTo[AdminUserShow](("name", name))
          }
        case _ =>
          if (format == "json") respondJson(Map("status" -> STATUS_FAIL,    "msg" -> t(s"Updated user not found")))
          else {
            flash(t(s"Updated user not found"))
            redirectTo[AdminUserShow](("name", name))
          }
      }
    }
  }
}

@Swagger(
  Swagger.Summary("Update user info"),
  Swagger.Response(200, "status = 0: Success to delete user, status = 1: Failed to delete user"),
  Swagger.Response(400, "Invalid request parameter"),
  Swagger.StringPath("name")
)
@DELETE("admin/user/:name")
class AdminUserDelete extends AdminAction {
  def execute() {
    val name = param("name")
    Required.exception("name", name)

    User.delete(name) match {
      case STATUS_SUCCESS =>
        if (format == "json") respondJson(Map("status" -> STATUS_SUCCESS))
        else {
          flash(t("Success"))
          redirectTo[AdminIndex]()
        }
      case errCd =>
         if (format == "json") respondJson(Map("status" -> STATUS_FAIL, "msg" -> t(s"Failed to delete user: ${errCd}")))
         else {
          flash(t(s"Failed to delete user: ${errCd}"))
          redirectTo[AdminIndex]()
         }
    }
  }
}

@Swagger(
  Swagger.Summary("Return specified user information"),
  Swagger.Response(200, "Response User"),
  Swagger.Response(404, "No user found by specified name"),
  Swagger.StringPath("name")
)
@GET("admin/user/:name")
class AdminUserShow extends AdminAction {
  def execute() {
    val name = param("name")
    Required.exception("UserName", name)

    val user = User.findByName(name)

    user match {
      case Some(user) =>
        if (format == "json") respondJson(Map("user" -> user))
        else {
          at("user") = user
          respondView()
        }
      case None =>
        if (format == "json") respondJson(Map())
        else {
          respondDefault404Page()
        }
    }
  }
}


// Example of action cache
@Swagger(
  Swagger.Summary("Return latest msg, result is cached 1 minute"),
  Swagger.Response(200, "Response Msg")
)
@GET("admin/msg")
@CacheActionMinute(1)
class AdminLastMessage extends xitrum.Action {  //AdminAction {
  def execute(){
    val msgs = Msg.getLatest(1)
    respondJson(msgs.map(_.toMap))
  }
}

// Example of server-side object cache
@Swagger(
  Swagger.Summary("Return latest msg by name, result is from serverside-cache(not database)"),
  Swagger.Response(200, "Response Msg"),
  Swagger.StringPath("name")
)
@GET("admin/msg/:name")
class AdminUserMessages extends AdminAction {
  // latest msg of each user is cached with username at `Msg.insert`
  val cache = Config.xitrum.cache
  def execute(){
    cache.getAs[Map[String, String]](param("name")) match {
      case Some(m) => respondJson(m)
      case None    => respondJson(Map.empty)
    }
  }
}