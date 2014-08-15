package matsuri.demo.actor

import scala.collection.mutable.{Map => MMap}

import akka.actor.{Actor, ActorRef, Props, Terminated}

import xitrum.{SockJsAction, SockJsText}
import xitrum.annotation.SOCKJS
import xitrum.util.SeriDeseri

import matsuri.demo.action.ChatAction
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
  override def handleTerminated(client: ActorRef) = handleUnsubscribe(client, Map.empty)

  override def handlePush(option: Map[String, Any]): Map[String, Any] = {
    option.getOrElse("cmd", "invalid") match {
      case "text" => option
      case "ping" =>
        Map(
          "error"     -> STATUS_SUCCESS,
          "seq"       -> option.getOrElse("seq", -1),
          "targets"   -> option.getOrElse("targets", "*"),
          "tag"       -> "ping",
          "senderId"  -> option.getOrElse("senderId","")
        )
      case "pong" =>
        Map(
          "error"     -> STATUS_SUCCESS,
          "seq"       -> option.getOrElse("seq", -1),
          "targets"   -> option.getOrElse("targets", ""),
          "tag"       -> "pong",
          "senderId"  -> option.getOrElse("senderId","")
        )
      case unknown =>
        Map(
          "tag"     -> "system",
          "error"   -> INVALID_CMD,
          "seq"     -> option.getOrElse("seq", -1),
          "targets" -> option.getOrElse("targets", "")
        )
    }
  }
  override def handlePull(option: Map[String, Any]): Map[String, Any] = {
    option.getOrElse("cmd", "invalid") match {
      case "clientCount" =>
        Map(
          "tag"     -> "clientCount",
          "error"   -> STATUS_SUCCESS,
          "seq"     -> option.getOrElse("seq", -1),
          "count"   -> clients.size
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