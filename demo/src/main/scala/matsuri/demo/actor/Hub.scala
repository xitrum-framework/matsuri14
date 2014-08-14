package matsuri.demo.actor

import akka.actor.{Actor, ActorRef, Props, Terminated}
import glokka.Registry
import xitrum.{Config, Log}

case class Done(option: Map[String, Any] = Map.empty)        // Hub       -> LocalNode
case class Publish(option: Map[String, Any] = Map.empty)     // Hub       -> LocalNode
case class Pull(option: Map[String, Any] = Map.empty)        // LocalNode -> Hub
case class Push(option: Map[String, Any] = Map.empty)        // LocalNode -> Hub
case class Subscribe(option: Map[String, Any] = Map.empty)   // LocalNode -> Hub
case class Unsubscribe(option: Map[String, Any] = Map.empty) // LocalNode -> Hub

object Hub {
  val KEY_PROXY = "HUB_PROXY"

  // Glokka registry
  val actorRegistry = Registry.start(Config.actorSystem, KEY_PROXY)

  // To force start registry at process start up,
  // Call this method at `main` before start `xitrum.Server`
  def start() {}
}

trait Hub extends Actor {
  protected var clients = Seq[ActorRef]()
  private lazy val node = self.toString

  def receive = {
    case Push(option) =>
      Log.debug(s"[Hub][${node}] Received Push request")
      val result = handlePush(option)
      clients.foreach { client =>
        if (client != sender) client ! Publish(result)
      }
      sender ! Done(result)

    case Pull(option) =>
      Log.debug(s"[Hub][${node}] Received Pull request")
      sender ! Done(handlePull(option))

    case Subscribe(option) =>
      Log.debug(s"[Hub][${node}] Received Subscribe request")
      if (!clients.contains(sender)) {
        clients = clients :+ sender
        context.watch(sender)
        val result = handleSubscribe(sender, option)
        clients.foreach { client =>
          if (client != sender) client ! Publish(result)
        }
        sender ! Done(result)
      }

    case Unsubscribe(option) =>
      Log.debug(s"[Hub][${node}] Received Unsubscribe request")
      clients = clients.filterNot(_ == sender)
      context.unwatch(sender)
      val result = handleUnsubscribe(sender, option)
      clients.foreach { client =>
        if (client != sender) client ! Publish(result)
      }
      sender ! Done(result)

    case Terminated(client) =>
      Log.debug(s"[Hub][${node}] Received Terminated event"+client.toString)
      clients = clients.filterNot(_ == client)
      val result = handleTerminated(client)
      clients.foreach { client =>
        if (client != sender) client ! Publish(result)
      }

    case ignore =>
      Log.warn(s"[Hub][${node}] Unexpected message: $ignore")
  }

  // Implement these method as you like
  def handlePush(option: Map[String, Any]): Map[String, Any]
  def handlePull(option: Map[String, Any]): Map[String, Any]
  def handleSubscribe(client: ActorRef,   option: Map[String, Any]): Map[String, Any]
  def handleUnsubscribe(client: ActorRef, option: Map[String, Any]): Map[String, Any]
  def handleTerminated(client: ActorRef): Map[String, Any]
}

trait HubClient extends Actor {
  protected lazy val node = self.toString

  def lookUpHub(key: String, hubProps: Props, option: Any = None) {
    Log.debug(s"[HubClient][${node}] Searching HUB node...")
    Hub.actorRegistry ! Registry.Register(key, hubProps)
    context.become {
      case result: Registry.FoundOrCreated => doWithHub(result.ref, option)
    }
  }

  // Implement these method as you like
  def doWithHub(publisher: ActorRef, option: Any)
}
