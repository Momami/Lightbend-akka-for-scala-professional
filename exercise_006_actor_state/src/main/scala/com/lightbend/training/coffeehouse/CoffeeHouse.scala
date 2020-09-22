package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object CoffeeHouse {
  case class CreateGuest(favoriteCoffee: Coffee)
  def props(): Props =
    Props(new CoffeeHouse)
}

class CoffeeHouse extends Actor with ActorLogging{
  import CoffeeHouse._

  log.debug("CoffeeHouse Open")
  private val waiter = createWaiter

  override def receive: Receive = {
    case CreateGuest(favoriteCoffee) => createGuest(favoriteCoffee)
  }

  protected def createGuest(favoriteCoffee: Coffee): ActorRef =
    context.actorOf(Guest.props(waiter, favoriteCoffee))

  protected def createWaiter(): ActorRef = context.actorOf(Waiter.props(), "waiter")
}
