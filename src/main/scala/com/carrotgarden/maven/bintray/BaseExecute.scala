package com.carrotgarden.maven.bintray

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException

/**
 * Shared mojo execution process steps.
 */
trait BaseExecute { self : BaseParams with BuildApi with AbstractMojo =>

  /**
   * Actually perform goal execution.
   */
  def perform() : Unit

  override def execute() : Unit = {
    try {
      if ( skip ) {
        getLog().info( "Skipping plugin goal execution." );
        return ;
      }
      if ( hasIncremental ) {
        getLog().info( "Skipping incremental execution." );
        return ;
      }
      perform()
    } catch {
      case e : Throwable => throw new MojoFailureException( "Execution error", e );
    }
  }

}
