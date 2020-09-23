package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.routing.FromConfig

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.collection.immutable.Map

object CoffeeHouse {
  case class CreateGuest(favoriteCoffee: Coffee, caffeineLimit: Int)
  case class ApproveCoffee(coffee: Coffee, guest: ActorRef)
  case class Status(guestCount: Int)
  case object GetStatus


  def props(caffeineLimit: Int): Props =
    Props(new CoffeeHouse(caffeineLimit))
}

class CoffeeHouse(caffeineLimit: Int) extends Actor with ActorLogging{
  import CoffeeHouse._

  log.debug("CoffeeHouse Open")
  private val waiter = createWaiter()
  private val finishCoffeeDuration: FiniteDuration =
    context.system.settings.config.getDuration(
      "coffee-house.guest.finish-coffee-duration",
    TimeUnit.MILLISECONDS).millis
  private val prepareCoffeeDuration: FiniteDuration =
    context.system.settings.config.getDuration(
      "coffee-house.barista.prepare-coffee-duration",
      TimeUnit.MILLISECONDS).millis
  private val maxComplaintCount: Int =
    context.system.settings.config.getInt(
      "coffee-house.waiter.max-complaint-count"
    )
  private val accuracy: Int =
    context.system.settings.config.getInt(
      "coffee-house.barista.accuracy")
  private val barista = createBarista()
  private var guestBook: Map[ActorRef, Int] = Map.empty.withDefaultValue(0)

  override def receive: Receive = {
    case CreateGuest(favoriteCoffee, guestCaffeineLimit) =>
      val guest = createGuest(favoriteCoffee, guestCaffeineLimit)
      guestBook = guestBook + (guest -> 0)
      log.info(s"Guest $guest added to guest book")
      context.watch(guest)
    case ApproveCoffee(coffee, guest)
      if (guestBook(guest) < caffeineLimit) =>
        barista.forward(Barista.PrepareCoffee(coffee, guest))
        log.info(s"Guest $guest caffeine count incremented.")
        guestBook += guest -> (guestBook(guest) + 1)
    case ApproveCoffee(_, guest) =>
        log.info(s"Sorry, $guest, but you have reached your limit.")
        context.stop(guest)
    case GetStatus => sender() ! Status(guestBook.size)
    case Terminated(guest) =>
      log.info(s"Thanks $guest, for being our guest!")
      guestBook -= guest
  }

  protected def createGuest(favoriteCoffee: Coffee, guestCaffeineLimit: Int): ActorRef =
    context.actorOf(Guest.props(waiter, favoriteCoffee, finishCoffeeDuration, guestCaffeineLimit))

  protected def createWaiter(): ActorRef =
    context.actorOf(Waiter.props(self, barista, maxComplaintCount), "waiter")

  protected def createBarista(): ActorRef =
    context.actorOf(FromConfig.props(
      Barista.props(prepareCoffeeDuration, accuracy)), "barista")

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case Guest.CaffeineException => SupervisorStrategy.Stop
      case Waiter.FrustratedException(coffee, guest) =>
        barista.forward(Barista.PrepareCoffee(coffee, guest))
        SupervisorStrategy.Restart
    }
    OneForOneStrategy() {
      decider.orElse(super.supervisorStrategy.decider)
    }
  }
}
