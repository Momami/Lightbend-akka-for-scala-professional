package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Guest {
  case object CoffeeFinished

  def props(waiter: ActorRef, favoriteCoffee: Coffee): Props =
    Props(new Guest(waiter, favoriteCoffee))
}

class Guest(waiter: ActorRef, favoriteCoffee: Coffee) extends Actor with ActorLogging{
  import Guest._

  private var coffeeCount: Int = 0

  override def receive: Receive = {
    case Waiter.CoffeeServed(coffee) => {
      coffeeCount += 1
      log.info(s"Enjoying my ${coffeeCount} yummy ${coffee}!")
    }
    case CoffeeFinished => waiter ! Waiter.ServeCoffee(favoriteCoffee)
  }
}
