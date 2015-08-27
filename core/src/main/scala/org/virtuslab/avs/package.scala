package org.virtuslab

import akka.actor.{ActorRef, Props}

/**
 * @author Mikołaj Jakubowski
 */
package object avs {

  /**
   * implicit conversion to versioned Props to add `withVersioning` method
   */
  implicit def wrapPropsWithVersioning(props: Props): VersionedProps = new VersionedProps(props)

  class VersionedProps(props: Props) {

    /**
     * enable versioning of this actor Props
     * @return
     */
    def withVersioning(implicit actorClassRepository: ActorRef) = VersioningParentActor.props(props, actorClassRepository)

  }

}
