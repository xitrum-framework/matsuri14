package matsuri.demo

import xitrum.Server

import matsuri.demo.actor.Hub

object Boot {
  def main(args: Array[String]) {
    Hub.start()
    Server.start()
  }
}
