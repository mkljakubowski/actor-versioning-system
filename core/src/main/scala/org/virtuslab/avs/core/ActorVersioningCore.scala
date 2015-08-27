package org.virtuslab.avs.core

import akka.actor.ActorSystem
import org.virtuslab.avs.{SocketListenerActor, ActorClassRepository}

/**
 * class to be integrated in main application cake - enables actor versioning system
 * @author Mikołaj Jakubowski
 */
class ActorVersioningCore(system: ActorSystem) extends BootedCore(system) {

  implicit val repository = system.actorOf(ActorClassRepository.props())

  private val socketActor = system.actorOf(SocketListenerActor.props(repository))

}
