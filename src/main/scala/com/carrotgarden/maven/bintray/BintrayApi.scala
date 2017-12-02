package com.carrotgarden.maven.bintray

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations._

import org.json.JSONObject

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.MultipartBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Bintray rest api. Entry point to the bintray resource management.
 */
trait BintrayApi { self : BaseParams with AbstractMojo =>

  /**
   * Bintray rest api url.
   */
  @Parameter( property     = "bintray.restApiUrl", defaultValue = "https://bintray.com/api/v1" )
  var restApiUrl : String = _

  /**
   * REST api connection timeout, seconds.
   */
  @Parameter( property     = "bintray.restConnectTimeout", defaultValue = "10" )
  var restConnectTimeout : Int = _

  /**
   * REST api read operation timeout, seconds.
   */
  @Parameter( property     = "bintray.restReadTimeout", defaultValue = "10" )
  var restReadTimeout : Int = _

  /**
   * REST api write operation timeout, seconds.
   */
  @Parameter( property     = "bintray.restWriteTimeout", defaultValue = "10" )
  var restWriteTimeout : Int = _

  /**
   * Bintray rest api data format.
   */
  lazy val TEXT = MediaType.parse( "text/plain" )
  lazy val JSON = MediaType.parse( "application/json; charset=utf-8" );
  lazy val BINARY = MediaType.parse( "application/octet-stream" )

  /**
   * Provide bintray rest api authentication from
   * {@code username}/{@code password} with fallback to the {@link #serverId}.
   */
  lazy val authenticator = new Authenticator() {
    @Override
    def authenticate( route : Route, response : Response ) = {
      var username = BintrayApi.this.username;
      var password = BintrayApi.this.password;
      if ( username == null || password == null ) {
        val server = settings.getServer( serverId );
        if ( server != null ) {
          username = server.getUsername();
          password = server.getPassword();
        }
      }
      val credentials = Credentials.basic( username, password );
      response.request().newBuilder().header( "Authorization", credentials ).build();
    }
  };

  /**
   * Bintray rest api client.
   */
  lazy val client = {
    new OkHttpClient.Builder()
      .authenticator( authenticator )
      .connectTimeout( restConnectTimeout, TimeUnit.SECONDS )
      .readTimeout( restReadTimeout, TimeUnit.SECONDS )
      .writeTimeout( restWriteTimeout, TimeUnit.SECONDS )
      .followRedirects( true )
      .followSslRedirects( true )
      .build();
  }

  /**
   * Maven upload url
   * <p>
   * https://bintray.com/docs/api/#_maven_upload
   */
  def urlMavenDeploy() = {
    // PUT /maven/:subject/:repo/:package/:file_path[;publish=0/1]
    restApiUrl + "/maven/" + subject + "/" + repository + "/" + bintrayPackage;
  }

  /**
   * Upload local file content. Must provide meta data via headers.
   */
  def urlContentUpload( relativePath : String ) = {
    restApiUrl + "/content/" + subject + "/" + repository + "/" + relativePath
  }

  /**
   * Content publish url
   * <p>
   * https://bintray.com/docs/api/#_publish_discard_uploaded_content
   */
  def urlContentPublish() = {
    // POST /content/:subject/:repo/:package/:version/publish
    val version = project.getVersion();
    restApiUrl + "/content/" + subject + "/" + repository + "/" + bintrayPackage + "/" + version + "/publish";
  }

  /**
   * Package descriptor url
   * <p>
   * https://bintray.com/docs/api/#_get_package
   */
  def urlPackageGet() = {
    // GET /packages/:subject/:repo/:package[?attribute_values=1]
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage;
  }

  /**
   * Package create url
   * <p>
   * https://bintray.com/docs/api/#_create_package
   */
  def urlPackageCreate() = {
    // POST /packages/:subject/:repo
    restApiUrl + "/packages/" + subject + "/" + repository;
  }

  /**
   * Package delete url
   * <p>
   * https://bintray.com/docs/api/#url_delete_package
   */
  def urlPackageDelete() = {
    // DELETE /packages/:subject/:repo/:package
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage;
  }

  /**
   * Version descriptor url
   * <p>
   * https://bintray.com/docs/api/#_get_version
   */
  def urlVersionGet( version : String ) = {
    // GET /packages/:subject/:repo/:package/versions/:version[?attribute_values=1]
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage + "/versions/" + version;
  }

  /**
   * Version delete url
   * <p>
   * https://bintray.com/docs/api/#url_delete_version
   */
  def urlVersionDelete( version : String ) = {
    // DELETE /packages/:subject/:repo/:package/versions/:version
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage + "/versions/" + version;
  }

  /**
   * Render rest api response message.
   */
  def render( title : String, response : Response ) = {
    title + " code=" + response.code() + " body=" + response.body().string();
  }

  /**
   * Verify if version description contains magic regex.
   */
  def hasPreserve( version : String ) : Boolean = {
    val versionJson = versionGet( version );
    val key = "desc";
    if ( versionJson.has( key ) ) {
      // Flatten content.
      val description = versionJson.get( key ).toString();
      if ( description.matches( preserveRegex ) ) {
        return true;
      }
    }
    return false;
  }

  /**
   * Remove previous versions of artifacts from repository.
   */
  def contentCleanup() = {
    getLog().info( "Cleaning package content: " + bintrayPackage );
    val packageJson = packageGet();
    val latest = packageJson.optString( "latest_version", "invalid_version" );
    val versionList = packageJson.getJSONArray( "versions" );
    versionList.forEach( ( entry ) => {
      try {
        val version = entry.toString();
        if ( !latest.equals( version ) ) {
          if ( hasPreserve( version ) ) {
            getLog().info( "Keeping version: " + version );
          } else {
            getLog().info( "Erasing version: " + version );
            versionDelete( version );
          }
        }
      } catch {
        case e : Throwable => throw new RuntimeException( e );
      }
    } );
  }

  /**
   * Mark deployed artifact as "published" for bintray consumption.
   */
  def contentPublish() = {
    getLog().info( "Publishing package content: " + bintrayPackage );
    val url = urlContentPublish();
    val json = "{}";
    val body = RequestBody.create( JSON, json );
    val request = new Request.Builder().url( url ).post( body ).build();
    val response = client.newCall( request ).execute();
    if ( response.code() != 200 ) {
      getLog().error( render( "Publish content error", response ) );
      throw new RuntimeException();
    }
  }

  /**
   * Provide required header meta data.
   */
  def injectHeader( builder : Request.Builder ) : Request.Builder = {
    builder
  }

  /**
   * Upload file content. Requires header meta data.
   */
  def contentUpload( local : File, remote : String ) = {
    val url = urlContentUpload( remote )
    val body = RequestBody.create( BINARY, local )
    val builder = new Request.Builder().url( url ).put( body );
    val request = injectHeader( builder ).build()
    val response = client.newCall( request ).execute();
    if ( response.code() != 201 ) {
      getLog().error( render( "Content upload error", response ) );
      throw new RuntimeException();
    }
  }

  /**
   * Verify bintray target package is present.
   */
  def hasPackage() : Boolean = {
    val url = urlPackageGet();
    val request = new Request.Builder().url( url ).build();
    val response = client.newCall( request ).execute();
    response.code() == 200;
  }

  /**
   * Fetch bintray target package description.
   */
  def packageGet() : JSONObject = {
    val url = urlPackageGet();
    val request = new Request.Builder().url( url ).build();
    val response = client.newCall( request ).execute();
    if ( response.code() != 200 ) {
      getLog().error( render( "Package fetch error", response ) );
      throw new RuntimeException();
    }
    val data = response.body().string();
    val json = new JSONObject( data );
    json;
  }

  /**
   * Create bintray target package entry with required meta data.
   */
  def packageCreate() = {
    val url = urlPackageCreate();
    val json = new JSONObject() //
      .put( "name", bintrayPackage ) //
      .put( "vcs_url", packageVcsUrl ) //
      .put( "licenses", packageLicenses ) //
      .toString();
    val body = RequestBody.create( JSON, json );
    val request = new Request.Builder().url( url ).post( body ).build();
    val response = client.newCall( request ).execute();
    if ( response.code() != 201 ) {
      getLog().error( render( "Package create error", response ) );
      throw new RuntimeException();
    }
  }

  /**
   * Delete bintray target package with all versions of artifacts.
   */
  def packageDelete() = {
    val url = urlPackageGet();
    val request = new Request.Builder().url( url ).build();
    val response = client.newCall( request ).execute();
    if ( response.code() != 200 ) {
      getLog().error( render( "Package delete error", response ) );
      throw new RuntimeException();
    }
  }

  /**
   * Delete package before deployment.
   */
  def destroyPackage() = {
    if ( hasPackage() ) {
      getLog().info( "Bintray package delete: ... " + bintrayPackage );
      packageDelete();
    } else {
      getLog().info( "Bintray package is missing: " + bintrayPackage );
    }
  }

  /**
   * Create target bintray package on demand.
   */
  def ensurePackage() = {
    if ( hasPackage() ) {
      getLog().info( "Bintray package is present: " + bintrayPackage );
    } else {
      getLog().info( "Bintray package create ...: " + bintrayPackage );
      packageCreate();
    }
  }

  /**
   * Fetch bintray package version description.
   */
  def versionGet( version : String ) : JSONObject = {
    val url = urlVersionGet( version );
    val request = new Request.Builder().url( url ).get().build();
    val response = client.newCall( request ).execute();
    if ( response.code() != 200 ) {
      getLog().error( render( "Version fetch error", response ) );
      throw new RuntimeException();
    }
    val data = response.body().string();
    val json = new JSONObject( data );
    json;
  }

  /**
   * Create bintray package version description with attached artifacts.
   */
  def versionCreate( version : String ) = { // FIXME
    val url = urlVersionDelete( version );
    val request = new Request.Builder().url( url ).delete().build();
    val response = client.newCall( request ).execute();
    if ( response.code() != 200 ) {
      getLog().error( render( "Version delete error", response ) );
      throw new RuntimeException();
    }
  }

  /**
   * Delete bintray package version description with attached artifacts.
   */
  def versionDelete( version : String ) = {
    val url = urlVersionDelete( version );
    val request = new Request.Builder().url( url ).delete().build();
    val response = client.newCall( request ).execute();
    if ( response.code() != 200 ) {
      getLog().error( render( "Version delete error", response ) );
      throw new RuntimeException();
    }
  }

}
