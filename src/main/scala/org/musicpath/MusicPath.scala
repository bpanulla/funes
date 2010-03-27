package org.musicpath

import scala.xml.{ProcInstr,NodeSeq,Text}
import java.net.URI
import com.thinkminimo.step._
import Scene._
import net.croz.scardf._

class MusicPath extends Step {
  
  implicit def convert(uri:String):URI = new URI(uri)
  val url = "http://musicpath.org/"
  protected def contextPath = request.getContextPath
  implicit val model = new Model
 
  override def init = this.model.read("file:///home/leif/band.ttl", "TURTLE")

  // Helper functions:

  // Put a <?xsl-stylesheet ?> processing-instruction at the top of the response.
  def template(content:NodeSeq):NodeSeq = ProcInstr("xml-stylesheet", "type='text/xsl' href='/stylesheets/root.xsl'")++Text("\n")++content
  // Select all things of a given RDF:type.
  def allOf(category:Res) = Sparql selectAllX asRes where( (X, RDF.Type, category) ) from model
  
  def bandView(band:Res) = 
        <band ref={band.uri.getPath}>
          <name>{band/Foaf.name}</name>
          <members>{
            for (stint <- band/staffed) yield
            <member>{stint/by/Foaf.givenname
            }</member>
          }</members>
        </band>

  def personView(person:Res) =
    <person ref={person.uri.getPath}>
      <name>{person/Foaf.givenname}</name>
      <plays>{
        for (stint <- person/performs) yield
        <stint><in ref={(stint/in/asRes).uri.getPath}>{stint/in/Foaf.name}</in><instrument>(stint/plays/asRes).uri</instrument></stint>
      }</plays>
    </person>

  before {
    contentType = "application/xml"
  }

  get("/path") {"The current path is: " ++ contextPath}
  // Display all the bands in the system.

  get("/bands") { template(
    <bands title="Bands">{
        allOf(Mo.MusicGroup) map bandView 
    }</bands>
  )}

  get("/bands/:band") { template(
    bandView(Res(url+"bands/"+params(":band")))
  )}

  // Display all the people in the system.
  get("/people") { template(
    <people title="People">{ allOf(Foaf.Person) map personView }</people>
  )}

  get("/people/:person") { template(
    personView(Res(url+"people/"+params(":person")))
  )}

  get("/") {
    template( 
    <span title="Home">
    <h1>Hello!</h1>
    Please make a selection: {request.getContextPath}
    <div><a href="/bands">bands</a></div>
    <div><a href="/people">people</a></div>
    </span>)
  }

}
