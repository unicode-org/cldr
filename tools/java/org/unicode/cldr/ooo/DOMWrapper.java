/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import java.util.*;

/**
 *
 * class has generic methods for performing common DOM parsing tasks
 * - OpenOffice.org does not have distinguishing attributes for repeated eleemnts,
 *   the elements are identified by their order.
 * - Using DOMWrapper instead of XPath as want to do 2 way transparent passthrough 
 *   of OpenOffice.org specific data.
 *   
 *
 */

public class DOMWrapper
{
    private Document m_Doc = null;
    
    public DOMWrapper(Document doc)
    {
        resetDoc(doc);
    }
    
    //reset doc to retrieve referenced data
    public void resetDoc(Document doc)
    {
        m_Doc = doc;
    }
    
    
  /* get data which is identified by it's element
   * implies this element tag name is unique in the doc
   */
    public String getTextFromElement(String element)
    {
        String text = null;
        if (element == null)
            return text;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        Node node = nl.item(0);
        if ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE))
        {
            NodeList nlChildren = node.getChildNodes();
            Node textNode = nlChildren.item(0);
            if ((textNode != null) && (textNode.getNodeType() == Node.TEXT_NODE))
            {
                text = textNode.getNodeValue();
            }
        }
        return text;
    }
    
      /* get data which is identified by it's element and parent element
       * mplies this element tag name is unique under this parentElement
       */
    public String getTextFromElement(String parentElement, String element)
    {
        String text = null;
        if ((parentElement == null) || (element == null))
            return text;
   
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if ((textNode != null) && (textNode.getNodeType() == Node.TEXT_NODE))
                {
                    text = textNode.getNodeValue();
                    break;
                }
            }
        }
        return text;
    }
    
    /* get data from all elements where there is a match on element and parent element
     *  returns Vector of Strings
     */
    public Vector getTextFromAllElements(String parentElement, String element)
    {
        Vector data = new Vector();
        if ((parentElement == null) || (element == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if ((textNode != null) && (textNode.getNodeType() == Node.TEXT_NODE))
                {
                    String text = textNode.getNodeValue();
                    if (text != null) data.add(text);
                }
            }
        }
        return data;
    }
    
    
   /* get all attributes for the element identified by element and its parent element
    *  returns a Vector of Hashtables (key=attr name, value=attr value)
    */
    public Vector getAttributesFromElement(String parentElement, String element)
    {
        Vector allElementAttributes = new Vector();
        
        if ((parentElement == null) || (element == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable attributes = new Hashtable();
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                NamedNodeMap attrMap = node.getAttributes();
                for (int j=0; j < attrMap.getLength(); j++)
                {
                    Node attrNode = attrMap.item(j);
                    String key = attrNode.getNodeName();
                    String value = attrNode.getNodeValue();
                    attributes.put(key, value);
                }
            }
            allElementAttributes.add(attributes);
        }
        return allElementAttributes;
    }
    
   /* get all attributes for the element identified by element and parent element
    *  returns a Hashtable of Hashtables (outer key = passed in attribute's value,  inner key=attr name, value=attr value)
    */
    public Hashtable getAttributesFromElement(String parentElement, String element, String attribute)
    {
        Hashtable allElementAttributes = new Hashtable();
        
        if ((parentElement == null) || (element == null) || (attribute == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable attributes = new Hashtable();
            String attributeValue = null;
            
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                NamedNodeMap attrMap = node.getAttributes();
                for (int j=0; j < attrMap.getLength(); j++)
                {
                    Node attrNode = attrMap.item(j);
                    String key = attrNode.getNodeName();
                    String value = attrNode.getNodeValue();
                    if (key.compareTo(attribute) ==0)
                        attributeValue = value;
                    attributes.put(key, value);
                }
            }
            if (attributeValue != null)
            {
                if (allElementAttributes.containsKey(attributeValue)==false)
                    allElementAttributes.put(attributeValue, attributes);
                else
                    System.err.println ("WARNING 1: duplicate " + attributeValue + " attribute under " + parentElement);
            }
        }
        return allElementAttributes;
    }
  
 
    //get the values of attribute1 and attribute2
    //return Hashtable, key = attribute1, value = attribute2
    public Hashtable getAndMapAttributes(String element, String attribute1, String attribute2)
    {
        Hashtable data = new Hashtable();
        
        if ((element == null) || (attribute1 == null) || (attribute2 == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable attributes = new Hashtable();
            String attributeValue = null;
            
            Node node = nl.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                NamedNodeMap attrMap = node.getAttributes();
                String key=null, value=null;
                for (int j=0; j < attrMap.getLength(); j++)
                {
                    Node attrNode = attrMap.item(j);
                    if (attrNode.getNodeName().equals(attribute1))
                        key = attrNode.getNodeValue();

                    if (attrNode.getNodeName().equals(attribute2))
                        value = attrNode.getNodeValue();
                }
                if (key!=null && value !=null) 
                {
            //        System.err.println (key + "   " + value);
                    //deal with identical keys by concatenating 
                    String val = (String) data.get (key);
                    if (val != null)
                        value = val + " " + value;
                    data.put(key, value);   
                }
            }
        }
        return data;
    }
    
 /* get the value of attribName belonging to element provided element's parent matches parentElement
  * and provided parentElement has an attribute = attribName whose value = parentAttribValue
  */
    public String getAttributesFromElement(String parentElement, String element, String attribName, String parentAttribName, String parentAttribValue)
    {
        if ((parentElement == null) || (element == null) || (attribName == null)
        || (parentAttribName == null) || (parentAttribValue == null))
            return null;
        
        String attributeValue = null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                boolean bParentMatch = false;
                NamedNodeMap attrMap = nodeParent.getAttributes();
                for (int j=0; j < attrMap.getLength(); j++)
                {
                    Node attrNode = attrMap.item(j);
                    String key = attrNode.getNodeName();
                    String value = attrNode.getNodeValue();
                    if ((key.compareTo(parentAttribName) ==0) && (value.compareTo(parentAttribValue) ==0))
                    {
                        bParentMatch = true;
                        break;
                    }
                }
                
                if (bParentMatch == true)
                {
                    attrMap = node.getAttributes();
                    for (int j=0; j < attrMap.getLength(); j++)
                    {
                        Node attrNode = attrMap.item(j);
                        String key = attrNode.getNodeName();
                        String value = attrNode.getNodeValue();
                        if (key.compareTo(attribName) ==0)
                            attributeValue = value;
                    }
                }
            }
        }
        return attributeValue;
    }
    
 /* get the value of attribName belonging to element provided element's parent matches parentElement
  * and provided parentElement has an attribute = attribName whose value = parentAttribValue
  */
    public Vector getAttributesFromElement2(String parentElement, String element, String attribName, String parentAttribName, String parentAttribValue)
    {
        if ((parentElement == null) || (element == null) || (attribName == null)
        || (parentAttribName == null) || (parentAttribValue == null))
            return null;
        
        Vector attributes = new Vector ();
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                boolean bParentMatch = false;
                NamedNodeMap attrMap = nodeParent.getAttributes();
                for (int j=0; j < attrMap.getLength(); j++)
                {
                    Node attrNode = attrMap.item(j);
                    String key = attrNode.getNodeName();
                    String value = attrNode.getNodeValue();
                    if ((key.compareTo(parentAttribName) ==0) && (value.compareTo(parentAttribValue) ==0))
                    {
                        bParentMatch = true;
                        break;
                    }
                }
                
                if (bParentMatch == true)
                {
                    attrMap = node.getAttributes();
                    for (int j=0; j < attrMap.getLength(); j++)
                    {
                        Node attrNode = attrMap.item(j);
                        String key = attrNode.getNodeName();
                        String value = attrNode.getNodeValue();
                        if (key.compareTo(attribName) ==0)
                            attributes.add (value);
                    }
                }
            }
        }
        return attributes;
    }
    
    /* get all attributes for the element identified by element
     *  returns a Vector of Hashtables (key=attr name, value=attr value)
     */
    public Vector getAttributesFromElement(String element)
    {
        if (element == null)
            return null;
        
        Vector allElementAttributes = new Vector();
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable attributes = new Hashtable();
            Node node = nl.item(i);
            
            NamedNodeMap attrMap = node.getAttributes();
            for (int j=0; j < attrMap.getLength(); j++)
            {
                Node attrNode = attrMap.item(j);
                String key = attrNode.getNodeName();
                String value = attrNode.getNodeValue();
                attributes.put(key, value);
            }
            allElementAttributes.add(attributes);
        }
        return allElementAttributes;
    }
    
    // gets the value of attrName from first element called "element"
    public String getAttributeFromElement(String element, String attrName)
    {
        if (element == null)
            return null;
        
        String attrValue = null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Node node = nl.item(i);
            
            NamedNodeMap attrMap = node.getAttributes();
            for (int j=0; j < attrMap.getLength(); j++)
            {
                Node attrNode = attrMap.item(j);
                if (attrNode.getNodeName().compareTo (attrName)==0)
                {
                    attrValue = attrNode.getNodeValue();
                    break;
                }
            }
        }
        return attrValue;
    }
        
        /* get values of attribName for all elements identified by "element"
         * returns vector of all these attrib values
         */
    public Vector getAttrsFromElement(String element, String attribName)
    {
        if (element == null)
            return null;
        
        Vector allElementAttributes = new Vector();
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Node node = nl.item(i);
            NamedNodeMap attrMap = node.getAttributes();
            for (int j=0; j < attrMap.getLength(); j++)
            {
                Node attrNode = attrMap.item(j);
                if (attrNode.getNodeName().compareTo(attribName)==0)
                {
                    allElementAttributes.add(attrNode.getNodeValue());
                    break;
                }
            }
        }
        return allElementAttributes;
    }
    
          /* get all attributes for the element identified by element and parent element
           *
           */
    public Hashtable getAttributesFromNode(Node node)
    {
        Hashtable attributes = new Hashtable();
        
        if (node == null)
            return null;
        
        if (node.getNodeType() == Node.ELEMENT_NODE)
        {
            NamedNodeMap attrMap = node.getAttributes();
            for (int j=0; j < attrMap.getLength(); j++)
            {
                Node attrNode = attrMap.item(j);
                String key = attrNode.getNodeName();
                String value = attrNode.getNodeValue();
                attributes.put(key, value);
            }
        }
        return attributes;
    }
    
    /* retrieves an element, it's sibling and the valus of their greatGrandParent's attribute
     * and returns a Hashtable of Hashtables where
     * outer key = greatGrandParent's attribute's value, inner key = element, inner value = elementSibling
     */
    public Hashtable getTextFromElementsAndGGPAttrib(String parentElement, String element, String elementSibling, String greatGrandParentAttrib)
    {
        Hashtable inner = new Hashtable();
        Hashtable outer = new Hashtable();
        if ((parentElement == null) || (element == null) || (elementSibling == null))
            return null;
        
        String prevGreatGParentAttribValue = "unknown";
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String elementText = null;
            String siblingElementText = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                //get the elements value
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if (textNode.getNodeType() == Node.TEXT_NODE)
                {
                    elementText = textNode.getNodeValue();
                }
                
                //get the sibling's value
                Node siblingNode = node.getNextSibling();
                while (siblingNode != null)
                {
                    if (siblingNode.getNodeName().compareTo(elementSibling)==0)
                    {
                        //get the elements value
                        NodeList nlSiblingChildren = siblingNode.getChildNodes();
                        Node siblingTextNode = nlSiblingChildren.item(0);
                        if ((siblingTextNode != null) && (siblingTextNode.getNodeType() == Node.TEXT_NODE))
                        {
                            siblingElementText = siblingTextNode.getNodeValue();
                            break;
                        }
                    }
                    siblingNode = siblingNode.getNextSibling();
                }
                
                //get the greatGParent's attrib
                Node greatGParentNode = nodeParent.getParentNode().getParentNode();
                if (greatGParentNode != null)
                {
                    Hashtable greatGParentAttribs = getAttributesFromNode(greatGParentNode);
                    String greatGParentAttribValue = (String) greatGParentAttribs.get((Object)greatGrandParentAttrib);
                    
                    if (greatGParentAttribValue == null)
                        continue;
                    
                    if (prevGreatGParentAttribValue.compareTo("unknown")==0)
                        prevGreatGParentAttribValue = greatGParentAttribValue;  //first time
                    
                    if (prevGreatGParentAttribValue.compareTo(greatGParentAttribValue)==0)
                    { //they share the same GGParent attribute so put in same inner nexted map
                        if ((elementText != null) && (siblingElementText != null))
                        {
                            if (inner.containsKey(elementText)==false)
                               inner.put(elementText, siblingElementText);                            
                            else
                                System.err.println ("WARNING 2: duplicate " + elementText + " element value under " + parentElement);
                        }
                     
                    }
                    else
                    {  //belong to a different category so write the previous inner nexted map to outer map and start a new inner map
                        Hashtable innerCopy = new Hashtable(inner);
                        if (outer.containsKey(prevGreatGParentAttribValue)==false)
                            outer.put(prevGreatGParentAttribValue, innerCopy);
                        else
                            System.err.println ("WARNING 3: duplicate " + prevGreatGParentAttribValue + " value for " + greatGrandParentAttrib);
                             
                        inner.clear();
                        if ((elementText != null) && (siblingElementText != null))
                        {
                            if (inner.containsKey(elementText)==false)
                                inner.put(elementText, siblingElementText);                            
                            else
                                System.err.println ("WARNING 4: duplicate " + elementText + " element value under " + parentElement);
                        }
                    }
                    prevGreatGParentAttribValue = greatGParentAttribValue; //store for next loop
                }
            }
            }  //end for loop
        
        //take case of last category which will not have been written inside loop
        if (prevGreatGParentAttribValue.compareTo("unknown")!=0)
        {
            if (outer.containsKey(prevGreatGParentAttribValue)==false)
                outer.put(prevGreatGParentAttribValue, inner);
            else
                System.err.println ("WARNING 5: duplicate " + prevGreatGParentAttribValue + " value for " + greatGrandParentAttrib);
        }
        return outer;
    }
    
    
    /* retrieves value of an element (where element is identified by it's own name and that of it's parent)
     *  and the valus of one of the grandParent's attribute
     * and returns a Hashtable where key = grandParent's attribute,  value = element's value
     */
    public Hashtable getTextFromElementsAndGPAttrib(String parentElement, String element, String grandParentAttrib)
    {
        if ((parentElement == null) || (element == null) || (grandParentAttrib == null))
            return null;
        
        Hashtable outer = new Hashtable();
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String elementText = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                //get the element's value
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if (textNode.getNodeType() == Node.TEXT_NODE)
                {
                    elementText = textNode.getNodeValue();
                }
                
                //get the grandParent's attrib
                Node grandParentNode = nodeParent.getParentNode();
                if (grandParentNode != null)
                {
                    Hashtable grandParentAttribs = getAttributesFromNode(grandParentNode);
                    String grandParentAttribValue = (String) grandParentAttribs.get((Object)grandParentAttrib);
                    
                    if (grandParentAttribValue != null)
                    {
                        if (outer.containsKey(grandParentAttribValue)==false)
                            outer.put(grandParentAttribValue, elementText);
                        else
                            System.err.println ("WARNING 6: duplicate " + grandParentAttribValue + " value for " + grandParentAttrib);           
                    }
                }
            }
        }  //end for loop
        return outer;
    }
    
    
        /* retrieves text value of an element (where element is identified by its own name, that of its parent and that of its grandparent)
         *  and the values of one of the grandParent's attribute
         * and returns a Hashtable where key = grandParent's attribute,  value = element's value
         */
    public Hashtable getTextFromElementsAndGPAttrib(String element, String parentElement, String gparentElement, String grandParentAttrib)
    {
        if ((element == null) || (parentElement == null) || (gparentElement == null) || (grandParentAttrib == null))
            return null;
        
        Hashtable outer = new Hashtable();
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String elementText = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent != null)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                Node nodeGParent = nodeParent.getParentNode();
                if ((nodeParent.getNodeType() == Node.ELEMENT_NODE)
                && (nodeGParent != null)
                && (nodeGParent.getNodeName().compareTo(gparentElement) ==0))
                {
                    //get the element's value
                    NodeList nlChildren = node.getChildNodes();
                    Node textNode = nlChildren.item(0);
                    if (textNode.getNodeType() == Node.TEXT_NODE)
                        elementText = textNode.getNodeValue();
                    
                    //get the grandParent's attrib
                    Hashtable grandParentAttribs = getAttributesFromNode(nodeGParent);
                    String grandParentAttribValue = (String) grandParentAttribs.get((Object)grandParentAttrib);
                    
                    if ((grandParentAttribValue != null) && (elementText != null))
                    {
                        if (outer.containsKey(grandParentAttribValue)==false)
                            outer.put(grandParentAttribValue, elementText);
                        else
                            System.err.println ("WARNING 7: duplicate " + grandParentAttribValue + " value for " + grandParentAttrib);           
                    }
                }
            }
        }  //end for loop
        return outer;
    }
    
            /* retrieves text value of an element (where element is identified by its own name, that of its parent and that of its grandparent)
         *  and the values of one of the grandParent's attribute
         * and returns a Vector of attibValues from all elements matching "element"
         */
    public Vector getTextFromElementsAndGPAttrib(String element, String elAttrib, String gparentElement, String grandParentAttrib, String gParentAttribValue)
    {
        if ((element == null) || (elAttrib == null) || (gparentElement == null) || (grandParentAttrib == null) || (gParentAttribValue == null))
            return null;
        
        Vector attrs = new Vector ();
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String attrText = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE) && (nodeParent != null))
            {
                Node nodeGParent = nodeParent.getParentNode();
                if ((nodeParent.getNodeType() == Node.ELEMENT_NODE)
                && (nodeGParent != null)
                && (nodeGParent.getNodeName().compareTo(gparentElement) ==0))
                {
                    //get the element's value
                    NamedNodeMap nlAttrs = node.getAttributes();
                    Node attrNode = nlAttrs.getNamedItem(elAttrib);
                    if ((attrNode.getNodeType() == Node.ATTRIBUTE_NODE)
                    && (attrNode.getNodeName().compareTo (elAttrib)==0))
                        attrText = attrNode.getNodeValue();
                    
                    //get the grandParent's attrib
                    Hashtable grandParentAttribs = getAttributesFromNode(nodeGParent);
                    String grandParentAttribValue = (String) grandParentAttribs.get((Object)grandParentAttrib);
                    
                    if (grandParentAttribValue.compareTo (gParentAttribValue)==0)
                       attrs.add(attrText);
                }
            }
        }  //end for loop
        return attrs;
    }
    
    /* retrieves value of an element (where element is identified by it's own name and that of it's parent)
     *  and the valus of one of the Parent's attribute
     * and returns a Hashtable where key = parent's attribute,  value = element's value
     */
    public Hashtable getTextFromElementsAndParentAttrib(String parentElement, String element, String parentAttrib)
    {
        if ((parentElement == null) || (element == null) || (parentAttrib == null))
            return null;
        
        Hashtable outer = new Hashtable();
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String elementText = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                //get the element's value
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if ((textNode != null)&& (textNode.getNodeType() == Node.TEXT_NODE))
                {
                    elementText = textNode.getNodeValue();
                }
                
                if (nodeParent != null)
                {
                    Hashtable parentAttribs = getAttributesFromNode(nodeParent);
                    String parentAttribValue = (String) parentAttribs.get((Object)parentAttrib);
                    
                    if ((parentAttribValue != null) && (elementText != null))
                    {
                        if (outer.containsKey(parentAttribValue)==false)
                            outer.put(parentAttribValue, elementText);
                        else
                            System.err.println ("WARNING 8: duplicate " + parentElement + " with " + parentAttrib + " = " + parentAttribValue);
                    }
                }
            }
        }  //end for loop
        return outer;
    }
    
        /* retrieves value of an element (where element is identified by it's own name and that of it's parent)
         *  and the valus of one of the element's attributes
         * and returns a Hashtable where key = element's attribute's calue,  value = element's value
         */
    public Hashtable getTextFromElementsAndElementAttrib(String parentElement, String element, String elementAttrib /*attrib name*/)
    {
        if ((parentElement == null) || (element == null) || (elementAttrib == null))
            return null;
        
        Hashtable outer = new Hashtable();
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String elementText = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                //get the element's value
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if ((textNode != null)&& (textNode.getNodeType() == Node.TEXT_NODE))
                {
                    elementText = textNode.getNodeValue();
                }
                
                Hashtable attribs = getAttributesFromNode(node);
                String attribValue = (String) attribs.get((Object)elementAttrib);
                if ((attribValue != null) && (elementText != null))
                {
                    if (outer.containsKey(attribValue)==false)
                        outer.put(attribValue, elementText);
                    else
                        System.err.println ("WARNING 9: duplicate " + attribValue + " for " + elementAttrib);                
                }
            }
        }  //end for loop
        return outer;
    }
    
        /* retrieves an element, it's sibling
         * and returns a Hashtable of  where key = element value, inner value = elementSibling's value
         */
    public Hashtable getTextFromElement(String parentElement, String element, String elementSibling)
    {
        Hashtable table = new Hashtable();
        if ((parentElement == null) || (element == null) || (elementSibling == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String elementText = null;
            String siblingElementText = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                //get the elements value
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if (textNode.getNodeType() == Node.TEXT_NODE)
                {
                    elementText = textNode.getNodeValue();
                }
                
                //get the sibling's value,
                Node siblingNode = node.getNextSibling();
                while (siblingNode != null)
                {
                    if (siblingNode.getNodeName().compareTo(elementSibling)==0)
                    {
                        //get the elements value
                        NodeList nlSiblingChildren = siblingNode.getChildNodes();
                        Node siblingTextNode = nlSiblingChildren.item(0);
                        if ((siblingTextNode != null) && (siblingTextNode.getNodeType() == Node.TEXT_NODE))
                        {
                            siblingElementText = siblingTextNode.getNodeValue();
                            break;
                        }
                    }
                    siblingNode = siblingNode.getNextSibling();
                }
                
                //sibling can come before or after element so look before it, if it wasn't found after
                if (siblingElementText == null)
                {
                    siblingNode = node.getPreviousSibling();
                    while (siblingNode != null)
                    {
                        if (siblingNode.getNodeName().compareTo(elementSibling)==0)
                        {
                            //get the elements value
                            NodeList nlSiblingChildren = siblingNode.getChildNodes();
                            Node siblingTextNode = nlSiblingChildren.item(0);
                            if ((siblingTextNode != null) && (siblingTextNode.getNodeType() == Node.TEXT_NODE))
                            {
                                siblingElementText = siblingTextNode.getNodeValue();
                                break;
                            }
                        }
                        siblingNode = siblingNode.getPreviousSibling();
                    }
                }
                
                if ((elementText != null) && (siblingElementText != null))
                {
                    if (table.containsKey(elementText)==false)
                        table.put(elementText, siblingElementText);
                    else
                        System.err.println ("WARNING 10: duplicate " + elementText + " under " + parentElement);
                }
            }
        }  //end for loop
        
        return table;
    }
    
    // gets the value of the identified attribute from the first element called "element"
    //returns null if attribute or element not found
    public String getAttributeValue(String element, String attribute)
    {
        String attributeValue = null;
        if ((element == null) || (attribute == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        if (nl.getLength() >0)
        {
            Node node = nl.item(0);
            NamedNodeMap attrMap = node.getAttributes();
            for (int j=0; j < attrMap.getLength(); j++)
            {
                Node attrNode = attrMap.item(j);
                String key = attrNode.getNodeName();
                
                if (key.compareTo(attribute)==0)
                {
                    attributeValue = attrNode.getNodeValue();
                    break;
                }
            }
        }
        return attributeValue;
    }
    
    /* method looks for "attribute" of all "element"s, if found looks for "parentAttribute" of 'parentElement", if found
     * puts in Hashtable (key = "parentAttribute"'s value, data = "attribute"'s value
     */
    public Hashtable getAttributeValues(String element, String attribute, String parentElement, String parentAttribute)
    {
        if ((element==null) || (attribute==null) || (parentElement==null) || (parentAttribute==null))
            return null;
        
        Hashtable table = new Hashtable();
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String attributeValue = null;
            Node node = nl.item(i);
            NamedNodeMap attrMap = node.getAttributes();
            for (int j=0; j < attrMap.getLength(); j++)
            {
                Node attrNode = attrMap.item(j);
                String key = attrNode.getNodeName();
                
                if (key.compareTo(attribute)==0)
                {
                    attributeValue = attrNode.getNodeValue();
                    break;
                }
            }
            
            //if we find the element's attribute we'return interested in then get the parentElement's attribute of interest
            if (attributeValue != null)
            {
                String parentAttributeValue = null;
                Node nodeParent = node.getParentNode();
                NamedNodeMap parentAttrMap = nodeParent.getAttributes();
                for (int j=0; j < parentAttrMap.getLength(); j++)
                {
                    Node attrNode = parentAttrMap.item(j);
                    String key = attrNode.getNodeName();
                    
                    if (key.compareTo(parentAttribute)==0)
                    {
                        parentAttributeValue = attrNode.getNodeValue();
                        break;
                    }
                }
                if (parentAttributeValue != null)
                {
                    if (table.containsKey(parentAttributeValue)==false)
                        table.put(parentAttributeValue, attributeValue);
                    else
                         System.err.println ("WARNING 11: duplicate " + parentAttributeValue + " for " + parentAttribute);                                       
                }
            }
        }
        return table;
    }
    
    
  /* retrieves value of an element (where element is identified by it's own name and that of it's parent)
   *  and all the parent's attributes
   * and returns a Hashtable of Hashtables
   * where outer key = element's value,  inner key = parent's attrib name value = parent's attrib value
   */
    public Hashtable getParentAttribsOfElement(String parentElement, String element)
    {
        if ((parentElement == null) || (element == null))
            return null;
        
        Hashtable table = new Hashtable();
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String elementText = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                //get the element's value
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if ((textNode != null)&& (textNode.getNodeType() == Node.TEXT_NODE))
                {
                    elementText = textNode.getNodeValue();
                }
                
                if (nodeParent != null)
                {
                    Hashtable parentAttribs = getAttributesFromNode(nodeParent);
                    if ((elementText != null) && (parentAttribs != null))
                    { 
                        if (table.containsKey(elementText)==false)
                            table.put(elementText, parentAttribs);
                        else
                            System.err.println ("WARNING 12: duplicate " + elementText + " under " + parentElement);
                    }
                }
            }
        }  //end for loop
        return table;
    }
    
    
        /* Given an element identified by the value of one of its attributes, its
         * parent element and its grandparent, return the element's text.
         */
    public String getTextFromElementWithAttrib(String element, String attribute, String attribValue, String parent, String grandparent)
    {
        String text = "";
        
        // Go throught the elements in search of the first match.
        NodeList nl = m_Doc.getElementsByTagName(element);
        int i = 0;
        boolean found = false;
        do
        {
            if (nl.getLength() == 0)
                break;
            Node node = nl.item(i);
            NamedNodeMap attrMap = node.getAttributes();
            for (int j=0; j < attrMap.getLength(); j++)
            {
                Node attrNode = attrMap.item(j);
                String key = attrNode.getNodeName();
                String val = attrNode.getNodeValue();
                
                if ((key.compareTo(attribute)==0) && (val.compareTo(attribValue)==0))
                {
                    // Now check the parent and grandparent, see if we have a match.
                    Node parentNode = node.getParentNode();
                    
                    // if both the parent and grandparent match the names passed
                    // as parameters, then we have our match.
                    if ((parentNode != null) && (parentNode.getNodeName().compareTo(parent)==0))
                    {
                        Node grandparentNode = parentNode.getParentNode();
                        
                        if ((grandparentNode != null) && (grandparentNode.getNodeName().compareTo(grandparent)==0))
                        {
                            // match found.
                            NodeList nlChildren = node.getChildNodes();
                            Node textNode = nlChildren.item(0);
                            if ((textNode != null) && (textNode.getNodeType() == Node.TEXT_NODE))
                            {
                                text = textNode.getNodeValue();
                                break;
                            }
                        }
                    }
                }
            }
            i++;
        } while ((i < nl.getLength()) && (!found));
        
        return text;
    }
    
    
    /* get all attributes and text for the element identified by element and its parent element
     *  returns a Vector of Hashtables (key "innerText"=>data is String, key "attributes"=>hashtable (key=attr name, value=attr value))
     * Adapted from getAttributesFromElement(String parentElement, String element)
     */
    public Vector getAttributesAndTextFromAllElements(String parentElement, String element)
    {
        Vector allElements = new Vector(); // to be returned
        
        if ((parentElement == null) || (element == null))
            return null;
        
        //Vector allElementAttributes = new Vector();
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable attribsText = new Hashtable();
            Hashtable attributes = new Hashtable();
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                // get element's attribs
                NamedNodeMap attrMap = node.getAttributes();
                for (int j=0; j < attrMap.getLength(); j++)
                {
                    Node attrNode = attrMap.item(j);
                    String key = attrNode.getNodeName();
                    String value = attrNode.getNodeValue();
                    attributes.put(key, value);
                }
                
                // get element's inner text
                NodeList nlChildren = node.getChildNodes();
                Node textNode = nlChildren.item(0);
                if ((textNode != null) && (textNode.getNodeType() == Node.TEXT_NODE))
                {
                    String text = textNode.getNodeValue();
                    if (text != null)
                        attribsText.put("innerText", text);
                }
            }
            if (attributes.size()>0)
                attribsText.put("attributes", attributes);
            
            if (attribsText.size()>0)
                allElements.add(attribsText);
        }
        return allElements;
    }
    
    /*
     *Return the text of all elements that match the passed=in element, parent,
     *parentAttribute, parentAttributeValue gparent, ggparent and gggparent.
     *Return Hashtable (key=gggparentAttribute, value=Hashtables) of
     *Hashtables (key=attribute, value=element text.
     */
    public Hashtable getTextFromAllElementsWithGGGParent(String element, String attribute, String parent, String parentAttribute, String parentAttributeValue, String gparent, String ggparent, String gggparent, String gggparentAttribute)
    {
        Hashtable allElements = new Hashtable();
        if ((element == null) || (attribute == null) || (parent == null) || (parentAttribute == null) || (parentAttributeValue == null) || (gparent == null) || (ggparent == null) || (gggparent == null) || (gggparentAttribute == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable elementText = new Hashtable();
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (getAttributeValue(node, attribute) != null)
            && (nodeParent.getNodeName().compareTo(parent) ==0)
            && (nodeAttributeValueMatches(nodeParent, parentAttribute, parentAttributeValue)))
            {
                // grand parent
                Node nodeGParent = nodeParent.getParentNode();
                if ((nodeParent.getNodeType() == Node.ELEMENT_NODE)
                && (nodeGParent.getNodeName().compareTo(gparent) ==0))
                {
                    // great grand parent
                    Node nodeGGParent = nodeGParent.getParentNode();
                    if ((nodeGParent.getNodeType() == Node.ELEMENT_NODE)
                    && (nodeGGParent.getNodeName().compareTo(ggparent) ==0))
                    {
                        // great great grand parent
                        Node nodeGGGParent = nodeGGParent.getParentNode();
                        if ((nodeGGParent.getNodeType() == Node.ELEMENT_NODE)
                        && (nodeGGGParent.getNodeName().compareTo(gggparent) ==0))
                        {
                            String gggparentAttributeValue = getAttributeValue(nodeGGGParent, gggparentAttribute);
                            
                            // now, place value in the hashtable to be returned.
                            String nodeText = getNodeText(node);
                            
                            if ((gggparentAttributeValue != null) && (nodeText != null))
                            {
                                Hashtable innerTable = (Hashtable) allElements.get(gggparentAttributeValue);
                                if (innerTable == null)
                                {
                                    innerTable = new Hashtable();
                                    allElements.put(gggparentAttributeValue, innerTable);
                                }
                                // avoid the case where two elements have the same attribute value.
                                // in that case, only take the first element's value, sinc the elements
                                // will be identified by this attribute's value.
                                String str = getAttributeValue(node, attribute);
                                if (innerTable.get(str) == null)
                                    innerTable.put(str, nodeText);
                                else
                                    System.err.println ("WARNING 13: duplicate " + str + " with " + attribute + " = " + nodeText);
                            } // end if
                        } // end if
                    } // end if
                } // end if
            } // end if
        } // end for
        
        return allElements;
    }
 
   /*
     *Return the text of all elements that match the passed=in element, parent,
     *parentAttribute, parentAttributeValue gparent, ggparent and gggparent.
     *Return Hashtable (key=gggparentAttribute, value=Hashtables) of
     *Hashtables (key=attribute, value=element text.
    * WRITTEN SPECIFICALLY FOR FORMAT/STAND-ALONE ISSUE
     */
    public Hashtable getTextFromAllElementsWithGGGParent(String element, String attribute, String parent, String parentAttribute, String parentAttributeValue, 
              String gparent, String ggparent, String gggparent, String gggparentAttribute, String gparentAttribute, String gparentAttribValue)
    {
        Hashtable allElements = new Hashtable();
        if ((element == null) || (attribute == null) || (parent == null) || (parentAttribute == null) || (parentAttributeValue == null) || (gparent == null) || (ggparent == null) || (gggparent == null) || (gggparentAttribute == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable elementText = new Hashtable();
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (getAttributeValue(node, attribute) != null)
            && (nodeParent.getNodeName().compareTo(parent) ==0)
            && (nodeAttributeValueMatches(nodeParent, parentAttribute, parentAttributeValue)))
            {
                // grand parent
                Node nodeGParent = nodeParent.getParentNode();

                if ((nodeParent.getNodeType() == Node.ELEMENT_NODE)
                && (nodeGParent.getNodeName().compareTo(gparent) ==0))
                {
                    if (getAttributeValue(nodeGParent, gparentAttribute).compareTo(gparentAttribValue) !=0)
                        continue;  //if it's not foprmat then skip it.
                    
                    // great grand parent
                    Node nodeGGParent = nodeGParent.getParentNode();
                    if ((nodeGParent.getNodeType() == Node.ELEMENT_NODE)
                    && (nodeGGParent.getNodeName().compareTo(ggparent) ==0))
                    {
                        // great great grand parent
                        Node nodeGGGParent = nodeGGParent.getParentNode();
                        if ((nodeGGParent.getNodeType() == Node.ELEMENT_NODE)
                        && (nodeGGGParent.getNodeName().compareTo(gggparent) ==0))
                        {
                            String gggparentAttributeValue = getAttributeValue(nodeGGGParent, gggparentAttribute);
                            
                            // now, place value in the hashtable to be returned.
                            String nodeText = getNodeText(node);
                            
                            if ((gggparentAttributeValue != null) && (nodeText != null))
                            {
                                Hashtable innerTable = (Hashtable) allElements.get(gggparentAttributeValue);
                                if (innerTable == null)
                                {
                                    innerTable = new Hashtable();
                                    allElements.put(gggparentAttributeValue, innerTable);
                                }
                                // avoid the case where two elements have the same attribute value.
                                // in that case, only take the first element's value, sinc the elements
                                // will be identified by this attribute's value.
                                String str = getAttributeValue(node, attribute);
                                if (innerTable.get(str) == null)
                                    innerTable.put(str, nodeText);
                                else
                                    System.err.println ("WARNING 13: duplicate " + str + " with " + attribute + " = " + nodeText);
                            } // end if
                        } // end if
                    } // end if
                } // end if
            } // end if
        } // end for
        
        return allElements;
    }
    
    /*
     *Return the text of all elements that match the passed=in element, parent,
     *parentAttribute, parentAttributeValue gparent, ggparent.
     *Return Hashtable (key=ggparentAttribute, value=Hashtables) of
     *Hashtables (key=attribute, value=element text.
     */
    public Hashtable getTextFromAllElementsWithGGParent(String element, String attribute, String parent, String gparent, String ggparent, String ggparentAttribute)
    {
        Hashtable allElements = new Hashtable();
        if ((element == null) || (attribute == null) || (parent == null) || (gparent == null) || (ggparent == null) || (ggparentAttribute == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable elementText = new Hashtable();
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (getAttributeValue(node, attribute) != null)
            && (nodeParent.getNodeName().compareTo(parent) ==0))
            {
                // grand parent
                Node nodeGParent = nodeParent.getParentNode();
                if ((nodeParent.getNodeType() == Node.ELEMENT_NODE)
                && (nodeGParent.getNodeName().compareTo(gparent) ==0))
                {
                    // great grand parent
                    Node nodeGGParent = nodeGParent.getParentNode();
                    if ((nodeGParent.getNodeType() == Node.ELEMENT_NODE)
                    && (nodeGGParent.getNodeName().compareTo(ggparent) ==0))
                    {
                        String ggparentAttributeValue = getAttributeValue(nodeGGParent, ggparentAttribute);
                        
                        // now, place value in the hashtable to be returned.
                        String nodeText = getNodeText(node);
                        
                        if ((ggparentAttributeValue != null) && (nodeText != null))
                        {
                            Hashtable innerTable = (Hashtable) allElements.get(ggparentAttributeValue);
                            if (innerTable == null)
                            {
                                innerTable = new Hashtable();
                                allElements.put(ggparentAttributeValue, innerTable);
                            }
                            // avoid the case where two elements have the same attribute value.
                            // in that case, only take the first element's value, sinc the elements
                            // will be identified by this attribute's value.
                            if (innerTable.get(getAttributeValue(node, attribute)) == null)
                                innerTable.put(getAttributeValue(node, attribute), nodeText);
                            else
                                System.err.println ("WARNING 14: duplicate " + attribute);
                        } // end if
                    } // end if
                } // end if
            } // end if
        } // end for
        
        return allElements;
    }
    
    /*
     *Return the text of all elements that match the passed=in element, parent,
     *parentAttribute, parentAttributeValue gparent, ggparent.
     *If two elements exist, where both's great grand parent's attribute value
     *is identical, only the first of these elements' text will be stored.
     *Return Hashtable (key=ggparentAttribute, value=text)
     */
    public Hashtable getTextFromAllElementsWithGGParent(String element, String parent, String gparent, String ggparent, String ggparentAttribute)
    {
        Hashtable allElements = new Hashtable();
        if ((element == null) || (parent == null) || (gparent == null) || (ggparent == null) || (ggparentAttribute == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Hashtable elementText = new Hashtable();
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parent) ==0))
            {
                // grand parent
                Node nodeGParent = nodeParent.getParentNode();
                if ((nodeParent.getNodeType() == Node.ELEMENT_NODE)
                && (nodeGParent.getNodeName().compareTo(gparent) ==0))
                {
                    // great grand parent
                    Node nodeGGParent = nodeGParent.getParentNode();
                    if ((nodeGParent.getNodeType() == Node.ELEMENT_NODE)
                    && (nodeGGParent.getNodeName().compareTo(ggparent) ==0))
                    {
                        String ggparentAttributeValue = getAttributeValue(nodeGGParent, ggparentAttribute);
                        
                        // now, place value in the hashtable to be returned.
                        String nodeText = getNodeText(node);
                        
                        if ((ggparentAttributeValue != null) && (ggparentAttributeValue.length()>0) && (nodeText != null))
                        {
                            // make sure that an element identified by the same
                            // grandparent attribute value isn't already stored.
                            String text = (String) allElements.get(ggparentAttributeValue);
                            if (text == null)
                                allElements.put(ggparentAttributeValue, nodeText);
                            else
                                System.err.println ("WARNING 15: duplicate " + ggparentAttributeValue + " under " + ggparentAttribute);
                        }
                    } // end if
                } // end if
            } // end if
        } // end for
        
        return allElements;
    }
    
    private boolean nodeAttributeValueMatches(Node node, String attribute, String value)
    {
        boolean matches = false;
        
        if (node == null)
            return false;
        
        // get element's attribs
        NamedNodeMap attrMap = node.getAttributes();
        int j=0;
        while ((j < attrMap.getLength()) && (!matches))
        {
            Node attrNode = attrMap.item(j);
            String key = attrNode.getNodeName();
            String nodeValue = attrNode.getNodeValue();
            
            matches = ((key.compareTo(attribute)==0) && (nodeValue.compareTo(value)==0));
            
            j++;
        }
        
        return matches;
    }
    
    private boolean nodeHasAttribute(Node node, String attribute)
    {
        boolean hasAttribute = false;
        
        if (node == null)
            return false;
        
        // get element's attribs
        NamedNodeMap attrMap = node.getAttributes();
        int j=0;
        while ((j < attrMap.getLength()) && (!hasAttribute))
        {
            Node attrNode = attrMap.item(j);
            String key = attrNode.getNodeName();
            hasAttribute = (key.compareTo(attribute) == 0);
            j++;
        }
        
        return hasAttribute;
    }
    
    private String getAttributeValue(Node node, String attribute)
    {
        String value = null;
        
        if (node == null)
            return null;
        
        // get element's attribs
        NamedNodeMap attrMap = node.getAttributes();
        int j=0;
        while ((j < attrMap.getLength()) && (value == null))
        {
            Node attrNode = attrMap.item(j);
            String key = attrNode.getNodeName();
            
            // found attribute, get its value
            if (key.compareTo(attribute)==0)
                value = attrNode.getNodeValue();
            
            j++;
        }
        
        return value;
    }
    
    private String getNodeText(Node node)
    {
        String text = null;
        
        // get element's inner text
        NodeList nlChildren = node.getChildNodes();
        Node textNode = nlChildren.item(0);
        if ((textNode != null) && (textNode.getNodeType() == Node.TEXT_NODE))
            text = textNode.getNodeValue();
        
        return text;
    }
    
     /* retrieves value of each element's attribute (where each element is identified
      * by its own name and that of its parent, and unique given the value of it grand parent's attribute).
      *
      * Returns a Hashtable where key = grandParent's attribute value,  value = element's attribute value,
      * a row for each mathcing element found.
      */
    public Hashtable getAttributeFromElementsAndGPAttrib(String parentElement, String element, String elementAttrib, String grandParentAttrib)
    {
        if ((parentElement == null) || (element == null) || (elementAttrib == null) || (grandParentAttrib == null))
            return null;
        
        Hashtable outer = new Hashtable();
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            String attributeValue = null;
            String gparentAttribValue = null;
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                //get the element attribute's value
                attributeValue = getAttributeValue(node, elementAttrib);
                
                //get the grandParent's attrib
                Node grandParentNode = nodeParent.getParentNode();
                if ((attributeValue != null) && (grandParentNode != null))
                {
                    gparentAttribValue = getAttributeValue(grandParentNode, grandParentAttrib);
                    
                    if (gparentAttribValue != null)
                    {
                        if (outer.containsKey(gparentAttribValue)==false)
                            outer.put(gparentAttribValue, attributeValue);
                        else
                            System.err.println ("WARNING 16: duplicate " + gparentAttribValue + " for " + grandParentAttrib);
                    }
                }
            }
        }  //end for loop
        
        return outer;
    }
    
    
    
    // gets the value of the identified attribute from the first element called "element"
    // that has a parent named "parentElement".
    //returns null if attribute or element not found
    public String getAttributeFromElement(String parentElement, String element, String elementAttrib)
    {
        if ((parentElement == null) || (element == null) || (elementAttrib == null) )
            return null;
        
        String attributeValue = null;
        
        Hashtable outer = new Hashtable();
        NodeList nl = m_Doc.getElementsByTagName(element);
        for (int i=0; i < nl.getLength(); i++)
        {
            Node node = nl.item(i);
            Node nodeParent = node.getParentNode();
            if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (nodeParent.getNodeName().compareTo(parentElement) ==0))
            {
                //get the element attribute's value
                attributeValue = getAttributeValue(node, elementAttrib);
            }
        }  //end for loop
        
        return attributeValue;
    }
    
    /* get all attributes for the element identified by element and its parent element
     *  returns a Vector of Vectors of Hashtables (key=attr name, value=attr value).
     *  Each inner Vector contains the elements of one unique parent node.  So, a Vector
     *  exists for each unique parent node who match parentElement param and whose
     *  children match the element param.
     */
    public Vector getAttributesFromElementGroups(String parentElement, String element)
    {
        Vector allParentGroups = new Vector();
        
        if ((parentElement == null) || (element == null))
            return null;
        
        NodeList nl = m_Doc.getElementsByTagName(parentElement);
        
        for (int i=0; i < nl.getLength(); i++)
        {
            Vector parentGroup = new Vector();
            Node nodeParent = nl.item(i);  // the parent node
            NodeList nlChildren = nodeParent.getChildNodes();
            for (int j=0; j < nlChildren.getLength(); j++)
            {
                // Check if children match.  If so, add their attributes as
                // hashtables to this parent?s vector of hashtables.
                Hashtable attributes = new Hashtable();
                Node node = nlChildren.item(j);
                
                if ((node.getNodeType() == Node.ELEMENT_NODE)
                && (node.getNodeName().compareTo(element) ==0))
                {
                    // Get the node?s attributes into a Hashtable.
                    NamedNodeMap attrMap = node.getAttributes();
                    for (int k=0; k < attrMap.getLength(); k++)
                    {
                        Node attrNode = attrMap.item(k);
                        String key = attrNode.getNodeName();
                        String value = attrNode.getNodeValue();
                        attributes.put(key, value);
                    }
                }
                if (attributes.size()>0)
                    parentGroup.add(attributes);
                
            }
            if (parentGroup.size()>0)
                allParentGroups.add(parentGroup);
            
        }
        
        return allParentGroups;
    }
    
    // Return true if there exists an element in the document that matches
    // the named element.
    public boolean elementExists(String element)
    {
        boolean exists = false;
        if ((element == null) || (element.length()==0))
            return exists;
        
        NodeList nl = m_Doc.getElementsByTagName(element);
        if (nl.getLength()>0)
            exists = true;
        else
            exists = false;
        
        return exists;
    }
    
    //retireve data from element attrib, identified by parent element and parent atrtrib and GP eleemnt and attrib
    // and puts res in Hashtable of Vectors where :
    //      key = GP atrrib value, Vecotr[0] = parent attrib value, vector[1] = element attrib's value
    // i.e. in suplemental data :
    //      <region iso3166="GR">
    //           <currency iso4217="EUR">
    //              <alternate iso4217="GRD"/>
    //  would give key = "GR", vector[0] = "EUR", vector[1] = "GRD"
/*    public Hashtable getAttribsFrom_El_parent_GP(String el, String elAttr, String parEl, String parAttr, String gpEl, String gpAttr)
    {
        if ((el==null) || (elAttr==null) || (parEl==null) || (parAttr==null) || (gpEl==null) || (gpAttr==null))
            return null;
        
        
        //get the attrib vals of all the GP eleemnts
        Vector iso3166REgions = getAttrsFromElement(gpEl, gpAttr);
        if (iso3166REgions == null)
            return null;
        
        Hashtable table = new Hashtable();
        
        for (int i=0; i < iso3166REgions.size(); i++)
        {
            Vector iso4217currencies = new Vector();
            String iso4217currency = getAttributesFromElement(gpEl, parEl, parAttr, gpAttr, (String)iso3166REgions.elementAt(i));
            if (iso4217currency != null)
            {
                iso4217currencies.add(0, iso4217currency);
                
                Vector iso4217currency_alt = getTextFromElementsAndGPAttrib (el, elAttr,  gpEl, gpAttr, (String)iso3166REgions.elementAt(i));
                if (iso4217currency_alt != null)
                {
                    for (int j=0; j < iso4217currency_alt.size (); j++)
                        iso4217currencies.add (iso4217currency_alt.elementAt(j));
                }
                table.put(iso3166REgions.elementAt(i), iso4217currencies);
            }
        }
        return table;
    }*/
    
    //retireve data from element attrib, identified by parent element and parent atrtrib
    // and puts res in Hashtable of Vectors where :
    //      key = parent atrrib value, Vecotr[0] = 1st element attrib's value, vector[1] = 2nd element attrib's value
    // i.e. in suplemental data :
    //      <region iso3166="GR">
    //           <currency iso4217="EUR">
    //           <currency iso4217="GRD"/>
    //  would give key = "GR", vector[0] = "EUR", vector[1] = "GRD"
/*    public Hashtable getAttribsFrom_El_parent(String el, String elAttr, String parEl, String parAttr)
    {
        if ((el==null) || (elAttr==null) || (parEl==null) || (parAttr==null))
            return null;
        
        //get the attrib vals of all the parent eleemnts
        Vector iso3166REgions = getAttrsFromElement(parEl, parAttr);
        if (iso3166REgions == null)
            return null;
        
        Hashtable table = new Hashtable();  
        for (int i=0; i < iso3166REgions.size(); i++)
        {
            Vector iso4217currencies = getAttributesFromElement2(parEl, el, elAttr, parAttr, (String)iso3166REgions.elementAt(i));
            table.put(iso3166REgions.elementAt(i), iso4217currencies);
        }
        return table;
    }*/
    
 
}
