package matsuri.demo.action

import akka.actor.{Actor, ActorRef, Props, Terminated}

import xitrum.{SockJsAction, SockJsText}
import xitrum.annotation.{GET, SOCKJS}
import xitrum.util.SeriDeseri

import matsuri.demo.actor.{HubClient, ChatHub, Done, Publish, Push, Pull, Subscribe, UnSubscribe}
import matsuri.demo.constant.ErrorCD._
import matsuri.demo.filter.LoginFilter
import matsuri.demo.session.SVar

@GET("chat")
class ChatIndex extends DefaultLayout with LoginFilter {
  def execute() {
    respondView()
  }
}

@SOCKJS("connect")
class Chat extends SockJsAction with HubClient {
  private val hubKey    = "glokkaExampleHub"
  private val hubProps  = Props[ChatHub]

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
//            log.debug(s"[HubClient][${clientId}] Send UnSubscribe request to HUB")
//            hub ! UnSubscribe(Map(
//                                "error"   -> STATUS_SUCCESS,
//                                "tag"     -> "system",
//                                "seq"     -> parsed.getOrElse("seq", -1)
//                              ))
//
          case ("pull", parsed) =>
            // LocalNode -> Hub (-> LocalNode)
            log.debug(s"[${clientId}] Send Pull request to HUB")
            hub ! Pull(parsed + ("clientId" -> clientId))

          case ("push", parsed) =>
            // LocalNode -> Hub (-> AnotherNode)
            log.debug(s"[HubClient][${clientId}] Send Push request to HUB")
            hub ! Push(parsed + ("senderName" -> name, "senderId" -> clientId))

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
