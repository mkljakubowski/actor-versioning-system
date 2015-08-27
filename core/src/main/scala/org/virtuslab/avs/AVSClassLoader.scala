package org.virtuslab.avs

import java.net.URLClassLoader

/**
 * classloader enabling to set new actor's environment correctly
 * @author Mikołaj Jakubowski
 */
class AVSClassLoader(parent: ClassLoader) extends URLClassLoader(Array.empty, parent) {

  def defineClassFromRemote(name: String, bytes: Array[Byte], resolve: Boolean): Class[_] = {
    val c = defineClass(name, bytes, 0, bytes.length)
    if (resolve) {
      resolveClass(c)
    }
    c
  }

}
