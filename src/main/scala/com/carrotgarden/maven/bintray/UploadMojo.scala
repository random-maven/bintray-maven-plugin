package com.carrotgarden.maven.bintray

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations._
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import okhttp3.Request

import com.carrotgarden.maven.tools.Description
import java.nio.file.Path

@Description( """
Upload local file content, such as Eclipse P2 repository to existing Bintray repository.
Goal operates via <a href="https://bintray.com/docs/api/">Bintray REST API</a>.
""" )
@Mojo(
  name            = "upload",
  defaultPhase    = LifecyclePhase.DEPLOY,
  requiresProject = true
)
class UploadMojo extends AbstractMojo
  with BuildApi
  with BintrayApi
  with BaseParams
  with BaseExecute {

  @Description( """
  Local source folder for content upload. Absolute path.
  """ )
  @Parameter( property     = "bintray.sourceFolder", defaultValue = "${project.build.directory}/repository" )
  var sourceFolder : File = _

  @Description( """
  Remote target folder for content upload. Relative path.
  """ )
  @Parameter( property     = "bintray.targetFolder", defaultValue = "repository" )
  var targetFolder : String = _

  @Description( """
  Bintray resource publication mode. Corresponds to <code>X-Bintray-Publish</code> rest header.
  """ )
  @Parameter( property     = "bintray.bintrayPublish", defaultValue = "1" )
  var bintrayPublish : String = _

  @Description( """
  Bintray resource publication mode. Corresponds to <code>X-Bintray-Override</code> rest header.
  """ )
  @Parameter( property     = "bintray.bintrayOverride", defaultValue = "1" )
  var bintrayOverride : String = _

  @Description( """
  Bintray resource publication mode. Corresponds to <code>X-Bintray-Explode</code> rest header.
  """ )
  @Parameter( property     = "bintray.bintrayExplode", defaultValue = "0" )
  var bintrayExplode : String = _

  @Description( """
  Execution step 3. optionally enable content cleanup in remote repository.
  Filter parameter: <a href="#cleanupRegex"><b>cleanupRegex</b></a>.
  """ )
  @Parameter( property     = "bintray.performCleanup", defaultValue = "true" )
  var performCleanup : Boolean = _

  @Description( """
  Regular expression used to select files for cleanup.
  Matched against remote file relative path.
  Matches no files by default.
  """ )
  @Parameter( property     = "bintray.cleanupRegex", defaultValue = "" )
  var cleanupRegex : String = _

  @Description( """
  Execution step 4. optionally enable content upload from local directory.
  Filter parameter: <a href="#uploadRegex"><b>uploadRegex</b></a>.
  """ )
  @Parameter( property     = "bintray.performUpload", defaultValue = "true" )
  var performUpload : Boolean = _

  @Description( """
  Regular expression used to select files for upload.
  Matched against local file absolute path.
  Matches all files by default.
  """ )
  @Parameter( property     = "bintray.uploadRegex", defaultValue = """^.+$""" )
  var uploadRegex : String = _

  /**
   * Content processing headers: override existing version, etc.
   */
  override def injectHeader( builder : Request.Builder ) = {
    builder.addHeader( "X-Bintray-Package", bintrayPackage )
    builder.addHeader( "X-Bintray-Version", bintrayVersion )
    builder.addHeader( "X-Bintray-Publish", bintrayPublish )
    builder.addHeader( "X-Bintray-Override", bintrayOverride )
    builder.addHeader( "X-Bintray-Explode", bintrayExplode )
  }

  /**
   * Content cleanup in remote repository.
   */
  def cleanupContent() : Unit = {
    getLog().info( s"Cleaning remote content matching '${cleanupRegex}' from '${bintrayPackage}'" )
    if ( hasEmptyString( cleanupRegex ) ) {
      return
    }
    val matcher = cleanupRegex.r.pattern.matcher( "" )
    def hasMatch( path : String ) : Boolean = {
      matcher.reset( path ).matches
    }
    packageListPath().filter( hasMatch( _ ) )
      .foreach { remote =>
        getLog().info( s"   Remote path: ${remote}" )
        contentCleanup( remote )
      }
  }

  /**
   * Construct remote upload path.
   */
  def remoteTarget( remote : String ) : String =
    if ( hasEmptyString( targetFolder ) ) {
      remote
    } else {
      s"${targetFolder}/${remote}"
    }

  /**
   * Upload file content from local directory.
   */
  def uploadContent() : Unit = {
    getLog().info( s"Uploading local content matching '${uploadRegex}' from '${sourceFolder}'" )
    require( !hasEmptyString( uploadRegex ), "Upload regex can not be empty." )
    val matcher = uploadRegex.r.pattern.matcher( "" )
    def hasMatch( path : Path ) : Boolean = {
      Files.isRegularFile( path ) && matcher.reset( path.toAbsolutePath.toString ).matches
    }
    val root = sourceFolder.toPath
    Files.walk( root ).filter( hasMatch( _ ) )
      .forEach { local =>
        val remote = remoteTarget( root.relativize( local ).toString )
        getLog().info( s"   Remote path: ${remote}" )
        contentUpload( local.toFile, remote.toString )
      }
  }

  override def perform() : Unit = {
    if ( performDestroy ) {
      destroyPackage()
    }
    if ( performEnsure ) {
      ensurePackage()
    }
    if ( performCleanup ) {
      cleanupContent()
    }
    if ( performUpload ) {
      uploadContent()
    }
  }

}
