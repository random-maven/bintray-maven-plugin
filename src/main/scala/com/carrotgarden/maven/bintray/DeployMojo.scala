package com.carrotgarden.maven.bintray

import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Map

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory
import org.apache.maven.artifact.repository.Authentication
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout
import org.apache.maven.plugin.deploy.AbstractDeployMojo
import org.apache.maven.plugins.annotations._

/**
 * <p>
 * Deploy maven project artifacts to existing bintray maven repository.
 * <p>
 * Goal operates via bintray rest api:
 * <a href="https://bintray.com/docs/api/">https://bintray.com/docs/api/</a>.
 * <p>
 * Optional features:
 * <ul>
 * <li>create bintray repository package on-demand
 * <li>remove previous versions from bintray package
 * </ul>
 */
@Mojo( name            = "deploy", defaultPhase = LifecyclePhase.DEPLOY, requiresProject = true )
class DeployMojo extends AbstractDeployMojo
  with BuildApi
  with BintrayApi
  with BaseParams
  with BaseExecute {

  /**
   * Execution step 3: actually do invoke artifact deployment to the target
   * repository.
   */
  @Parameter( property     = "bintray.performDeploy", defaultValue = "true" )
  var performDeploy : Boolean = _

  /**
   * Execution step 4: optionally mark deployment artifact as "published for bintray
   * consumption" after the deployment.
   */
  @Parameter( property     = "bintray.performPublish", defaultValue = "true" )
  var performPublish : Boolean = _

  /**
   * Execution step 5: optionally remove previous versions with files from target
   * bintray package after the deployment. Preserve select versions via
   * {@link #preserveRegex}.
   */
  @Parameter( property     = "bintray.performCleanup", defaultValue = "true" )
  var performCleanup : Boolean = _

  /**
   * Location of deployment artifact.
   */
  @Parameter( property = "project.build.finalName", required = false, readonly = true )
  var finalName : String = _

  /**
   * Location of deployment artifact.
   */
  @Parameter( property = "project.build.directory", required = false, readonly = true )
  var buildDirectory : String = _

  /**
   * Parameter used to control how many times a failed deployment will be retried
   * before giving up and failing. If a value outside the range 1-10 is specified
   * it will be pulled to the nearest value within the range 1-10.
   */
  @Parameter( property     = "retryFailedDeploymentCount", defaultValue = "1" )
  var retryFailedDeploymentCount : Int = _

  /**
   * Repository materialization factory.
   */
  @Component
  var repositoryFactoryX : ArtifactRepositoryFactory = _

  /**
   * Repository layout definitions.
   */
  @Component( role = classOf[ ArtifactRepositoryLayout ] )
  var repositoryLayoutsX : Map[ String, ArtifactRepositoryLayout ] = _

  /**
   * Invoke standard maven deployment process with custom bintray maven
   * repository.
   */
  def executeDeploy() = {

    getLog().info( "Deploying package: " + bintrayPackage );

    // Create list of artifacts to be deployed.
    val artifactList = new ArrayList[ Artifact ]();
    artifactList.add( project.getArtifact() );
    artifactList.addAll( project.getAttachedArtifacts() );

    val url = urlMavenDeploy();

    // Create artifact repository.
    val repository = repositoryFactoryX //
      .createDeploymentArtifactRepository( //
        serverId, url,
        repositoryLayoutsX.get( "default" ), false
      );

    // Set authentication if user and key are passed to properties.
    if ( username != null && password != null ) {
      repository.setAuthentication( new Authentication( username, password ) );
    }

    // Deploy artifacts.
    artifactList.forEach { artifact =>

      var deployFilePath = if ( artifact.getFile() != null ) artifact.getFile().toPath() else null;

      if ( deployFilePath == null || !Files.exists( deployFilePath ) ) {
        if ( deployFilePath != null ) {
          getLog().debug( "Artifact " + artifact + " doesn't exist in " + deployFilePath );
        }
        deployFilePath = Paths.get( buildDirectory, finalName + "." + artifact.getType() );
      }

      if ( !Files.exists( deployFilePath ) && artifact.getType().equals( "pom" ) ) {
        getLog().debug( "Artifact " + artifact + " doesn't exist in " + deployFilePath );
        deployFilePath = Paths.get( buildDirectory ).getParent().resolve( "pom.xml" );
      }

      if ( Files.exists( deployFilePath ) ) {
        deploy( deployFilePath.toFile(), artifact, repository, getLocalRepository(), retryFailedDeploymentCount );
      } else {
        getLog().debug( "Artifact " + artifact + " doesn't exist in " + deployFilePath );
        getLog().warn( "Cannot deploy artifact " + artifact + ", no file to deploy" );
      }

    }

  }

  override def perform() : Unit = {
    if ( performDestroy ) {
      destroyPackage();
    }
    if ( performEnsure ) {
      ensurePackage();
    }
    if ( performDeploy ) {
      executeDeploy();
    }
    if ( performPublish ) {
      contentPublish();
    }
    if ( performCleanup ) {
      contentCleanup();
    }
  }

}
