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

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import com.carrotgarden.maven.tools.Description
import java.io.IOException
import org.json.JSONArray

/**
 * Bintray REST API. Entry point to the bintray resource management.
 */
trait BintrayApi {

  self : BaseParams with AbstractMojo =>

  @Description( """
  Bintray REST API URL.
  """ )
  @Parameter( property     = "bintray.restApiUrl", defaultValue = "https://bintray.com/api/v1" )
  var restApiUrl : String = _

  @Description( """
  REST api connection timeout, seconds.
  """ )
  @Parameter( property     = "bintray.restConnectTimeout", defaultValue = "30" )
  var restConnectTimeout : Int = _

  @Description( """
  REST api read operation timeout, seconds.
  """ )
  @Parameter( property     = "bintray.restReadTimeout", defaultValue = "30" )
  var restReadTimeout : Int = _

  @Description( """
  REST api write operation timeout, seconds.
  """ )
  @Parameter( property     = "bintray.restWriteTimeout", defaultValue = "30" )
  var restWriteTimeout : Int = _

  @Description( """
  Bintray public download URL.
  """ )
  @Parameter( property     = "bintray.downloadUrl", defaultValue = "https://dl.bintray.com" )
  var downloadUrl : String = _

  /**
   * Bintray REST API data format.
   */
  lazy val TEXT = MediaType.parse( "text/plain" )
  lazy val JSON = MediaType.parse( "application/json; charset=utf-8" )
  lazy val BINARY = MediaType.parse( "application/octet-stream" )

  /**
   * Provide bintray REST API authentication from
   * {@code username}/{@code password} with fallback to the {@link #serverId}.
   */
  lazy val basicAuthenticator = new Authenticator() {
    override def authenticate( route : Route, response : Response ) = {
      var username = BintrayApi.this.username;
      var password = BintrayApi.this.password;
      if ( username == null || password == null ) {
        val server = settings.getServer( serverId )
        if ( server != null ) {
          username = server.getUsername()
          password = server.getPassword()
        }
      }
      val credentials = Credentials.basic( username, password )
      response.request().newBuilder().header( "Authorization", credentials ).build()
    }
  };

  /**
   * Provide bintray proxy authentication from {@link #serverId}.
   * https://maven.apache.org/guides/mini/guide-proxies.html
   */
  lazy val proxyOption = settings.getProxies.asScala.find( _.getId == serverId )

  /**
   * Provide bintray proxy authentication from {@link #serverId}.
   * https://maven.apache.org/guides/mini/guide-proxies.html
   */
  lazy val proxyAuthenticator = {
    proxyOption.map { proxy =>
      val credential = Credentials.basic( proxy.getUsername, proxy.getPassword )
      new Authenticator() {
        override def authenticate( route : Route, response : Response ) : Request = {
          response.request().newBuilder().header( "Proxy-Authorization", credential ).build()
        }
      }
    }.getOrElse( Authenticator.NONE )
  }

  /**
   * Provide bintray proxy authentication from {@link #serverId}.
   * https://maven.apache.org/guides/mini/guide-proxies.html
   */
  lazy val proxySelector : java.net.ProxySelector = {
    import java.net._
    import java.util._
    proxyOption.map { proxy =>
      val proxyType = proxy.getProtocol match {
        case "http"   => Proxy.Type.HTTP
        case "socks"  => Proxy.Type.SOCKS
        case "direct" => Proxy.Type.DIRECT
        case _        => throw new RuntimeException( s"Wrong proxy protocol: ${proxy.getProtocol}." )
      }
      val proxyAddress = new InetSocketAddress( proxy.getHost, proxy.getPort )
      val proxyInstance = new Proxy( proxyType, proxyAddress )
      val nonProxyHostList = proxy.getNonProxyHosts.split( "\\|" ).toList
      def hasNonProxyHost( host : String ) : Boolean = {
        nonProxyHostList.find( test => test.endsWith( host ) ).isDefined
      }
      new ProxySelector() {
        override def select( uri : URI ) : List[ Proxy ] = {
          val protocol = uri.getScheme
          val host = uri.getHost
          val port = uri.getPort
          if ( hasNonProxyHost( host ) ) {
            Collections.singletonList( Proxy.NO_PROXY )
          } else {
            Collections.singletonList( proxyInstance )
          }
        }
        override def connectFailed( uri : URI, sa : SocketAddress, ioe : IOException ) : Unit = {
          throw new RuntimeException( s"Proxy failure: ${uri} ${sa}", ioe )
        }
      }
    }.getOrElse( ProxySelector.getDefault )
  }

  /**
   * Bintray REST API client.
   */
  lazy val client = {
    new OkHttpClient.Builder()
      .authenticator( basicAuthenticator )
      .proxyAuthenticator( proxyAuthenticator )
      .proxySelector( proxySelector )
      .connectTimeout( restConnectTimeout, TimeUnit.SECONDS )
      .readTimeout( restReadTimeout, TimeUnit.SECONDS )
      .writeTimeout( restWriteTimeout, TimeUnit.SECONDS )
      .followRedirects( true )
      .followSslRedirects( true )
      .build()
  }

  /**
   * Maven upload url
   * https://bintray.com/docs/api/#_maven_upload
   */
  def urlMavenDeploy() = {
    // PUT /maven/:subject/:repo/:package/:file_path[;publish=0/1]
    restApiUrl + "/maven/" + subject + "/" + repository + "/" + bintrayPackage;
  }

  /**
   * Enumerate remote file content.
   */
  def urlContentDownload( relativePath : String ) = {
    // https://dl.bintray.com/random-maven/maven/repository
    downloadUrl + "/" + subject + "/" + repository + "/" + relativePath
  }

  /**
   * Delete remote file content.
   * For OSS, this action is limited for 180 days from the content’s publish date.
   */
  def urlContentDelete( relativePath : String ) = {
    // DELETE /content/:subject/:repo/:file_path
    restApiUrl + "/content/" + subject + "/" + repository + "/" + relativePath
  }

  /**
   * Upload local file content. Must provide meta data via headers.
   */
  def urlContentUpload( relativePath : String ) = {
    restApiUrl + "/content/" + subject + "/" + repository + "/" + relativePath
  }

  /**
   * Content publish url
   * https://bintray.com/docs/api/#_publish_discard_uploaded_content
   */
  def urlContentPublish() = {
    // POST /content/:subject/:repo/:package/:version/publish
    restApiUrl + "/content/" + subject + "/" + repository + "/" + bintrayPackage + "/" + bintrayVersion + "/publish";
  }

  /**
   * Package descriptor url
   * https://bintray.com/docs/api/#_get_package
   */
  def urlPackageGet() = {
    // GET /packages/:subject/:repo/:package[?attribute_values=1]
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage;
  }

  /**
   * Get all files in a given package.
   * https://bintray.com/docs/api/#_get_package_files
   */
  def urlPackageList() = {
    // GET /packages/:subject/:repo/:package/files[?include_unpublished=0/1]
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage + "/files";
  }

  /**
   * Package create url
   * https://bintray.com/docs/api/#_create_package
   */
  def urlPackageCreate() = {
    // POST /packages/:subject/:repo
    restApiUrl + "/packages/" + subject + "/" + repository;
  }

  /**
   * Package delete url
   * https://bintray.com/docs/api/#url_delete_package
   */
  def urlPackageDelete() = {
    // DELETE /packages/:subject/:repo/:package
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage;
  }

  /**
   * Version descriptor url
   * https://bintray.com/docs/api/#_get_version
   */
  def urlVersionGet( version : String ) = {
    // GET /packages/:subject/:repo/:package/versions/:version[?attribute_values=1]
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage + "/versions/" + version;
  }

  /**
   * Version delete url
   * https://bintray.com/docs/api/#url_delete_version
   */
  def urlVersionDelete( version : String ) = {
    // DELETE /packages/:subject/:repo/:package/versions/:version
    restApiUrl + "/packages/" + subject + "/" + repository + "/" + bintrayPackage + "/versions/" + version;
  }

  /**
   * Render REST API response message.
   */
  def render( title : String, response : Response ) = {
    title + " code=" + response.code() + " body=" + response.body().string()
  }

  def regexPreserve : String = "(PRESERVE)"

  /**
   * Verify if version description contains magic regex.
   */
  def hasPreserve( version : String ) : Boolean = {
    val versionJson = versionGet( version )
    val key = "desc";
    if ( versionJson.has( key ) ) {
      // Flatten content.
      val description = versionJson.get( key ).toString()
      if ( description.matches( regexPreserve ) ) {
        return true;
      }
    }
    return false;
  }

  /**
   * Remove previous versions of artifacts from repository.
   */
  def contentCleanup() = {
    getLog().info( "Cleaning package content: " + bintrayPackage )
    val packageJson = packageGet()
    val latest = packageJson.optString( "latest_version", "invalid_version" )
    val versionList = packageJson.getJSONArray( "versions" )
    versionList.forEach( ( entry ) => {
      try {
        val version = entry.toString()
        if ( !latest.equals( version ) ) {
          if ( hasPreserve( version ) ) {
            getLog().info( "Keeping version: " + version )
          } else {
            getLog().info( "Erasing version: " + version )
            versionDelete( version )
          }
        }
      } catch {
        case e : Throwable => throw new RuntimeException( e )
      }
    } )
  }

  /**
   * Mark deployed artifact as "published" for bintray consumption.
   */
  def contentPublish() = {
    getLog().info( "Publishing package content: " + bintrayPackage )
    val url = urlContentPublish()
    val json = "{}";
    val body = RequestBody.create( JSON, json )
    val request = new Request.Builder().url( url ).post( body ).build()
    withResource(client.newCall( request ).execute()) { response =>
      if (response.code() != 200) {
        getLog().error(render("Publish content error", response))
        throw new RuntimeException()
      }
    }
  }

  /**
   * Provide required header meta data.
   */
  def injectHeader( builder : Request.Builder ) : Request.Builder = {
    builder
  }

  /**
   * Delete remote content.
   */
  def contentCleanup( remote : String ) = {
    val url = urlContentDelete( remote )
    val builder = new Request.Builder().url( url ).delete()
    val request = injectHeader( builder ).build()
    withResource(client.newCall( request ).execute()) {response =>
      if ( response.code() != 200 ) {
        getLog().error( render( "Content delete error", response ) )
        throw new RuntimeException()
      }
    }
  }

  /**
   * Upload file content. Requires header meta data.
   */
  def contentUpload( local : File, remote : String ) = {
    val url = urlContentUpload( remote )
    val body = RequestBody.create( BINARY, local )
    val builder = new Request.Builder().url( url ).put( body )
    val request = injectHeader( builder ).build()
    withResource(client.newCall(request).execute()) { response =>
      if (response.code() != 201) {
        getLog().error(render("Content upload error", response))
        throw new RuntimeException()
      }
    }
  }

  /**
   * Verify bintray target package is present.
   */
  def hasPackage() : Boolean = {
    val url = urlPackageGet()
    val request = new Request.Builder().url( url ).get().build()
    withResource(client.newCall( request ).execute()) { response =>
      response.code() == 200;
    }
  }

  /**
   * Fetch bintray target package description.
   */
  def packageGet() : JSONObject = {
    val url = urlPackageGet()
    val request = new Request.Builder().url( url ).get().build()
    withResource(client.newCall( request ).execute()) { response =>
      if (response.code() != 200) {
        getLog().error(render("Package fetch error", response))
        throw new RuntimeException()
      }
      val data = response.body().string()
      val json = new JSONObject(data)
      json
    }
  }

  /**
   * Fetch bintray target package file list descriptors.
   *
   * Status: 200 OK
   * [
   * {
   * "name": "nutcracker-1.1-sources.jar",
   * "path": "org/jfrog/powerutils/nutcracker/1.1/nutcracker-1.1-sources.jar",
   * "package": "jfrog-power-utils",
   * "version": "1.1",
   * "repo": "jfrog-jars",
   * "owner": "jfrog",
   * "created": "ISO8601 (yyyy-MM-dd'T'HH:mm:ss.SSSZ)",
   * "size": 1234,
   * "sha1": "602e20176706d3cc7535f01ffdbe91b270ae5012"
   * }
   * ]
   */
  def packageList() : JSONArray = {
    val url = urlPackageList()
    val request = new Request.Builder().url( url ).get().build()
    withResource(client.newCall( request ).execute()) { response =>
      if ( response.code() != 200 ) {
        getLog().error( render( "Package list error", response ) )
        throw new RuntimeException()
      }
      val data = response.body().string()
      val json = new JSONArray( data )
      json
    }
  }

  /**
   * Fetch bintray target package file list descriptors.
   */
  def packageListPath() : Seq[ String ] = {
    packageList().iterator.asScala.toSeq.map { entry =>
      val jsonFile = entry.asInstanceOf[ JSONObject ]
      val path = jsonFile.getString( "path" )
      path
    }
  }

  /**
   * Create bintray target package entry with required meta data.
   */
  def packageCreate() = {
    val url = urlPackageCreate()
    val json = new JSONObject() //
      .put( "name", bintrayPackage ) //
      .put( "vcs_url", packageVcsUrl ) //
      .put( "licenses", packageLicenses ) //
      .toString()
    val body = RequestBody.create( JSON, json )
    val request = new Request.Builder().url( url ).post( body ).build()
    withResource(client.newCall( request ).execute()) { response =>
      if (response.code() != 201) {
        getLog().error(render("Package create error", response))
        throw new RuntimeException()
      }
    }
  }

  /**
   * Delete bintray target package with all versions of artifacts.
   */
  def packageDelete() = {
    val url = urlPackageGet()
    val request = new Request.Builder().url( url ).build()
    withResource(client.newCall( request ).execute()) { response =>
      if (response.code() != 200) {
        getLog().error(render("Package delete error", response))
        throw new RuntimeException()
      }
    }
  }

  /**
   * Delete package before deployment.
   */
  def destroyPackage() = {
    if ( hasPackage() ) {
      getLog().info( "Bintray package delete: ... " + bintrayPackage )
      packageDelete()
    } else {
      getLog().info( "Bintray package is missing: " + bintrayPackage )
    }
  }

  /**
   * Create target bintray package on demand.
   */
  def ensurePackage() = {
    if ( hasPackage() ) {
      getLog().info( "Bintray package is present: " + bintrayPackage )
    } else {
      getLog().info( "Bintray package create ...: " + bintrayPackage )
      packageCreate()
    }
  }

  /**
   * Fetch bintray package version description.
   */
  def versionGet( version : String ) : JSONObject = {
    val url = urlVersionGet( version )
    val request = new Request.Builder().url( url ).get().build()
    withResource(client.newCall( request ).execute()) { response =>
      if (response.code() != 200) {
        getLog().error(render("Version fetch error", response))
        throw new RuntimeException()
      }
      val data = response.body().string()
      val json = new JSONObject(data)
      json
    }
  }

  /**
   * Create bintray package version description with attached artifacts.
   */
  def versionCreate( version : String ) = { // FIXME
    val url = urlVersionDelete( version )
    val request = new Request.Builder().url( url ).delete().build()
    withResource(client.newCall( request ).execute()) { response =>
      if (response.code() != 200) {
        getLog().error(render("Version create error", response))
        throw new RuntimeException()
      }
    }
  }

  /**
   * Delete bintray package version description with attached artifacts.
   */
  def versionDelete( version : String ) = {
    val url = urlVersionDelete( version )
    val request = new Request.Builder().url( url ).delete().build()
    withResource(client.newCall( request ).execute()) { response =>
      if (response.code() != 200) {
        getLog().error(render("Version delete error", response))
        throw new RuntimeException()
      }
    }
  }

  private def withResource[T <: AutoCloseable, V](r: => T)(f: T => V): V = {
    val resource: T = r
    require(resource != null, "resource is null")
    var exception: Throwable = null
    try {
      f(resource)
    } catch {
      case NonFatal(e) =>
        exception = e
        throw e
    } finally {
      closeAndAddSuppressed(exception, resource)
    }
  }

  private def closeAndAddSuppressed(e: Throwable, resource: AutoCloseable): Unit = {
    if (e != null) {
      try {
        resource.close()
      } catch {
        case NonFatal(suppressed) =>
          e.addSuppressed(suppressed)
      }
    } else {
      resource.close()
    }
  }

}
