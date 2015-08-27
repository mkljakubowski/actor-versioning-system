package org.virtuslab.avs.sender

import java.io.IOException
import java.nio.file.{Files, Paths}

/**
 * @author Mikołaj Jakubowski
 */
class LoggingClassLoader(parent: ClassLoader, classesDir: String) extends ClassLoader(parent) {

  var classNames = Seq.empty[(String, Array[Byte])]

  override def findClass(name: String): Class[_] = {
    val actorPath = name.split("\\.").mkString("/") + ".class"
    try {
      val bytecode = Files.readAllBytes(Paths.get(s"$classesDir/$actorPath"))
      val c = defineClass(name, bytecode, 0, bytecode.size)
      classNames = classNames :+(name, bytecode)
      c
    } catch {
      case e: IOException => super.findClass(name)
    }
  }

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    super.loadClass(name, resolve)
  }

}

object LoggingClassLoader {

  def dependencyFor(name: String, pcl: ClassLoader, classesDir: String): LoggingClassLoader = {
    new LoggingClassLoader(pcl, classesDir)
  }

}