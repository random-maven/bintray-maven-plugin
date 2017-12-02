package com.carrotgarden.maven.bintray

import scala.meta._
import scala.meta.contrib._

import org.scalameta.logger

import java.nio.file.Files
import java.nio.file.Paths
import java.io.File

/*
 * structure
 * 
 * Source(List(Pkg(Term.Select(Term.Select(Term.Select(Term.Name("com"), Term.Name("carrotgarden")), Term.Name("maven")), Term.Name("bintray")), List(Import(List(Importer(Term.Select(Term.Name("java"), Term.Name("io")), List(Importee.Name(Name("IOException")))))), Import(List(Importer(Term.Select(Term.Select(Term.Name("java"), Term.Name("nio")), Term.Name("file")), List(Importee.Name(Name("Files")))))), Import(List(Importer(Term.Select(Term.Select(Term.Name("java"), Term.Name("nio")), Term.Name("file")), List(Importee.Name(Name("Path")))))), Import(List(Importer(Term.Select(Term.Select(Term.Name("java"), Term.Name("nio")), Term.Name("file")), List(Importee.Name(Name("Paths")))))), Import(List(Importer(Term.Select(Term.Name("java"), Term.Name("util")), List(Importee.Name(Name("ArrayList")))))), Import(List(Importer(Term.Select(Term.Name("java"), Term.Name("util")), List(Importee.Name(Name("List")))))), Import(List(Importer(Term.Select(Term.Name("java"), Term.Name("util")), List(Importee.Name(Name("Map")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("artifact")), List(Importee.Name(Name("Artifact")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("artifact")), Term.Name("repository")), List(Importee.Name(Name("ArtifactRepository")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("artifact")), Term.Name("repository")), List(Importee.Name(Name("Authentication")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("artifact")), Term.Name("repository")), Term.Name("layout")), List(Importee.Name(Name("ArtifactRepositoryLayout")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("execution")), List(Importee.Name(Name("MavenSession")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("plugin")), List(Importee.Name(Name("MojoExecutionException")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("plugin")), List(Importee.Name(Name("MojoFailureException")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("plugin")), Term.Name("deploy")), List(Importee.Name(Name("AbstractDeployMojo")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("plugins")), Term.Name("annotations")), List(Importee.Name(Name("Component")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("plugins")), Term.Name("annotations")), List(Importee.Name(Name("LifecyclePhase")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("plugins")), Term.Name("annotations")), List(Importee.Name(Name("Mojo")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("plugins")), Term.Name("annotations")), List(Importee.Name(Name("Parameter")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("project")), List(Importee.Name(Name("MavenProject")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("settings")), List(Importee.Name(Name("Server")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("settings")), List(Importee.Name(Name("Settings")))))), Import(List(Importer(Term.Select(Term.Name("org"), Term.Name("json")), List(Importee.Name(Name("JSONArray")))))), Import(List(Importer(Term.Select(Term.Name("org"), Term.Name("json")), List(Importee.Name(Name("JSONObject")))))), Import(List(Importer(Term.Name("okhttp3"), List(Importee.Name(Name("Authenticator")))))), Import(List(Importer(Term.Name("okhttp3"), List(Importee.Name(Name("Credentials")))))), Import(List(Importer(Term.Name("okhttp3"), List(Importee.Name(Name("MediaType")))))), Import(List(Importer(Term.Name("okhttp3"), List(Importee.Name(Name("OkHttpClient")))))), Import(List(Importer(Term.Name("okhttp3"), List(Importee.Name(Name("Request")))))), Import(List(Importer(Term.Name("okhttp3"), List(Importee.Name(Name("RequestBody")))))), Import(List(Importer(Term.Name("okhttp3"), List(Importee.Name(Name("Response")))))), Import(List(Importer(Term.Name("okhttp3"), List(Importee.Name(Name("Route")))))), Import(List(Importer(Term.Select(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("maven")), Term.Name("plugin")), List(Importee.Name(Name("AbstractMojo")))))), Defn.Class(List(Mod.Annot(Init(Type.Name("Mojo"), Name(""), List(List(Term.Assign(Term.Name("name"), Lit.String("upload")), Term.Assign(Term.Name("defaultPhase"), Term.Select(Term.Name("LifecyclePhase"), Term.Name("DEPLOY"))), Term.Assign(Term.Name("requiresProject"), Lit.Boolean(true))))))), Type.Name("UploadMojo"), Nil, Ctor.Primary(Nil, Name(""), Nil), Template(Nil, List(Init(Type.Name("AbstractMojo"), Name(""), Nil), Init(Type.Name("BintrayApi"), Name(""), Nil), Init(Type.Name("BaseParams"), Name(""), Nil)), Self(Name(""), None), List(Defn.Def(Nil, Term.Name("execute"), Nil, List(List()), Some(Type.Name("Unit")), Term.Block(Nil)))))))))
 */

object DescriptorDemo extends App {

  val file = new File( "./src/main/scala/com/carrotgarden/maven/bintray/UploadMojo.scala" )

//  val content = new String( Files.readAllBytes( file.toPath() ) );
//  //println (content)
//  val tokens = content.tokenize.get

  val code = file.parse[ Source ].get
  val structure = code.structure
  println( structure )

  val comments = AssociatedComments( code )
  println( comments )

  val klaz = code.find( _.is[ Defn.Class ] ).get
  println( klaz )

  val head = comments.leading(klaz)
  println( head ) // javadoc is here
  
}

