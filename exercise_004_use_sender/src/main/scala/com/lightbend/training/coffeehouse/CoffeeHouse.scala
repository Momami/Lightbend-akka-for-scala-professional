package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, Props}

object CoffeeHouse {
  def props(): Props =
    Props(new CoffeeHouse)
}

class CoffeeHouse extends Actor with ActorLogging{
  log.debug("CoffeeHouse Open")

  override def receive: Receive = {
    case _ => sender() ! "Coffee Brewing"
  }
}
