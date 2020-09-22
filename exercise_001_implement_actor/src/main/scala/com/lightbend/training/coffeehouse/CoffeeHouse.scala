package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging}

class CoffeeHouse extends Actor with ActorLogging{
  override def receive: Receive = {
    case _ => log.info("Coffee Brewing")
  }
}
