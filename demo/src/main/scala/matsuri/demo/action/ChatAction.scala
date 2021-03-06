package matsuri.demo.action

import akka.actor.{Actor, ActorRef, Props, Terminated}

import xitrum.{SockJsAction, SockJsText}
import xitrum.annotation.{GET, SOCKJS}
import xitrum.util.SeriDeseri

import matsuri.demo.actor.{HubClient, ChatHub, Done, Publish, Push, Pull, Subscribe, Unsubscribe}
import matsuri.demo.constant.ErrorCD._
import matsuri.demo.model.Msg

@GET("chat")
class ChatIndex extends DefaultLayout with LoginFilter {
  def execute() {
    respondView()
  }
}

@SOCKJS("connect")
class ChatAction extends SockJsAction with HubClient with LoginFilter {
  private val hubKey   = "glokkaExampleHub"
  private val hubProps = Props[ChatHub]

  def execute() {
    lookUpHub(hubKey, hubProps, Map.empty)
  }

  override def doWithHub(hub: ActorRef, option: Any) {
    val name     = SVar.userName.get
    val clientId = node
    hub ! Subscribe(Map("senderName" -> name, "seq" -> -1))
    context.watch(hub)
    context.become {
      // (AnotherNode -> ) Hub -> LocalNode
      case Publish(msg) =>
        if (!msg.isEmpty) {
          msg.getOrElse("targets", "*") match {
            case list:Array[String] if (list.contains(clientId)) =>
              // LocalNode -> client
              respondSockJsText(parse2JSON(msg - ("error", "seq", "targets")))
            case targetId:String if (targetId == clientId) =>
              // LocalNode -> client
              respondSockJsText(parse2JSON(msg - ("error", "seq", "targets")))
            case "*" =>
              // LocalNode -> client
              respondSockJsText(parse2JSON(msg - ("error", "seq", "targets")))
            case ignore =>
          }
        }

      // (LocalNode ->) Hub -> LocalNode
      case Done(result) =>
        if (!result.isEmpty) respondSockJsText(parse2JSON(result + ("tag" -> "system")))

        // Client -> LocalNode
      case SockJsText(msg) =>
        log.debug(s"[HubClient][${clientId}] Received message from client: $msg")
        parse2MapWithTag(msg) match {
//          case ("subscribe", parsed) =>
//            // LocalNode -> Hub (-> LocalNode)
//            log.debug(s"[HubClient][${clientId}] Send Subscribe request to HUB")
//            hub ! Subscribe(Map(
//                                "error"   -> STATUS_SUCCESS ,
//                                "tag"     -> "system",
//                                "seq"     -> parsed.getOrElse("seq", -1)
//                              ))
//
//          case ("unsubscribe", parsed) =>
//            // LocalNode -> Hub (-> LocalNode)
//            log.debug(s"[HubClient][${clientId}] Send Unsubscribe request to HUB")
//            hub ! Unsubscribe(Map(
//                                "error"   -> STATUS_SUCCESS,
//                                "tag"     -> "system",
//                                "seq"     -> parsed.getOrElse("seq", -1)
//                              ))
//
          case ("pull", parsed) =>
            parsed.getOrElse("cmd", "invalid") match {
              case "latest10Msg" =>
                val msgs = Msg.getLatest(10, None)
                respondSockJsText(parse2JSON(Map(
                  "tag"     -> "system",
                  "error"   -> STATUS_SUCCESS,
                  "seq"     -> parsed.getOrElse("seq", -1),
                  "msgs"    -> msgs.map(_.toMap)
                )))

              case "olderThan" =>
                val olderThanId = if (parsed.isDefinedAt("olderThanId")) Some(parsed("olderThanId").asInstanceOf[String]) else None
                val msgs = Msg.getLatest(10, olderThanId)
                respondSockJsText(parse2JSON(Map(
                  "tag"     -> "system",
                  "error"   -> STATUS_SUCCESS,
                  "seq"     -> parsed.getOrElse("seq", -1),
                  "msgs"    -> msgs.map(_.toMap)
                )))

              case "allMsg" =>
                val msgs = Msg.listAll()
                respondSockJsText(parse2JSON(Map(
                  "tag"     -> "system",
                  "error"   -> STATUS_SUCCESS,
                  "seq"     -> parsed.getOrElse("seq", -1),
                  "msgs"    -> msgs.map(_.toMap)
                )))

              case _       =>
                // LocalNode -> Hub (-> LocalNode)
                log.debug(s"[${clientId}] Send Pull request to HUB")
                hub ! Pull(parsed + ("clientId" -> clientId))
            }

          case ("push", parsed) =>
            // LocalNode -> Hub (-> AnotherNode)
            parsed.getOrElse("cmd", None) match {
              case "text" =>
                val msg = Msg.insert(parsed.getOrElse("body","").toString, name)
                hub ! Push(Map(
                        "error"     -> STATUS_SUCCESS,
                        "seq"       -> parsed.getOrElse("seq", -1),
                        "targets"   -> parsed.getOrElse("targets", "*"),
                        "cmd"       -> "text",
                        "senderId"  -> clientId,
                        "msg"       -> msg.toMap))
              case _      =>
                hub ! Push(parsed + ("senderName" -> name, "senderId" -> clientId))
            }

          case (invalid, parsed) =>
            // LocalNode -> client
            respondSockJsText(
              parse2JSON(
                Map(
                  "error"   -> INVALID_TAG,
                  "tag"     -> "system",
                  "seq"     -> parsed.getOrElse("seq", -1),
                  "message" -> s"Invalid tag:${invalid}. Tag must be `subscribe` or `unsubscribe` or `pull` or `push`."
                )
              )
            )
        }

      case Terminated(hub) =>
        log.warn("Hub is terminatad")
        // Retry to lookup hub
        Thread.sleep(100L * (scala.util.Random.nextInt(3) + 1))
        lookUpHub(hubKey, hubProps, option)

      case ignore =>
        log.warn(s"Unexpected message: $ignore")
    }
  }

  private def parse2MapWithTag(jsonStr: String): (String, Map[String, String]) = {
    SeriDeseri.fromJson[Map[String, String]](jsonStr) match {
      case Some(json) =>
          (json.getOrElse("tag", "invalidTag"), json)
      case None =>
        log.warn(s"Failed to parse request: $jsonStr")
        ("invalid", Map.empty)
    }
  }

  private def parse2JSON(ref: AnyRef) = SeriDeseri.toJson(ref)
}
