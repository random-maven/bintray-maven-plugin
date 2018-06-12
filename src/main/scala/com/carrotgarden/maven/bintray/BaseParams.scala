package com.carrotgarden.maven.bintray

import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings
import org.apache.maven.plugins.annotations._

import com.carrotgarden.maven.tools.Description

/**
 * Shared mojo execution configuration parameters.
 */
trait BaseParams {

  @Description( """
  Maven project providing deployment artifacts.
  """ )
  @Parameter( defaultValue = "${project}", required = true, readonly = true )
  var project : MavenProject = _

  @Description( """
  Maven session during execution.
  """ )
  @Parameter( defaultValue = "${session}", required = true, readonly = true )
  var session : MavenSession = _

  @Description( """
  Expose server credentials 
  from <a href="https://maven.apache.org/settings.html">settings.xml</a>.
  """ )
  @Parameter( defaultValue = "${settings}", required = true, readonly = true )
  var settings : Settings = _

  @Description( """
  Bintray rest-api-actor for authentication. 
  When missing, uses 
  <a href="#serverId"><b>serverId</b></a> / <code>username</code>
  from <a href="https://maven.apache.org/settings.html">settings.xml</a>.
<pre>
&lt;server&gt;
  &lt;id&gt;distro-bintray&lt;/id&gt;
  &lt;username&gt;...&lt;/username&gt;
  &lt;password&gt;...&lt;/password&gt;
&lt;/server&gt;
</pre> 
  """ )
  @Parameter( property = "bintray.username" )
  var username : String = _

  @Description( """
  Bintray rest-api-token for authentication. 
  When missing, uses 
  <a href="#serverId"><b>serverId</b></a> / <code>password</code>
  from <a href="https://maven.apache.org/settings.html">settings.xml</a>.
<pre>
&lt;server&gt;
  &lt;id&gt;distro-bintray&lt;/id&gt;
  &lt;username&gt;...&lt;/username&gt;
  &lt;password&gt;...&lt;/password&gt;
&lt;/server&gt;
</pre> 
  """ )
  @Parameter( property = "bintray.password" )
  var password : String = _

  @Description( """
  Server id for credentials lookup: <code>username</code>, <code>password</code>  
  from <a href="https://maven.apache.org/settings.html">settings.xml</a>.
  Used when not provided via parameters:
  <a href="#username"><b>username</b></a>,
  <a href="#password"><b>password</b></a>.
<pre>
&lt;server&gt;
  &lt;id&gt;distro-bintray&lt;/id&gt;
  &lt;username&gt;...&lt;/username&gt;
  &lt;password&gt;...&lt;/password&gt;
&lt;/server&gt;
</pre>
  Configure this <code>serverId</code> as <code>proxy/id</code> for optional
  <a href="https://maven.apache.org/guides/mini/guide-proxies.html">proxy setup</a>. 
  Note: bintray credentials and proxy credentials are unrelated.
<pre>
&lt;proxy&gt;
  &lt;id&gt;distro-bintray&lt;/id&gt;
  &lt;username&gt;...&lt;/username&gt;
  &lt;password&gt;...&lt;/password&gt;
&lt;/proxy&gt;
</pre> 
  """ )
  @Parameter( property     = "bintray.serverId", defaultValue = "distro-bintray" )
  var serverId : String = _

  @Description( """
  Bintray target repository package. Corresponds to <code>X-Bintray-Package</code> rest header.
  """ )
  @Parameter( property     = "bintray.bintrayPackage", defaultValue = "${project.artifactId}" )
  var bintrayPackage : String = _

  @Description( """
  Bintray target repository version. Corresponds to <code>X-Bintray-Version</code> rest header.
  """ )
  @Parameter( property     = "bintray.bintrayVersion", defaultValue = "${project.version}" )
  var bintrayVersion : String = _

  @Description( """
  Bintray package create definition parameter: 
  version control system url (rest parameter: <code>vcs_url</code>).
  """ )
  @Parameter( property     = "bintray.packageVcsUrl", defaultValue = "${project.url}" )
  var packageVcsUrl : String = _

  @Description( """
  Bintray package create definition parameter: 
  licenses list to attach to the target package (rest parameter: <code>licenses</code>).
  """ )
  @Parameter( property     = "bintray.packageLicenses", defaultValue = "Apache-2.0" )
  var packageLicenses : Array[ String ] = _

  @Description( """
  Bintray user or organization name which contains target
  <a href="#repository"><b>repository</b></a>.
  """ )
  @Parameter( property     = "bintray.subject", defaultValue = "${user.name}" )
  var subject : String = _

  @Description( """
  Bintray target repository name. 
  Repository must already exist for the bintray <a href="#subject"><b>subject</b></a>.
  """ )
  @Parameter( property     = "bintray.repository", defaultValue = "repo" )
  var repository : String = _

  @Description( """
  Flag to skip all steps of the deployment execution.
  """ )
  @Parameter( property     = "bintray.skip", defaultValue = "false" )
  var skip : Boolean = _

  @Description( """
  Execution step 1: optionally remove target bintray package 
  with all versions and files.
  """ )
  @Parameter( property     = "bintray.performDestroy", defaultValue = "false" )
  var performDestroy : Boolean = _

  @Description( """
  Execution step 2: optionally create target bintray package before deployment.
  Use create parameters: 
  <a href="#packageName"><b>packageName</b></a>,
  <a href="#packageVcsUrl"><b>packageVcsUrl</b></a>,
  <a href="#packageLicenses"><b>packageLicenses</b></a>.
  """ )
  @Parameter( property     = "bintray.performEnsure", defaultValue = "true" )
  var performEnsure : Boolean = _

}
