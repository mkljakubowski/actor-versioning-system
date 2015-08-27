package org.virtuslab.avs.helper

import akka.util.ByteString
import java.io.{ObjectOutputStream, ByteArrayOutputStream, ObjectInputStream, ByteArrayInputStream}

/**
 * serializes and deserializes object to send it over network easily
 * @author Mikołaj Jakubowski
 */
object ObjectSerializationHelper {

  def serialize[T](obj: T): ByteString = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(obj)
    oos.close()
    val bs = ByteString(baos.toByteArray)
    baos.close()
    bs
  }

  def deserialize[T](data: ByteString): T = {
    val in = new ByteArrayInputStream(data.toArray)
    val is = new ObjectInputStream(in)
    val obj = is.readObject().asInstanceOf[T]
    is.close()
    in.close()
    obj
  }
}
