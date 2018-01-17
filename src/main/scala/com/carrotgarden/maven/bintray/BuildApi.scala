package com.carrotgarden.maven.bintray

import org.sonatype.plexus.build.incremental.BuildContext;

import org.apache.maven.plugins.annotations.Component;

import com.carrotgarden.maven.tools.Description

trait BuildApi {

  self : BaseParams =>

  @Description( """
  Eclipse build integration context.
  """ )
  @Component()
  var buildContext : BuildContext = _

  /**
   * Project pom.xml open in IDE.
   */
  def sourcePomFile = project.getModel().getPomFile();

  /**
   * Remove build messages
   */
  def contextReset = buildContext.removeMessages( sourcePomFile );

  /**
   * Display build warnings
   */
  def contextWarn( message : String, error : Throwable = null ) =
    buildContext.addMessage( sourcePomFile, 1, 1, message, BuildContext.SEVERITY_WARNING, error );

  /**
   * Display build errors
   */
  def contextError( message : String, error : Throwable = null ) =
    buildContext.addMessage( sourcePomFile, 1, 1, message, BuildContext.SEVERITY_ERROR, error );

  /**
   * Detect incremental invocation from IDE.
   */
  def hasIncremental = buildContext.isIncremental()

}
