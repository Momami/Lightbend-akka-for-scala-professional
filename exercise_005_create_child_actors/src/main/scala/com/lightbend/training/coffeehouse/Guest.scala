package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, Props}

object Guest {
  def props(): Props = Props(new Guest)
}

class Guest extends Actor with ActorLogging{
  override def receive: Receive = Actor.emptyBehavior
}
