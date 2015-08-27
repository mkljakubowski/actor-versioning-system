package org.virtuslab.avs.sender

import java.net.InetSocketAddress
import java.nio.file.{Files, Paths}

import akka.actor._
import akka.io.{IO, Tcp}
import org.virtuslab.avs.SocketListenerActor
import org.virtuslab.avs.helper.ObjectSerializationHelper

import scala.util.Success

object SenderActor {

  /**
   * message to be sent over netork containing classes to be upgraded in application system
   */
  case class NewImpl(fqnFrom: String, fqnTo: String, bytecode: Array[Byte])

  def props(classDir: String, remote: InetSocketAddress) = Props(new SenderActor(classDir, remote))

  case object Ack extends Tcp.Event

}

/**
 * actor class handling connection to application system
 * @author Mikołaj Jakubowski
 */
class SenderActor(classesDir: String, remote: InetSocketAddress) extends Actor with ActorLogging {

  import SenderActor._
  import context.system

  IO(Tcp) ! Tcp.Connect(remote)

  var delayedMessage: Option[(ActorRef, NewImpl)] = None
  var connection: ActorRef = _
  var ui: ActorRef = _
  var storage = List.empty[SocketListenerActor.ActorImplementation]
  var suspended = false

  def receive = {
    case Tcp.Connected(_, local) => {
      log.info("Connected to remote system")
      connection = sender()
      connection ! Tcp.Register(self, keepOpenOnPeerClosed = true)
      context.watch(connection)
      if (storage.isEmpty) {
        context.become(ready)
      } else {
        context.become(writeToConnection())
      }
    }
    case msg: NewImpl => {
      handleNeqImplMessage(sender(), msg)
    }
  }

  def ready: Receive = {
    case msg: NewImpl => {
      handleNeqImplMessage(sender(), msg)
      context.become(writeToConnection())
    }
  }

  def handleNeqImplMessage(ref: ActorRef, msg: NewImpl) {
    msg match {
      case NewImpl(fqnFrom, fqnTo, bytecode) =>
        log.info("Handling new impl message")
        ui = ref
        val neededClasses = DependencyFinder.findLocalDepsOfClass(fqnTo, classesDir)

        buffer(prepareMessageWithImplementation(fqnFrom, fqnTo, bytecode))
        neededClasses.filterNot(_ == fqnTo).foreach { case className =>
          val actorPath = className.split("\\.").mkString("/") + ".class"
          val code = Files.readAllBytes(Paths.get(s"$classesDir/$actorPath"))
          buffer(prepareMessageWithImplementation("", className, code))
        }
    }
  }

  def writeToConnection(): Receive = {
    log.info(s"st ${storage.size}")
    if (storage.isEmpty) {
      ui ! Success
      context.stop(self)
    } else {
      connection ! Tcp.Write(ObjectSerializationHelper.serialize(storage), Ack)
    }

    handleConnectionClose orElse {
      case Ack => log.info("Message with classes has been sent")
    }
  }

  private def buffer(data: SocketListenerActor.ActorImplementation): Unit = {
    storage = storage :+ data
  }

  def handleConnectionClose: Receive = {
    case Terminated(_) =>
      log.info("Stopping, because connection for remote address died")
      ui ! Success
      context.stop(self)
    case _: Tcp.ConnectionClosed =>
      log.info("Stopping, because connection for remote address closed")
      ui ! Success
      context.stop(self)
    case cmd: Tcp.CommandFailed => log.error("command failed: {} in {}", cmd, cmd.cmd)
  }

  def prepareMessageWithImplementation(fqnFrom: String, fqnTo: String, bytecode: Array[Byte]): SocketListenerActor.ActorImplementation = {
    SocketListenerActor.ActorImplementation(fqnFrom, fqnTo, bytecode)
  }

}
