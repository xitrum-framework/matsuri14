package matsuri.demo

import com.typesafe.config.{Config => TConfig, ConfigFactory}

class DefaultDBConfig {
  val host     = "127.0.0.1"
  val port     = 27017
  val database = "matsuri"
  val user:Option[String]   = None
  val passwd:Option[String] = None
}

class DBConfig(config: TConfig) extends DefaultDBConfig {
  override val host     = config.getString("host")
  override val port     = config.getInt("port")
  override val database = config.getString("database")
  override val user   = if (config.hasPath("user"))     Some(config.getString("user")) else None
  override val passwd = if (config.hasPath("password")) Some(config.getString("password")) else None
}

class DefaultBasicAuthConfig {
  val realm = "Realm"
  val name  = "admin"
  val pass  = "password"
}

class BasicAuthConfig(config: TConfig) extends DefaultBasicAuthConfig {
  override val realm = config.getString("realm")
  override val name  = config.getString("name")
  override val pass  = config.getString("pass")
}

object Config {
  val db =
    if (xitrum.Config.application.hasPath("db"))
      new DBConfig(xitrum.Config.application.getConfig("db"))
    else
      new DefaultDBConfig()

  val basicAuth =
    if (xitrum.Config.application.hasPath("basicAuth"))
      new BasicAuthConfig(xitrum.Config.application.getConfig("basicAuth"))
    else
      new DefaultBasicAuthConfig()

}