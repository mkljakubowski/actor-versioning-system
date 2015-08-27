package org.virtuslab.avs.sender

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import org.virtuslab.avs.core.BootedCore

/**
 * sender module - to be used in remote system gathering actor classes to be sent over network
 * @author Mikołaj Jakubowski
 */
class SenderModule(system: ActorSystem, classDir: String, remote: InetSocketAddress) extends BootedCore(system) {

  def senderRef = system.actorOf(SenderActor.props(classDir, remote))

}
