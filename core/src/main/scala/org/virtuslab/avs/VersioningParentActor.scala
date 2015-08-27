package org.virtuslab.avs

import akka.actor._
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._

/**
 * @author Mikołaj Jakubowski
 */
object VersioningParentActor {

  /**
   * message containing new version of actor
   */
  case class NewVersion[A](clazz: Class[A])

  /**
   * message containing class of actor to revert to
   */
  case class RevertTo(clazz: Option[Class[_]])

  def props(props: Props, actorClassRepository: ActorRef) = Props(new VersioningParentActor(props, actorClassRepository))

  class ChildTerminatedException extends Exception

}

/**
 * actor that takes care of versioning if some user defined actor
 * @param props - props of actor to be versioned
 * @param actorClassRepository - actorRef of actor class repository
 */
class VersioningParentActor(props: Props, actorClassRepository: ActorRef) extends Actor with ActorLogging {

  import VersioningParentActor._
  import ActorClassRepository._

  var child = context.actorOf(props)
  var currentProps = props

  context.watch(child)
  actorClassRepository ! Register(props.actorClass().getName)

  /**
   * supervision strategy - if child dies find previous version
   */
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 0, withinTimeRange = 1.minute) {
      case _ => {
        if (props == currentProps) Escalate
        else {
          actorClassRepository ! PreviousVersionOf(currentProps.actorClass().getName)
          Stop
        }
      }
    }

  def receive = {
    case newVersion: NewVersion[_] => {
      //new version of child appeared
      val previousChild = child
      currentProps = Props(newVersion.clazz)
      child = context.actorOf(currentProps)
      log.debug("Switched actor impl to {}", newVersion.clazz.getName)
      actorClassRepository ! Register(newVersion.clazz.getName)
      previousChild ! PoisonPill
    }
    case Terminated(ref) if ref == child => {
      //child terminated
      log.debug("Child terminated")
      actorClassRepository ! PreviousVersionOf(currentProps.actorClass().getName)
    }
    case revertTo: RevertTo => {
      //revert child
      revertTo.clazz.map { clazz =>
        currentProps = Props(clazz, props.args)
        child = context.actorOf(currentProps)
        log.debug("Reverted actor impl to {}", clazz.getName)
        actorClassRepository ! Register(clazz.getName)
      }.getOrElse {
        if (currentProps != props) {
          currentProps = props
          child = context.actorOf(currentProps)
          log.debug("Reverted actor impl to base")
          actorClassRepository ! Register(props.actorClass().getName)
        }
      }
    }
    case msg => child.forward(msg) //if not versioning message - forward to child
  }

}
