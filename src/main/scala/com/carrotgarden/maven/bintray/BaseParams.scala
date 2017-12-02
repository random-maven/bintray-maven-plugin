package com.carrotgarden.maven.bintray

import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings
import org.apache.maven.plugins.annotations._

/**
 * Shared mojo execution configuration parameters.
 */
trait BaseParams {

  /**
   * Maven project providing deployment artifacts.
   */
  @Parameter( defaultValue = "${project}", required = true, readonly = true )
  var project : MavenProject = _

  /**
   * Maven session during execution.
   */
  @Parameter( defaultValue = "${session}", required = true, readonly = true )
  var session : MavenSession = _

  /**
   * Expose server credentials from settings.xml.
   */
  @Parameter( defaultValue = "${settings}", required = true, readonly = true )
  var settings : Settings = _

  /**
   * Bintray rest-api-user for authentication. When missing, uses
   * {@link #serverId}: {server/username} from settings.xml.
   */
  @Parameter( property = "bintray.username" )
  var username : String = _

  /**
   * Bintray rest-api-token for authentication. When missing, uses
   * {@link #serverId}: {server/password} from settings.xml.
   */
  @Parameter( property = "bintray.password" )
  var password : String = _

  /**
   * Server id for credentials lookup via {@link serverId}: { server/username,
   * server/password } in maven settings.xml.
   */
  @Parameter( property     = "bintray.serverId", defaultValue = "distro-bintray" )
  var serverId : String = _

  //     * Package can be optionally created on demand before deployment via {@link #performEnsure}.

  /**
   * Bintray target repository package. Corresponds to X-Bintray-Package
   */
  @Parameter( property     = "bintray.bintrayPackage", defaultValue = "${project.artifactId}" )
  var bintrayPackage : String = _

  /**
   * Bintray target repository version. Corresponds to X-Bintray-Version
   */
  @Parameter( property     = "bintray.bintrayVersion", defaultValue = "${project.version}" )
  var bintrayVersion : String = _

  /**
   * Bintray package create definition parameter: version control system url.
   */
  @Parameter( property     = "bintray.packageVcsUrl", defaultValue = "${project.url}" )
  var packageVcsUrl : String = _

  /**
   * Bintray package create definition parameter: licenses list to attach to the
   * target package.
   */
  @Parameter( property     = "bintray.packageLicenses", defaultValue = "Apache-2.0" )
  var packageLicenses : Array[ String ] = _

  /**
   * Bintray user or organization name which contains target maven repository
   * {@link #repository}.
   */
  @Parameter( property     = "bintray.subject", defaultValue = "${user.name}" )
  var subject : String = _

  /**
   * Bintray target repository name. Repository must already exist for the
   * bintray {@link #subject}.
   */
  @Parameter( property     = "bintray.repository", defaultValue = "repo" )
  var repository : String = _

  /**
   * Deploy behaviour: during deployment cleanup, preserve versions with the
   * version description matching given java regular expression.
   */
  @Parameter( property     = "bintray.preserveRegex", defaultValue = "(PRESERVE)" )
  var preserveRegex : String = _

  /**
   * Optionally skip all steps of the deployment execution.
   */
  @Parameter( property     = "bintray.skip", defaultValue = "false" )
  var skip : Boolean = _

  /**
   * Execution step 1: optionally remove target bintray package with all versions and
   * files.
   */
  @Parameter( property     = "bintray.performDestroy", defaultValue = "false" )
  var performDestroy : Boolean = _

  /**
   * Execution step 2: optionally create target bintray package before deployment.
   * Use create parameters: {@link #packageName}, {@link #packageVcsUrl},
   * {@link #packageLicenses}
   */
  @Parameter( property     = "bintray.performEnsure", defaultValue = "true" )
  var performEnsure : Boolean = _

}
