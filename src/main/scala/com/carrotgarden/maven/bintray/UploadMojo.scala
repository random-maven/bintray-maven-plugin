package com.carrotgarden.maven.bintray

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations._
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import okhttp3.Request

import com.carrotgarden.maven.tools.Description

@Description( """
Upload local file content, such as Eclipse P2 repository to existing Bintray repository.
Goal operates via <a href="https://bintray.com/docs/api/">Bintray rest api</a>.
</a>.
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
  Execution step 3. Enable content upload from local directory.
  """ )
  @Parameter( property     = "bintray.performUpload", defaultValue = "true" )
  var performUpload : Boolean = _

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
   * Construct remote upload path.
   */
  def remoteTarget( remote : String ) : String =
    if ( targetFolder == null || targetFolder == "" ) {
      remote
    } else {
      targetFolder + "/" + remote
    }

  /**
   * Upload file content from local directory.
   */
  def uploadContent() = {
    getLog().info( "Uploading file content: " + sourceFolder );
    val root = sourceFolder.toPath()
    Files.walk( root ).filter( Files.isRegularFile( _ ) )
      .forEach { local =>
        val remote = remoteTarget( root.relativize( local ).toString() )
        getLog().info( "Remote path: " + remote );
        contentUpload( local.toFile(), remote.toString() )
      }
  }

  override def perform() : Unit = {
    if ( performDestroy ) {
      destroyPackage()
    }
    if ( performEnsure ) {
      ensurePackage()
    }
    if ( performUpload ) {
      uploadContent()
    }
  }

}
