package matsuri.demo.model

import java.security.MessageDigest
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}

import com.mongodb.casbah.Imports._

import io.netty.util.CharsetUtil.UTF_8

import xitrum.{Config => XConfig, Log}
import xitrum.util.Secure

import matsuri.demo.Config
import matsuri.demo.constant.ErrorCD
import matsuri.demo.db.DB
import matsuri.demo.db.DB.msgsColl.{T => MsgCollType}
import matsuri.demo.util.Converter

object Msg extends Log {
  import matsuri.demo.constant.ErrorCD._
  val cache = XConfig.xitrum.cache

  def insert(body:String, senderName:String):Msg = {
    val createdAt = DB.nowSecs()
    val o = MongoDBObject(
              "body"       -> body,
              "senderName" -> senderName,
              "createdAt"  -> createdAt
            )

    val result = DB.msgsColl.insert(o)
    val m = new Msg(o.asInstanceOf[MsgCollType])
    cache.put(senderName, m.toMap)
    m
  }

  def deleteBySenderName(senderName: String):Int = {
    val query  = MongoDBObject( "senderName" -> senderName)
    val result = DB.msgsColl.remove(query)
    if (result.getN > 0) STATUS_SUCCESS
    else STATUS_FAIL
  }

  def listAll() = {
    val s      = MongoDBObject("_id" -> -1)
    val cursor = DB.msgsColl.find().sort(s)
    val ret    = new ListBuffer[Msg]
    while (cursor.hasNext) {
      ret += new Msg(cursor.next())
    }
    ret.toList
  }

  def getLatest(num:Int, olderThan:Option[String] = None) = {
    val s      = MongoDBObject("_id" -> -1)
    val query  = MongoDBObject()
    if (olderThan.isDefined)  query.put("olderThan",  MongoDBObject("$lt" -> olderThan))
    val cursor = DB.msgsColl.find(query).sort(s).limit(num)
    val ret    = new ListBuffer[Msg]
    while (cursor.hasNext) {
      ret += new Msg(cursor.next())
    }
    ret.toList
  }

  def findById(id:String): Option[Msg] = {
   findOne("_id", id)
  }

  def findBySenderName(senderName:Option[String]): List[Msg] = {
    senderName match {
      case Some(name) =>
        findBySenderName(name)
      case None       =>
        List.empty
    }
  }
  def findBySenderName(senderName:String): List[Msg] = {
    val s      = MongoDBObject("_id" -> -1)
    val query  = MongoDBObject("senderName" -> senderName)
    val cursor = DB.msgsColl.find(query).sort(s)
    val ret    = new ListBuffer[Msg]
    while (cursor.hasNext) {
      ret += new Msg(cursor.next())
    }
    ret.toList
  }


  private def findOne(key: String, value: String): Option[Msg] = {
    val q = MongoDBObject(key -> value)
    DB.msgsColl.findOne(q) match {
      case Some(x) => Some(new Msg(x))
      case None    => None
    }
  }
}

class Msg(doc: MsgCollType) {
  lazy val body           = doc.getAsOrElse[String]("body", "")
  lazy val senderName     = doc.getAsOrElse[String]("senderName", "")
  lazy val createdAt      = doc.getAsOrElse[Int]("createdAt", 0)

  val createdAtAsStr = if (createdAt == 0) "-" else Converter.unixTime2yyyyMMddHHmm(createdAt.toLong)

  def apply(body:String, senderName:String, createdAt:Int) = {
    this
  }

  def asDBObject = MongoDBObject(
    doc.toMap.asScala.map{case (k,v) => (k.toString, v)}.toList
  )

  def toJson = s"""{"body":"body","senderName":senderName,"createdAt":"$createdAtAsStr"}"""

  def toMap = {
    Map(
      "body"       -> body,
      "senderName" -> senderName,
      "createdAt"  -> createdAtAsStr
    )
  }
  override def toString = toJson
}
