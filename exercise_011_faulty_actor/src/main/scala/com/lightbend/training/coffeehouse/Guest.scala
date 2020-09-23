package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Timers}

import scala.concurrent.duration.FiniteDuration

object Guest {
  case object CoffeeFinished
  case object CaffeineException extends IllegalStateException

  def props(waiter: ActorRef,
            favoriteCoffee: Coffee,
            finishCoffeeDuration: FiniteDuration,
            caffeineLimit: Int): Props =
    Props(new Guest(waiter, favoriteCoffee, finishCoffeeDuration, caffeineLimit))
}

class Guest(waiter: ActorRef,
            favoriteCoffee: Coffee,
            finishCoffeeDuration: FiniteDuration,
            caffeineLimit: Int)
  extends Actor with ActorLogging with Timers{
  import Guest._

  private var coffeeCount: Int = 0
  orderCoffee()

  override def receive: Receive = {
    case Waiter.CoffeeServed(coffee) =>
      coffeeCount += 1
      log.info(s"Enjoying my ${coffeeCount} yummy ${coffee}!")
      timers.startSingleTimer(
        "coffee-finished",
        CoffeeFinished,
        finishCoffeeDuration
      )
    case CoffeeFinished if coffeeCount > caffeineLimit => throw CaffeineException
    case CoffeeFinished => orderCoffee()
    case PoisonPill => context.stop(self)
  }

  override def postStop(): Unit = log.info("Goodbye!")

  def orderCoffee() =
    waiter ! Waiter.ServeCoffee(favoriteCoffee)
}
