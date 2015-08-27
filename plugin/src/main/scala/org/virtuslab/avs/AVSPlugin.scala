package org.virtuslab.avs

import org.virtuslab.avs.sender.{SenderModule, SenderActor}
import sbt._
import sbt.Keys._
import java.nio.file.{Paths, Files}
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import java.net.InetSocketAddress

/**
 * @author Mikołaj Jakubowski
 */
object AVSPlugin extends sbt.Plugin {

  object AVSKeys {
    val remoteAddress = SettingKey[String]("remote-addr", "Remote system address")
    val remotePort = SettingKey[Int]("remote-port", "Remote system port")
  }

  import AVSKeys._

  val swapActorImplCommand = Command.args("swap-actor-impl", "FQN of actor classes from which to which should be replaced") {
    (state, args) =>
      if (args.size == 2) {
        val fqnFrom = args(0)
        val fqnTo = args(1)
        val classesDir = Project.extract(state).get(classDirectory in Compile).toString
        val addr = Project.extract(state).get(remoteAddress)
        val port = Project.extract(state).get(remotePort)
        val remote = new InetSocketAddress(addr, port)
        val actorPath = fqnTo.split("\\.").mkString("/") + ".class"
        val bytecode = Files.readAllBytes(Paths.get(s"$classesDir/$actorPath"))
        val systemClassLoader = classOf[akka.actor.LightArrayRevolverScheduler].getClassLoader

        val senderModule = new SenderModule(ActorSystem("sender-system", ConfigFactory.load(), systemClassLoader), classesDir, remote)

        val fut = senderModule.senderRef.?(SenderActor.NewImpl(fqnFrom, fqnTo, bytecode))(Timeout(1.minute))
        Await.ready(fut, 5.minute)
        state.log.info("Version sent, shutting down")
        senderModule.system.shutdown()
      } else {
        state.log.error("You must specify exactly 2 FQNs")
      }

      state
  }

  val defaultSettings = Seq(
    commands ++= Seq(swapActorImplCommand),
    remotePort := 10001,
    remoteAddress := "127.0.0.1",
    libraryDependencies ++= Seq("org.virtuslab" %% "akka-avs-core" % "0.1-SNAPSHOT")
  )
}
