package org.virtuslab.avs.core

import akka.actor.ActorSystem

/**
 * bottom layer of application cake
 * @author Mikołaj Jakubowski
 */
trait Core {
  def system: ActorSystem
}

class BootedCore(val system: ActorSystem) extends Core {

  sys.addShutdownHook(system.shutdown())

}

