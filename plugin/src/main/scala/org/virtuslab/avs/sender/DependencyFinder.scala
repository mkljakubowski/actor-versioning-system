package org.virtuslab.avs.sender

import com.sun.org.apache.bcel.internal.classfile._
import java.io.File

/**
 * findes dependency of actor class in current user classpath
 * @author Mikołaj Jakubowski
 */
object DependencyFinder {

  class DependencyFinder(javaClass: JavaClass) extends EmptyVisitor {
    var classes = Seq.empty[String]

    override def visitConstantClass(obj: ConstantClass) {
      val cp = javaClass.getConstantPool
      classes +:= obj.getBytes(cp)
    }
  }

  var classes = Seq.empty[String]

  def findDepsOfClass(fqn: String, classesDir: String): Set[String] = {
    println(fqn)
    try {
      val actorPath = fqn.split("\\.").mkString("/") + ".class"
      val cp = new ClassParser(s"$classesDir/$actorPath")
      val javaClass = cp.parse()
      val visitor = new DependencyFinder(javaClass)
      val classWalker = new DescendingVisitor(javaClass, visitor)
      classWalker.visit()
      visitor.classes
        .filterNot(_ == fqn)
        .filterNot(classes.contains)
        .filter {
        name =>
          val classFile = name.split("\\.").mkString("/") + ".class"
          val classPath = s"$classesDir/$classFile"
          new File(classPath).exists()
      }.flatMap { name =>
        classes +:= name
        findDepsOfClass(name, classesDir)
      }.toSet
    } catch {
      case _: Throwable =>
        Set.empty[String]
    }
  }

  def findLocalDepsOfClass(fqn: String, classesDir: String): Set[String] = {
    classes = Seq(fqn)
    findDepsOfClass(fqn, classesDir)
    classes.map(_.replace("/", ".")).toSet
  }

}