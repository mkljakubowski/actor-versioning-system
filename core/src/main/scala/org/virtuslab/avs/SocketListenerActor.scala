package org.virtuslab.avs

import akka.actor._
import akka.io._
import java.io.Serializable
import java.net.InetSocketAddress
import akka.util.ByteString
import akka.actor.Terminated
import org.virtuslab.avs.helper.ObjectSerializationHelper

object SocketListenerActor {

  /**
   * new actor implementation message that is sent over network
   * @param fqnFrom - fqn of actor from which to change
   * @param fqnTo - fqn of actor to which to change
   * @param bytecode - bytecode of new actor
   */
  case class ActorImplementation(fqnFrom: String, fqnTo: String, bytecode: Array[Byte]) extends Serializable

  def props(actorClassRepository: ActorRef) = Props(classOf[SocketListenerActor], actorClassRepository)
}

/**
 * actor for handling incoming connection to TCP port
 * @author Mikołaj Jakubowski
 */
class SocketListenerActor(actorClassRepository: ActorRef) extends Actor with ActorLogging {

  import context.system

  //bind to address to listen for incoming connections
  IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress("0.0.0.0", 10001))

  var connectionHandler: ActorRef = _

  def receive = {
    case Tcp.Bound(addr) => log.debug(s"Bound to address ${addr.getHostName}:${addr.getPort}") //bound to address successfully
    case Tcp.Connected(remoteAddr, localAddr) => {
      // new connection - spawn new child
      log.debug(s"Remote address $remoteAddr $localAddr connected")
      connectionHandler = context.actorOf(Props(new ConnectionHandler(sender(), actorClassRepository)))
      sender() ! Tcp.Register(connectionHandler, keepOpenOnPeerClosed = true)
    }
    case msg => log.error(msg.toString)
  }

}

/**
 * actor for handling open connections over TCP
 * @param connection
 * @param actorClassRepository
 */
class ConnectionHandler(connection: ActorRef, actorClassRepository: ActorRef) extends Actor with ActorLogging {

  import SocketListenerActor._

  context.watch(connection)

  def receive = receivingImpl

  var msgToSendToRepo: ActorClassRepository.ActorImplementation = _
  val cl = getClass.getClassLoader.loadClass(classOf[AVSClassLoader].getName).getConstructor(classOf[ClassLoader]).newInstance(classOf[AVSClassLoader].getClassLoader).asInstanceOf[AVSClassLoader]

  //handling of connection problems
  def handleConnectionClose: Receive = {
    case Terminated(`connection`) =>
      log.debug(s"Stopping, because connection for remote address died")
      context.stop(self)
    case _: Tcp.ConnectionClosed =>
      log.debug(s"Stopping, because connection for remote address closed")
      context.stop(self)
    case cmd: Tcp.CommandFailed => log.error("command failed: {} in {}", cmd, cmd.cmd)
  }

  //receive data
  def receivingImpl: Receive = handleConnectionClose orElse {
    case Tcp.Received(data) => handleNewImplementation(data)
    case msg => log.error(msg.toString)
  }

  //deserialize data and sent it up the stream to class repository to get propagated in system
  def handleNewImplementation(data: ByteString) {
    val actorImpls = ObjectSerializationHelper.deserialize[Seq[ActorImplementation]](data)
    log.debug(s"Received ${actorImpls.size} class impls")
    log.debug(actorImpls.map(_.fqnTo).mkString(" "))

    actorImpls.foreach { actorImpl =>
      log.debug(s"Processing ${actorImpl.fqnTo}")
      if (actorImpl.fqnFrom.nonEmpty) {
        log.debug("it was an actor")
        val actorClass = cl.defineClassFromRemote(actorImpl.fqnTo, actorImpl.bytecode, actorImpl.fqnFrom.nonEmpty)
        msgToSendToRepo = ActorClassRepository.ActorImplementation(actorImpl.fqnFrom, actorImpl.fqnTo, actorClass)
      } else {
        cl.defineClassFromRemote(actorImpl.fqnTo, actorImpl.bytecode, actorImpl.fqnFrom.nonEmpty)
      }
    }
    log.debug("Finished loading classes")
    actorClassRepository ! msgToSendToRepo
    context.stop(self)
  }

}