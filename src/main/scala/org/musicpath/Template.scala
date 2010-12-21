package org.musicpath

import org.scardf.{NodeConverter, Property, GraphNode, UriRef}
import NodeConverter.{asString, asGraphNode}
import scala.xml.{NodeSeq, Node, Elem, Text, NamespaceBinding, UnprefixedAttribute}
import java.net.URI

/*
  Welcome to Linked Data.
  You are at Node 0.
  Use "rel" attribute to traverse to a new subject (node).
  Use "property" to list a property of the current subject.
  Go!
 */
    
object Template {

val knowsTemp =
 <p xmlns:foaf="http://xmlns.com/foaf/0.1/">
     <span property="foaf:name"/>
     <a rel="foaf:knows" href=""><span property="foaf:name"/></a>
 </p>

    //def propertize(e:Elem)(subject:UriRef):Elem = propertize(e, None)(subject)
    private def propertize(e:Elem)(subject:GraphNode):NodeSeq = {
        val processedChildren = e.child flatMap processLinks(subject)
        e.attribute("property") match {
            case Some(prop) => propertyOf(subject, prop, e.scope).map((s) => e.copy(child=Text(s) +: processedChildren)).toSeq.flatten
            case None => e.copy(child=processedChildren)
        }
    }

    def propertyOf(subject:GraphNode, qname:NodeSeq, scope:NamespaceBinding):Iterable[String] = subject/UriRef(resolve(qname.text, scope))/asString.iterable

    private def realizeLink(e:Elem, attr:String)(subject:GraphNode):Elem = {
        val link = new UnprefixedAttribute(attr, subject.node.asInstanceOf[UriRef].uri, e.attributes.remove(attr))
        // Templating of properties is done a bit different on elements with a resource link, thus is is handled directly here.
        // If the property has a value,, put it in there as a text node.  If not, delete the property attribute.
        // More than one value is a warning (put that property on a child element!).
        val processedChildren = e.child flatMap processLinks(subject)
        val (contents, attributes) = e.attribute("property") match {
          case Some(prop) => propertyOf(subject, prop, e.scope) match {
            case List() => (processedChildren, link.remove("property"))
            case List(value) => (Text(value) +: processedChildren, link)
            case many => {
              println("More than one "+prop.text+" found for "+subject.node.toString+" on element "+e.label+", ignoring rest.")
              (Text(many.head) +: processedChildren, link)
            }
          }
          case None=> (processedChildren, link)
        }
        e.copy(attributes = attributes, child = contents )
    }

    private def resolve(qname:String, scope:NamespaceBinding):String = {
        val Array(prefix, local) = qname split ':'
        scope.getURI(prefix) + local
    }
/*
  def processLinks(subject:UriRef, node:Node):NodeSeq = node match {
    case e:Elem => {
      e.attribute("rel") match {
        case Some(rel) => {
          List("resource", "href", "src").dropWhile(!atts.contains(_)).headOption match {
            case Some(link) => subjecet/UriRef(rel) map realizeLink(e)
            case None => subjecet/UriRef(rel) map copyTillLink(subject, e)
          }
        }
      }
    }
  }

  }
  // TraverseTillAndThen(condition, action)(node)

    */

    private def copyTilLink(node:Node)(subject:GraphNode):Node = node match {
      case e:Elem => {
        getLink(e) match {
          case Some(ref) => realizeLink(e, ref)(subject)
          case None => e.copy(child=e.child.map(copyTilLink(_)(subject)))
        }
      }
      case other=> other
    }

    private def getLink(e:Elem):Option[String] ={
      val atts = e.attributes.map(_.key).toSet
      List("resource", "href", "src").dropWhile(!atts.contains(_)).headOption
    }
    // TODO: the old subject is valid until the children of the node with the link.
    // Make two behaviours for this: 
    // - One which additionally carries a new subject, doesn't look for rel attributes, and looks for a link attribute.
    // - The other, the default, is to just carry a subject, and be on the lookout for rel attributes.
    def processLinks(subject:GraphNode)(node:Node):NodeSeq = node match {
        case e:Elem => {
            e.attribute("rel") match {
                case Some(rel) => {
                    // Take the first link attribute name found:
                    val newSubjects = subject/UriRef(resolve(rel.text, e.scope))/asGraphNode.iterable 
                    getLink(e) match {
                        case Some(ref) => newSubjects.map(realizeLink(e, ref)).toSeq.flatten
                        case None => e.copy(child=newSubjects.map(copyTilLink(e.child.dropWhile(!_.isInstanceOf[Elem]).head)).toSeq.flatten) //propertize(e, Some(rel))(subject)
                    }
                }
                case None => propertize(e)(subject)
            }
        }
        case other => other
        //case t:Text => t
    }
}

// vim: set ts=4 sw=4 et: