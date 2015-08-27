package org.virtuslab.avs

import akka.actor.{ActorLogging, ActorRef, Props, Actor}

/**
 * @author Mikołaj Jakubowski
 */
object ActorClassRepository {

  /**
   * new actor implementation message
   * @param fqnFrom fqn of actor to upgrade
   * @param fqnTo fqn of new actor
   * @param clazz class of new actor
   */
  case class ActorImplementation(fqnFrom: String, fqnTo: String, clazz: Class[_])

  /**
   * registration of actor to listen for fqn class upgrades message
   */
  case class Register(fqn: String)

  /**
   * message asking about previous implementation of fqn
   */
  case class PreviousVersionOf(fqn: String)

  def props() = Props[ActorClassRepository]
}

class ActorClassRepository extends Actor with ActorLogging {

  import ActorClassRepository._

  var implementations = Seq.empty[ActorImplementation]
  var registeredActors = Map.empty[ActorRef, String]

  def receive = {
    case impl: ActorImplementation => {
      //propagate new implementation among interested versioned actors
      implementations = implementations :+ impl
      log.debug("Class repository is propagating change from {} to {}", impl.fqnFrom, impl.fqnTo)
      registeredActors.filter { case (_, fqn) => fqn == impl.fqnFrom }.foreach {
        case (ref, _) =>
          ref ! VersioningParentActor.NewVersion(impl.clazz)
      }
    }

    case Register(fqn) => {
      //add sender to list of versioned actors
      registeredActors = registeredActors.updated(sender(), fqn)
    }

    case PreviousVersionOf(fqn) => {
      //find and return previous version of actor
      val fqnToRevertTo = implementations.filter(_.fqnTo == fqn).last.fqnFrom
      val classToRevertTo = implementations.filter(_.fqnTo == fqnToRevertTo).reverse.headOption.map(_.clazz)
      sender() ! VersioningParentActor.RevertTo(classToRevertTo)
    }
  }
}
