package org.musicpath

import org.specs._
import org.scardf._

/**
 * Created by IntelliJ IDEA.
 * User: leif
 * Date: 12/30/10
 * Time: 11:04 PM
 */

class RDFaTemplateTest extends Specification {
  "RDFa Templating" should {

     val List(leif, john, bill) = List("http://leif.com", "http://john.com", "http://bill.com").map( UriRef(_) )
     val graph = Graph(
        leif -(FOAF.name->"Leif", FOAF.knows-> ObjSet(bill, john)),
        john-FOAF.name->"John",
        bill-FOAF.name->"Bill")

    "generate fleshed-out HTML from an RDF graph" in {
      val template =
        <p xmlns:foaf="http://xmlns.com/foaf/0.1/">
          <span property="foaf:name"/>
          <a href="" rel="foaf:knows"><span property="foaf:name"/></a>
        </p>

      Template.processLinks(graph/leif)(template) must equalIgnoreSpace(
      <p xmlns:foaf="http://xmlns.com/foaf/0.1/">
        <span property="foaf:name">Leif</span>
        <a href="http://john.com" rel="foaf:knows"><span property="foaf:name">John</span></a>
        <a href="http://bill.com" rel="foaf:knows"><span property="foaf:name">Bill</span></a>
      </p>)
    }

     "handle sprintf templating" in {
        val age = FOAF\"age"
        val ourGraph = Graph(leif-age->31)
        val template =
        <p xmlns:foaf="http://xmlns.com/foaf/0.1/" template-values="foaf:age">
           I can't believe I'm %d already.
        </p>

        Template.processLinks(ourGraph/leif)(template) must equalIgnoreSpace(
        <p xmlns:foaf="http://xmlns.com/foaf/0.1/">
           I can't believe I'm 31 already.
        </p>)

     }

  }

}