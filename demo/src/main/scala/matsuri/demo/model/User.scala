package matsuri.demo.model

import java.security.MessageDigest
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}

import com.mongodb.casbah.Imports._

import io.netty.util.CharsetUtil.UTF_8

import xitrum.Log
import xitrum.util.Secure

import matsuri.demo.Config
import matsuri.demo.constant.ErrorCD
import matsuri.demo.db.DB
import matsuri.demo.db.DB.usersColl.{T => UserCollType}
import matsuri.demo.util.Converter

object User extends Log {
  import matsuri.demo.constant.ErrorCD._

  def create(name:String, password:String, age:Option[Int], desc:Option[String]):Either[Int, Option[User]] = {
    val o = MongoDBObject(
              "name"      -> name,
              "password"  -> makeHash(name, password, 10),
              "createdAt" -> DB.nowSecs()
            )
    if (age.isDefined)  o.put("age",  age.get)
    if (desc.isDefined) o.put("desc", desc.get)

    println(o)

    if (DB.insertIfNonexistent(DB.usersColl, o)) {
      Right(findByName(name))
    } else {
      Log.error("Failed to create user. User is already exist. name:" + name)
      Left(USER_ALREADY_EXISTS)
    }
  }

  def update(name:String, password:Option[String], age:Option[Int], desc:Option[String]):Either[Int, Option[User]] = {
    val query = MongoDBObject("name" -> name)

    val set   = MongoDBObject("updatedAt" -> DB.nowSecs())
    if (password.isDefined) set.put("password", password.get)
    if (age.isDefined)      set.put("age",      age.get)
    if (desc.isDefined)     set.put("desc",     desc.get)
    val update = MongoDBObject("$set" -> set)

    val result = DB.usersColl.findAndModify(query, update)
    result match {
      case Some(u) => Right(findByName(name))
      case _       => Left(USER_NOT_FOUND)
    }
  }

  def delete(name: String):Int = {
    val query  = MongoDBObject( "name" -> name)
    val result = DB.usersColl.findAndRemove(query)
    result match {
      case Some(u) => STATUS_SUCCESS
      case _       => USER_NOT_FOUND
    }
  }

  def listAll() = {
    val s      = MongoDBObject("_id" -> -1)
    val cursor = DB.usersColl.find().sort(s)
    val ret    = new ListBuffer[User]
    while (cursor.hasNext) {
      ret += new User(cursor.next())
    }
    ret.toList
  }

  def findById(id:String): Option[User] = {
   findOne("_id", id)
  }

  def findByName(name:String): Option[User] = findOne("name", name)
  def findByName(name:Option[String]): Option[User] = {
   name match {
     case Some(name) => findByName(name)
     case None       => None
   }
  }

  private def findOne(key: String, value: String): Option[User] = {
    val q = MongoDBObject(key -> value)
    DB.usersColl.findOne(q) match {
      case Some(x) => Some(new User(x))
      case None    => None
    }
  }

  def authLogin(inputName:String, inputPassword:String) :Option[User] = {
    findByName(inputName) match {
      case u@Some(user) =>
        if (user.auth(makeHash(inputName, inputPassword, 10))) {
          val q      = MongoDBObject("name" -> inputName)
          val updObj = user.asDBObject ++ MongoDBObject("lastLogin" -> DB.nowSecs())
          DB.usersColl.update(q, updObj)
          u
        }
        else
          None
      case None =>
        None
    }
  }

  private def getHash(key:String):String =  {
    val messageDigest = MessageDigest.getInstance("MD5")
    messageDigest.reset
    messageDigest.update(key.getBytes(UTF_8))
    messageDigest.digest.map("%02x".format(_)).mkString
  }

  private def makeHash(key1:String, key2:String, max:Int):String = {
    val salt = getHash(key1)
    val hash = getHash(salt + key2)
    if (max < 1) hash
    else makeHash(salt, hash, max-1)
  }
}

class User(doc: UserCollType) {
  lazy val name           = doc.getAsOrElse[String]("name", "")
  lazy val age            = doc.getAsOrElse[Int]("age", 0)
  lazy val desc           = doc.getAsOrElse[String]("desc", "")
  lazy val createdAt      = doc.getAsOrElse[Int]("createdAt", 0)
  lazy val updatedAt      = doc.getAsOrElse[Int]("updatedAt", 0)
  lazy val lastLogin      = doc.getAsOrElse[Int]("lastLogin", 0)
  private lazy val password = doc.getAs[String]("password")

  val createdAtAsStr = if (createdAt == 0) "-" else Converter.unixTime2yyyyMMddHHmm(createdAt.toLong)
  val updatedAtAsStr = if (updatedAt == 0) "-" else Converter.unixTime2yyyyMMddHHmm(updatedAt.toLong)
  val lastLoginAsStr = if (lastLogin == 0) "-" else Converter.unixTime2yyyyMMddHHmm(lastLogin.toLong)

  def auth(inputPasswordAsHash:String):Boolean = {
    inputPasswordAsHash == password.get
  }

  def asDBObject = MongoDBObject(
    doc.toMap.asScala.map{case (k,v) => (k.toString, v)}.toList
  )

  def toJson = s"""{"name":"$name","age":$age,"desc":"$desc","createdAt":"$createdAtAsStr","updatedAt":"$updatedAtAsStr","lastLogin":"$lastLoginAsStr"}"""

  def toMap = {
    Map(
      "name"       -> name,
      "age"        -> age,
      "desc"       -> desc,
      "createdAt"  -> createdAtAsStr,
      "updatedAt"  -> updatedAtAsStr,
      "lastLogin"  -> lastLoginAsStr
    )
  }
  override def toString = toJson
}

