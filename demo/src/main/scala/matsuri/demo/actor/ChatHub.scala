package matsuri.demo.actor

import scala.collection.mutable.{Map => MMap}

import akka.actor.{Actor, ActorRef, Props, Terminated}

import xitrum.{SockJsAction, SockJsText}
import xitrum.annotation.SOCKJS
import xitrum.util.SeriDeseri

import matsuri.demo.action.Chat
import matsuri.demo.constant.ErrorCD._
import matsuri.demo.model.Msg

class ChatHub extends Hub {
  var lookUpTable = MMap[ActorRef, String]()

  override def handleSubscribe(client: ActorRef, option: Map[String, Any]): Map[String, Any] = {
    lookUpTable.put(client, option.getOrElse("senderName", "").toString)
    Map(
      "error"      -> STATUS_SUCCESS,
      "tag"        -> "joinMsg",
      "senderName" -> option.getOrElse("senderName","")
    )
  }

  override def handleUnsubscribe(client: ActorRef, option: Map[String, Any]): Map[String, Any] = {
    if (lookUpTable.contains(client)) {
      val removedMember = lookUpTable(client)
      lookUpTable.remove(client)
      Map(
        "error"      -> STATUS_SUCCESS,
        "tag"        -> "leaveMsg",
        "senderName" -> removedMember
      )
    } else {
      Map.empty
    }
  }

  override def handlePush(option: Map[String, Any]): Map[String, Any] = {
    option.getOrElse("cmd", "invalid") match {
      case "text" =>
        val msg = Msg.insert(option.getOrElse("body","").toString, option.getOrElse("senderName","Anonymous").toString)
        Map(
          "error"     -> STATUS_SUCCESS,
          "seq"       -> option.getOrElse("seq", -1),
          "targets"   -> option.getOrElse("targets", "*"),
          "tag"       -> "text",
          "senderId"  -> option.getOrElse("senderId",""),
          "msg"       -> msg.toMap
        )

      case unknown =>
        Map(
          "tag"     -> "system",
          "error"   -> INVALID_CMD,
          "seq"     -> option.getOrElse("seq", -1),
          "targets" -> option.getOrElse("uuid", "")
        )
    }
  }
  override def handlePull(option: Map[String, Any]):  Map[String, Any] = {
    option.getOrElse("cmd", "invalid") match {
      case "latest10Msg" =>
       val msgs = Msg.getLatest(10, None)
        Map(
          "tag"     -> "system",
          "error"   -> STATUS_SUCCESS,
          "seq"     -> option.getOrElse("seq", -1),
          "msgs"    -> msgs.map(_.toMap)
        )
      case "olderThan" =>
       val olderThanId = if (option.isDefinedAt("olderThanId")) Some(option("olderThanId").asInstanceOf[String]) else None
       val msgs = Msg.getLatest(10, olderThanId)
        Map(
          "tag"     -> "system",
          "error"   -> STATUS_SUCCESS,
          "seq"     -> option.getOrElse("seq", -1),
          "msgs"    -> msgs.map(_.toMap)
        )
      case "allMsg" =>
       val msgs = Msg.listAll()
        Map(
          "tag"     -> "system",
          "error"   -> STATUS_SUCCESS,
          "seq"     -> option.getOrElse("seq", -1),
          "msgs"    -> msgs.map(_.toMap)
        )
      case unknown =>
        Map(
          "tag"     -> "system",
          "error"   -> INVALID_CMD,
          "seq"     -> option.getOrElse("seq", -1)
        )
    }
  }
}