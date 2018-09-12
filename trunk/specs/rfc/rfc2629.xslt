<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                
                xmlns:msxsl="urn:schemas-microsoft-com:xslt"
                xmlns:exslt="http://exslt.org/common"
                xmlns:myns="mailto:julian.reschke@greenbytes.de?subject=rcf2629.xslt"
                xmlns:ed="http://greenbytes.de/2002/rfcedit"

                exclude-result-prefixes="msxsl exslt myns ed"
                >
<!--
    XSLT transformation from RFC2629 XML format to HTML

    Copyright (c) 2001-2004 Julian F. Reschke (julian.reschke@greenbytes.de)

    placed into the public domain

    change history:

    2001-03-28  julian.reschke@greenbytes.de

    Code rearranged, generate numbered section anchors for paragraphs (t)
    as well. Fixes in index handling.

    2001-04-12  julian.reschke@greenbytes.de

    Moved HTML output into XHTML namespace.

    2001-10-02  julian.reschke@greenbytes.de

    Fixed default location for RFCs and numbering of section references.
    Support ?rfc editing processing instruction.

    2001-10-07  julian.reschke@greenbytes.de

    Made telephone number links active.

    2001-10-08  julian.reschke@greenbytes.de

    Support for vspace element.

    2001-10-09  julian.reschke@greenbytes.de

    Experimental support for rfc-issue PI.

    2001-11-11  julian.reschke@greenbytes.de

    Support rfc private PI. Removed bogus code reporting the WG in the header.

    2001-12-17  julian.reschke@greenbytes.de

    Support title attribute on references element

    2002-01-05  julian.reschke@greenbytes.de

    Support for list/@style="@format"

    2002-01-09  julian.reschke@greenbytes.de

    Display "closed" RFC issues as deleted

    2002-01-14  julian.reschke@greenbytes.de

    Experimentally and optionally parse XML encountered in artwork elements
    (requires MSXSL).

    2002-01-27  julian.reschke@greenbytes.de

    Some cleanup. Moved RFC issues from PIs into namespaced elements.

    2002-01-29  julian.reschke@greenbytes.de

    Added support for sortrefs PI. Added support for figure names.

    2002-02-07  julian.reschke@greenbytes.de

    Highlight parts of artwork which are too wide (72 characters).

    2002-02-12  julian.reschke@greenbytes.de

    Code rearrangement for static texts. Fixes for section numbering.
    TOC generation rewritten.

    2002-02-15  julian.reschke@greenbytes.de

    Support for irefs in sections; support iref @primary=true

    2002-03-03  julian.reschke@greenbytes.de

    Moved anchor prefix into a constant. Added sanity checks on user anchor
    names.

    2002-03-23  julian.reschke@greenbytes.de

    Bugfix in detection of matching org names when creating the header. Fixed
    sorting in subitems.

    2002-04-02  julian.reschke@greenbytes.de

    Fix TOC link HTML generation when no TOC is generated (created broken
    HTML table code).

    2002-04-03  julian.reschke@greenbytes.de

    Made rendering of references more tolerant re: missing parts.

    2002-04-08  julian.reschke@greenbytes.de

    Fixed reference numbering when references are split into separate sections.

    2002-04-16  julian.reschke@greenbytes.de

    Fix default namespace (shouldn't be set for HTML output method).

    2002-04-19  julian.reschke@greenbytes.de

    Lowercase internal CSS selectors for Mozilla compliance. Do not put TOC
    into ul element.

    2002-04-21  julian.reschke@greenbytes.de

    Make numbered list inside numbered lists use alphanumeric numbering.

    2002-05-05  julian.reschke@greenbytes.de

    Updated issue/editing support.

    2002-05-15  julian.reschke@greenbytes.de

    Bugfix for section numbering after introduction of ed:replace

    2002-06-21  julian.reschke@greenbytes.de

    When producing private documents, do not include document status, copyright etc.

    2002-07-08  julian.reschke@greenbytes.de

    Fix xrefs to Appendices.

    2002-07-19  fielding

    Make artwork lightyellow for easier reading.

    2002-10-09  fielding

    Translate references title to anchor name to avoid non-uri characters.
    
    2002-10-13  julian.reschke@greenbytes.de
    
    Support for tocdepth PI.

    2002-11-03  julian.reschke@greenbytes.de
    
    Added temporariry workaround for Mozilla/Transformiix result tree fragment problem.
    (search for 'http://bugzilla.mozilla.org/show_bug.cgi?id=143668')
    
    2002-12-25  julian.reschke@greenbytes.de
    
    xref code: attempt to uppercase "section" and "appendix" when at the start
    of a sentence.
    
    2003-02-02  julian.reschke@greenbytes.de
    
    fixed code for vspace blankLines="0", enhanced display for list with "format" style,
    got rid of HTML blockquote elements, added support for "hangIndent"
    
    2003-04-10  julian.reschke@greenbytes.de
    
    experimental support for appendix and spanx elements
    
    2003-04-19  julian.reschke@greenbytes.de
    
    fixed counting of list numbers in "format %" styles (one counter
    per unique format string). Added more spanx styles.

    2003-05-02  julian.reschke@greenbytes.de
    
    experimental texttable support
    
    2003-05-02  fielding 
    
    Make mailto links optional (default = none) (jre: default and PI name changed)

    2003-05-04  julian.rechke@greenbytes.de
    
    experimental support for HTML link elements; fix default for table header
    alignment default

    2003-05-06  julian.rechke@greenbytes.de
    
    support for "background" PI.
    
    2003-05-11  julian.reschke@greenbytes.de
    
    change %c format to lowercase alphabetic. add support for keyword
    elements (generate META tag). fix various HTML conformance problems.
    added experimental support for role attribute. do not number paragraphs
    in unnumbered sections. update boilerplate texts. support for
    "iprnotified" PI. bugfix list numbering. strip whitespace when
    building tel: URIs.
    
    2003-05-12  julian.reschke@greenbytes.de
  
    more conformance fixes (layout moved into CSS, move lists and figures
    out of para content, do not use tables for list formatting)
    
    2003-05-13  julian.reschke@greenbytes.de
  
    add DC.Creator meta tag, refactoring

    2003-05-16  julian.reschke@greenbytes.de
  
    put nbsps between "section" and section number (xref).

    2003-05-18  julian.reschke@greenbytes.de
  
    author summary: add missing comma.
    
    2003-06-06  julian.reschke@greenbytes.de
    
    fix index generation bug (transposed characters in key generation). Enhance
    sentence start detection (xref starting a section was using lowercase
    "section").
    
    2003-06-22  julian.reschke@greenbytes.de
    
    exp. support for xref/@format. Add missing support for eref w/o content.
    exp. support for annotations in reference elements. Code cleanup
    reference table formatting.
    
    2003-07-09  julian.reschke@greenbytes.de
    
    Another fix for DC.Creator meta tag creation based on RFC2731

    2003-07-24  julian.reschke@greenbytes.de
    
    Fix namespace name for DC.Creator.
    
    2003-08-06  julian.reschke@greenbytes.de
    
    Cleanup node-set support (only use exslt (saxon, xalan, libxslt) extension
    functions; remove Transformix workarounds that stopped to work in Moz 1.4)

    2003-08-09  julian.reschke@greenbytes.de
    
    Generate HTML lang tag.
    
    2003-08-10  julian.reschke@greenbytes.de
    
    Map spanx/verb to HTML "samp" element. Fix author name display in
    references (reverse surname/initials for last author), add "Ed.".
    Fix internal bookmark generation.
    
    2003-08-17  julian.reschke@greenbytes.de
    
    Add DCMI dates, identifiers and abstract. Add PI to suppress DCMI
    generation.  Do not add TOC entry to Copyright Statement when there is
    none. Align RFC2629 PI names and parameter names. Change style for
    inline URIs generated by eref. Add header and footer support.
    Enhance CSS paging properties. Support topblock PI. Added hooks for
    proper XHTML generation through separate XSLT. Enhance warning and
    error messages. Add support for artwork image display. Table formatting
    fixes (borders, thead continuation).

    2003-08-18  julian.reschke@greenbytes.de
    
    Add workaround for MSXML4 node-set and Mozilla node-set issues (fallback
    just displays are warning).
    
    2003-10-06  julian.reschke@greenbytes.de
    
    Add workaround for broken pre/ins handling in Mozilla
    (see <http://bugzilla.mozilla.org/show_bug.cgi?id=204401>). Make use
    of cite attribute on ed:replace. CSS cleanup.
    
    2003-10-08  julian.reschke@greenbytes.de
    
    Fix minor issue detecting the same org for the header (caused by IE's
    non-standard whitespace handling). Fix default handling for /rfc/@category.
    
    2003-11-09  julian.reschke@greenbytes.de
    
    Inherit ed:entered-by from ancestor elements. Change CSS color for inserted
    text to green. Generate issues-list anchor. Do not complain about missing
    targets when the xref element is below ed:del. Remove code that attempted
    to distinguish section/Section when producing links - always use
    uppercase. Fix date rendering for issue resolutions.

    2003-11-29  julian.reschke@greenbytes.de
    
    Fix color values for table backgrounds for issue rendering. Change
    rendering of issue links to use inline-styles. Add colored issue markers to
    issues. 

    2003-12-13  julian.reschke@greenbytes.de
    
    Fix inheritance of ed:entered-by attribute. Display note elements inside
    change tracking as well.
    
    2004-01-18  julian.reschke@greenbytes.de
    
    When PI compact = 'yes', make most CSS print page breaks conditional.

    2004-02-20  julian.reschke@greenbytes.de
    
    Support for RFC3667 IPR changes (xml2rfc 1.22); see
    <http://lists.xml.resource.org/pipermail/xml2rfc/2004-February/001088.html>.
    
    2004-03-11  julian.reschke@greenbytes.de
    
    Add "(if approved)" to "updates" and "obsoletes" unless the document has
    an RFC number.
    
    2004-04-01  julian.reschke@greenbytes.de
    
    Fix RFC3667 output, see <http://lists.xml.resource.org/pipermail/xml2rfc/2004-April/001208.html>
-->


<xsl:output method="html" encoding="iso-8859-1" version="4.0" doctype-public="-//W3C//DTD HTML 4.01//EN" />


<!-- process some of the processing instructions supported by Marshall T. Rose's
     xml2rfc sofware, see <http://xml.resource.org/> -->


<!-- rfc compact PI -->

<xsl:param name="xml2rfc-compact"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'compact=')], '&quot; ', ''),
        'compact=')"
/>

<!-- rfc footer PI -->

<xsl:param name="xml2rfc-footer"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'footer=')], '&quot; ', ''),
        'footer=')"
/>

<!-- rfc header PI -->

<xsl:param name="xml2rfc-header"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'header=')], '&quot; ', ''),
        'header=')"
/>

<!-- include a table of contents if a processing instruction <?rfc?>
     exists with contents toc="yes". Can be overriden by an XSLT parameter -->

<xsl:param name="xml2rfc-toc"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'toc=')], '&quot; ', ''),
        'toc=')"
/>

<!-- optional tocdepth-->

<xsl:param name="xml2rfc-tocdepth"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'tocdepth=')], '&quot; ', ''),
        'tocdepth=')"
/>

<xsl:variable name="parsedTocDepth">
  <xsl:choose>
    <xsl:when test="$xml2rfc-tocdepth='1'">1</xsl:when>
    <xsl:when test="$xml2rfc-tocdepth='2'">2</xsl:when>
    <xsl:when test="$xml2rfc-tocdepth='3'">3</xsl:when>
    <xsl:when test="$xml2rfc-tocdepth='4'">4</xsl:when>
    <xsl:when test="$xml2rfc-tocdepth='5'">5</xsl:when>
    <xsl:otherwise>99</xsl:otherwise>
  </xsl:choose>
</xsl:variable>

<!-- suppress top block if a processing instruction <?rfc?>
     exists with contents tocblock="no". Can be overriden by an XSLT parameter -->

<xsl:param name="xml2rfc-topblock"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'topblock=')], '&quot; ', ''),
        'topblock=')"
/>

<!-- use symbolic reference names instead of numeric ones if a processing instruction <?rfc?>
     exists with contents symrefs="yes". Can be overriden by an XSLT parameter -->

<xsl:param name="xml2rfc-symrefs"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'symrefs=')], '&quot; ', ''),
        'symrefs=')"
/>

<!-- sort references if a processing instruction <?rfc?>
     exists with contents sortrefs="yes". Can be overriden by an XSLT parameter -->

<xsl:param name="xml2rfc-sortrefs"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'sortrefs=')], '&quot; ', ''),
        'sortrefs=')"
/>

<!-- insert editing marks if a processing instruction <?rfc?>
     exists with contents editing="yes". Can be overriden by an XSLT parameter -->

<xsl:param name="xml2rfc-editing"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'editing=')], '&quot; ', ''),
        'editing=')"
/>

<!-- make it a private paper -->

<xsl:param name="xml2rfc-private"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'private=')], '&quot;', ''),
        'private=')"
/>

<!-- background image? -->

<xsl:param name="xml2rfc-background"
  select="substring-after(
      translate(/processing-instruction('rfc')[contains(.,'background=')], '&quot;', ''),
        'background=')"
/>

<!-- extension for XML parsing in artwork -->

<xsl:param name="parse-xml-in-artwork"
  select="substring-after(
      translate(/processing-instruction('rfc-ext')[contains(.,'parse-xml-in-artwork=')], '&quot; ', ''),
        'parse-xml-in-artwork=')"
/>

<!-- extension for exclusing DCMI properties in meta tag (RFC2731) -->

<xsl:param name="xml2rfc-ext-support-rfc2731"
  select="substring-after(
      translate(/processing-instruction('rfc-ext')[contains(.,'support-rfc2731=')], '&quot; ', ''),
        'support-rfc2731=')"
/>

<!-- choose whether or not to do mailto links --> 
  
 <xsl:param name="xml2rfc-linkmailto" 
   select="substring-after( 
       translate(/processing-instruction('rfc')[contains(.,'linkmailto=')], '&quot;', ''), 
         'linkmailto=')" 
 /> 


<!-- iprnotified switch --> 
  
 <xsl:param name="xml2rfc-iprnotified" 
   select="substring-after( 
       translate(/processing-instruction('rfc')[contains(.,'iprnotified=')], '&quot;', ''), 
         'iprnotified=')" 
 /> 


<!-- URL prefix for RFCs. -->

<xsl:param name="rfcUrlPrefix" select="'http://www.ietf.org/rfc/rfc'" />

<!-- warning re: absent node-set ext. function -->
<xsl:variable name="node-set-warning">
  This stylesheet requires either an XSLT-1.0 processor with node-set()
  extension function, or an XSLT-2.0 processor. Therefore, parts of the
  document couldn't be displayed.
</xsl:variable>

<!-- build help keys for indices -->
<xsl:key name="index-first-letter"
  match="iref"
    use="translate(substring(@item,1,1),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />

<xsl:key name="index-item"
  match="iref"
    use="@item" />

<xsl:key name="index-item-subitem"
  match="iref"
    use="concat(@item,'..',@subitem)" />

<!-- character translation tables -->
<xsl:variable name="lcase" select="'abcdefghijklmnopqrstuvwxyz'" />
<xsl:variable name="ucase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />       

<xsl:variable name="plain" select="' #/ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
<xsl:variable name="touri" select="'___abcdefghijklmnopqrstuvwxyz'" />

<!-- prefix for automatically generated anchors -->
<xsl:variable name="anchor-prefix" select="'rfc'" />

<!-- IPR version switch -->
<xsl:variable name="ipr-rfc3667" select="(/rfc/@number &gt; 3708) or not((/rfc/@ipr = 'full2026') or 
  (/rfc/@ipr = 'noDerivativeWorks2026') or (/rfc/@ipr = 'noDerivativeWorksNow') or (/rfc/@ipr = 'none'))" />

          
<!-- Templates for the various elements of rfc2629.dtd -->
              
<xsl:template match="abstract">
  <h1><a name="{$anchor-prefix}.abstract"/>Abstract</h1>
  <xsl:apply-templates />
</xsl:template>

<msxsl:script language="JScript" implements-prefix="myns">
  function parseXml(str) {
    var doc = new ActiveXObject ("MSXML2.DOMDocument");
    doc.async = false;
    if (doc.loadXML (str)) return "";
    return doc.parseError.reason + "\n" + doc.parseError.srcText + " (" + doc.parseError.line + "/" + doc.parseError.linepos + ")";
  }
</msxsl:script>

<xsl:template match="artwork">
  <xsl:if test="not(ancestor::ed:del) and $parse-xml-in-artwork='yes' and function-available('myns:parseXml')">
    <xsl:if test="contains(.,'&lt;?xml')">
      <xsl:variable name="body" select="substring-after(substring-after(.,'&lt;?xml'),'?>')" /> 
      <xsl:if test="$body!='' and myns:parseXml($body)!=''">
        <table style="background-color: red; border-width: thin; border-style: solid; border-color: black;">
        <tr><td>
        XML PARSE ERROR:
        <pre><xsl:value-of select="myns:parseXml($body)" /></pre>
        </td></tr></table>
      </xsl:if>
    </xsl:if>
    <xsl:if test="@ed:parse-xml-after">
      <xsl:if test="myns:parseXml(string(.))!=''">
        <table style="background-color: red; border-width: thin; border-style: solid; border-color: black;">
        <tr><td>
        XML PARSE ERROR:
        <pre><xsl:value-of select="myns:parseXml(string(.))" /></pre>
        </td></tr></table>
      </xsl:if>
    </xsl:if>
  </xsl:if>
  <pre>
    <xsl:call-template name="insertInsDelClass" />
    <!--<xsl:value-of select="." />--><xsl:call-template name="showArtwork">
    <xsl:with-param name="mode" select="'html'" />
    <xsl:with-param name="text" select="." />
    <xsl:with-param name="initial" select="'yes'" />
  </xsl:call-template></pre>
</xsl:template>

<xsl:template match="artwork[@src and starts-with(@type,'image/')]">
  <img src="{@src}" alt="{.}">
    <xsl:copy-of select="@width|@height"/>
  </img>
</xsl:template>

<xsl:template match="author">
  <tr>
    <td>&#0160;</td>
    <td>
      <xsl:value-of select="@fullname" />
      <xsl:if test="@role">
        (<xsl:value-of select="@role" />)
      </xsl:if>
    </td>
  </tr>
  <tr>
    <td>&#0160;</td>
    <td><xsl:value-of select="organization" /></td>
  </tr>
  <xsl:if test="address/postal/street!=''">
    <tr>
      <td>&#0160;</td>
      <td><xsl:for-each select="address/postal/street"><xsl:value-of select="." /><br /></xsl:for-each></td>
    </tr>
  </xsl:if>
  <xsl:if test="address/postal/city|address/postal/region|address/postal/code">
    <tr>
      <td>&#0160;</td>
      <td>
        <xsl:if test="address/postal/city"><xsl:value-of select="address/postal/city" />, </xsl:if>
        <xsl:if test="address/postal/region"><xsl:value-of select="address/postal/region" />&#160;</xsl:if>
        <xsl:if test="address/postal/code"><xsl:value-of select="address/postal/code" /></xsl:if>
      </td>
    </tr>
  </xsl:if>
  <xsl:if test="address/postal/country">
    <tr>
      <td>&#0160;</td>
      <td><xsl:value-of select="address/postal/country" /></td>
    </tr>
  </xsl:if>
  <xsl:if test="address/phone">
    <tr>
      <td class="right"><b>Phone:&#0160;</b></td>
      <td><a href="tel:{translate(address/phone,' ','')}"><xsl:value-of select="address/phone" /></a></td>
    </tr>
  </xsl:if>
  <xsl:if test="address/facsimile">
    <tr>
      <td class="right"><b>Fax:&#0160;</b></td>
      <td><a href="fax:{translate(address/facsimile,' ','')}"><xsl:value-of select="address/facsimile" /></a></td>
    </tr>
  </xsl:if>
  <xsl:if test="address/email">
    <tr>
      <td class="right"><b>EMail:&#0160;</b></td>
      <td>
        <a>
          <xsl:if test="$xml2rfc-linkmailto!='no'">
            <xsl:attribute name="href">mailto:<xsl:value-of select="address/email" /></xsl:attribute>
          </xsl:if>
          <xsl:value-of select="address/email" />
        </a>
      </td>
    </tr>
  </xsl:if>
  <xsl:if test="address/uri">
    <tr>
      <td class="right"><b>URI:&#0160;</b></td>
      <td><a href="{address/uri}"><xsl:value-of select="address/uri" /></a></td>
    </tr>
  </xsl:if>
  <tr>
    <td>&#0160;</td>
    <td />
  </tr>
</xsl:template>

<xsl:template match="back">

  <!-- add references section first, no matter where it appears in the
    source document -->
  <xsl:apply-templates select="references" />
   
  <!-- next, add information about the document's authors -->
  <xsl:call-template name="insertAuthors" />
    
  <!-- add all other top-level sections under <back> -->
  <xsl:apply-templates select="*[not(self::references)]" />

  <xsl:if test="not($xml2rfc-private)">
    <!-- copyright statements -->
    <xsl:variable name="copyright"><xsl:call-template name="insertCopyright" /></xsl:variable>
  
    <!-- emit it -->
    <xsl:choose>
      <xsl:when test="function-available('msxsl:node-set')">
        <xsl:apply-templates select="msxsl:node-set($copyright)" />
      </xsl:when>
      <xsl:when test="function-available('exslt:node-set')">
        <xsl:apply-templates select="exslt:node-set($copyright)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:message><xsl:value-of select="$node-set-warning"/></xsl:message>
        <p class="error"><xsl:value-of select="$node-set-warning"/></p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
  
  <!-- insert the index if index entries exist -->
  <xsl:if test="//iref">
    <xsl:call-template name="insertIndex" />
  </xsl:if>

</xsl:template>

<xsl:template match="eref[node()]">
  <a href="{@target}"><xsl:apply-templates /></a>
</xsl:template>
               
<xsl:template match="eref[not(node())]">
  <xsl:text>&lt;</xsl:text>
  <a href="{@target}"><xsl:value-of select="@target" /></a>
  <xsl:text>&gt;</xsl:text>
</xsl:template>

<xsl:template match="figure">
  <xsl:if test="@anchor!=''">
    <div><a name="{@anchor}" /></div>
  </xsl:if>
  <xsl:choose>
    <xsl:when test="@title!='' or @anchor!=''">
      <xsl:variable name="n"><xsl:number level="any" count="figure[@title!='' or @anchor!='']" /></xsl:variable>
      <div><a name="{$anchor-prefix}.figure.{$n}" /></div>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="n"><xsl:number level="any" count="figure[not(@title!='' or @anchor!='')]" /></xsl:variable>
      <div><a name="{$anchor-prefix}.figure.u.{$n}" /></div>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates />
  <xsl:if test="@title!='' or @anchor!=''">
    <xsl:variable name="n"><xsl:number level="any" count="figure[@title!='' or @anchor!='']" /></xsl:variable>
    <p class="figure">Figure <xsl:value-of select="$n"/><xsl:if test="@title!=''">: <xsl:value-of select="@title" /></xsl:if></p>
  </xsl:if>
</xsl:template>

<xsl:template match="front">
  
  <xsl:if test="$xml2rfc-topblock!='no'">
    <xsl:call-template name="insertTocLink">
      <xsl:with-param name="includeTitle" select="true()" />
    </xsl:call-template>

    <!-- collect information for left column -->
      
    <xsl:variable name="leftColumn">
      <xsl:call-template name="collectLeftHeaderColumn" />    
    </xsl:variable>
  
    <!-- collect information for right column -->
      
    <xsl:variable name="rightColumn">
      <xsl:call-template name="collectRightHeaderColumn" />    
    </xsl:variable>
      
    <!-- insert the collected information -->
    <table summary="header information" class="header" border="0" cellpadding="1" cellspacing="1">
      <xsl:choose>
        <xsl:when test="function-available('msxsl:node-set')">
          <xsl:call-template name="emitheader">
            <xsl:with-param name="lc" select="msxsl:node-set($leftColumn)" />    
            <xsl:with-param name="rc" select="msxsl:node-set($rightColumn)" />    
          </xsl:call-template>
        </xsl:when>    
        <xsl:when test="function-available('exslt:node-set')">
          <xsl:call-template name="emitheader">
            <xsl:with-param name="lc" select="exslt:node-set($leftColumn)" />    
            <xsl:with-param name="rc" select="exslt:node-set($rightColumn)" />    
          </xsl:call-template>
        </xsl:when>    
        <xsl:otherwise>
          <xsl:message><xsl:value-of select="$node-set-warning"/></xsl:message>
          <p class="error"><xsl:value-of select="$node-set-warning"/></p>
        </xsl:otherwise>
      </xsl:choose>
    </table>
  </xsl:if>
    
  <p class="title">
    <!-- main title -->
    <xsl:value-of select="title"/>
    <xsl:if test="/rfc/@docName">
      <br/>
      <span class="filename"><xsl:value-of select="/rfc/@docName"/></span>
    </xsl:if>  
  </p>
  
  <xsl:if test="not($xml2rfc-private)">
    <!-- Get status info formatted as per RFC2629-->
    <xsl:variable name="preamble"><xsl:call-template name="insertPreamble" /></xsl:variable>
    
    <!-- emit it -->
    <xsl:choose>
      <xsl:when test="function-available('msxsl:node-set')">
        <xsl:apply-templates select="msxsl:node-set($preamble)" />
      </xsl:when>
      <xsl:when test="function-available('exslt:node-set')">
        <xsl:apply-templates select="exslt:node-set($preamble)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:message><xsl:value-of select="$node-set-warning"/></xsl:message>
        <p class="error"><xsl:value-of select="$node-set-warning"/></p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
            
  <xsl:apply-templates select="abstract" />
  <xsl:apply-templates select="note" />
  <!-- show notes inside change tracking as well -->
  <xsl:apply-templates select="ed:replace[.//note]" />
    
  <xsl:if test="$xml2rfc-toc='yes'">
    <xsl:apply-templates select="/" mode="toc" />
    <xsl:call-template name="insertTocAppendix" />
  </xsl:if>

</xsl:template>


<xsl:template match="iref">
  <a><xsl:attribute name="name"><xsl:value-of select="$anchor-prefix"/>.iref.<xsl:number level="any"/></xsl:attribute></a>
</xsl:template>

<!-- list templates depend on the list style -->

<xsl:template match="list[@style='empty' or not(@style)]">
  <dl>
    <xsl:call-template name="insertInsDelClass"/>
    <xsl:apply-templates />
  </dl>
</xsl:template>

<xsl:template match="list[starts-with(@style,'format ')]">
  <dl>
    <xsl:call-template name="insertInsDelClass"/>
    <xsl:apply-templates />
  </dl>
</xsl:template>

<xsl:template match="list[@style='hanging']">
  <dl>
    <xsl:call-template name="insertInsDelClass"/>
    <xsl:apply-templates />
  </dl>
</xsl:template>

<xsl:template match="list[@style='numbers']">
  <ol>
    <xsl:call-template name="insertInsDelClass"/>
    <xsl:apply-templates />
  </ol>
</xsl:template>

<!-- numbered list inside numbered list -->
<xsl:template match="list[@style='numbers']/t/list[@style='numbers']" priority="9">
  <ol style="list-style-type: lower-alpha">
    <xsl:call-template name="insertInsDelClass"/>
    <xsl:apply-templates />
  </ol>
</xsl:template>

<xsl:template match="list[@style='symbols']">
  <ul>
    <xsl:call-template name="insertInsDelClass"/>
    <xsl:apply-templates />
  </ul>
</xsl:template>

<!-- same for t(ext) elements -->

<xsl:template match="list[@style='empty' or not(@style)]/t">
  <dd style="margin-top: .5em">
    <xsl:apply-templates />
  </dd>
</xsl:template>

<xsl:template match="list[@style='numbers' or @style='symbols']/t">
  <li>
    <xsl:apply-templates />
  </li>
</xsl:template>

<xsl:template match="list[@style='hanging']/t">
  <dt style="margin-top: .5em">
    <xsl:value-of select="@hangText" />
  </dt>
  <dd>
    <!-- if hangIndent present, use 0.7 of the specified value (1em is the width of the "m" character -->
    <xsl:if test="../@hangIndent and ../@hangIndent!='0'">
      <xsl:attribute name="style">margin-left: <xsl:value-of select="../@hangIndent * 0.7"/>em</xsl:attribute>
    </xsl:if>
    <xsl:apply-templates />
  </dd>
</xsl:template>

<xsl:template match="list[starts-with(@style,'format ') and (contains(@style,'%c') or contains(@style,'%d'))]/t">
  <xsl:variable name="list" select=".." />
  <xsl:variable name="format" select="substring-after(../@style,'format ')" />
  <xsl:variable name="pos">
    <xsl:choose>
      <xsl:when test="$list/@counter">
        <xsl:number level="any" count="list[@counter=$list/@counter or (not(@counter) and @style=concat('format ',$list/@counter))]/t" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:number level="any" count="list[concat('format ',@counter)=$list/@style or (not(@counter) and @style=$list/@style)]/t" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <dt>
    <xsl:choose>
      <xsl:when test="contains($format,'%c')">
        <xsl:value-of select="substring-before($format,'%c')"/><xsl:number value="$pos" format="a" /><xsl:value-of select="substring-after($format,'%c')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="substring-before($format,'%d')"/><xsl:number value="$pos" format="1" /><xsl:value-of select="substring-after($format,'%d')"/>
      </xsl:otherwise>
    </xsl:choose>
  </dt>
  <dd>
    <xsl:apply-templates />
  </dd>
</xsl:template>

<xsl:template match="middle">
  <xsl:apply-templates />
</xsl:template>

<xsl:template match="note">
  <xsl:variable name="num"><xsl:number/></xsl:variable>
  <h1><a name="{$anchor-prefix}.note.{$num}"/><xsl:value-of select="@title" /></h1>
    <xsl:apply-templates />
</xsl:template>

<xsl:template match="postamble">
  <p>
    <xsl:call-template name="editingMark" />
    <xsl:apply-templates />
  </p>
</xsl:template>

<xsl:template match="preamble">
  <p>
    <xsl:call-template name="editingMark" />
    <xsl:apply-templates />
  </p>
</xsl:template>


<xsl:template match="reference">

  <xsl:variable name="target">
    <xsl:choose>
      <xsl:when test="@target"><xsl:value-of select="@target" /></xsl:when>
      <xsl:when test="seriesInfo/@name='RFC'"><xsl:value-of select="concat($rfcUrlPrefix,seriesInfo[@name='RFC']/@value,'.txt')" /></xsl:when>
      <xsl:when test="seriesInfo[starts-with(.,'RFC')]">
        <xsl:variable name="rfcRef" select="seriesInfo[starts-with(.,'RFC')]" />
        <xsl:value-of select="concat($rfcUrlPrefix,substring-after (normalize-space($rfcRef), ' '),'.txt')" />
      </xsl:when>
      <xsl:otherwise />
    </xsl:choose>
  </xsl:variable>
  
  <tr>
    <td class="topnowrap">
      <b>
        <a name="{@anchor}">
          <xsl:call-template name="referencename">
            <xsl:with-param name="node" select="." />
          </xsl:call-template>
        </a>
      </b>
    </td>
    
    <td class="top">
      <xsl:for-each select="front/author">
        <xsl:choose>
          <xsl:when test="@surname and @surname!=''">
            <xsl:variable name="displayname">
              <!-- surname/initials is reversed for last author except when it's the only one -->
              <xsl:choose>
                <xsl:when test="position()=last() and position()!=1">
                  <xsl:value-of select="concat(@initials,' ',@surname)" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat(@surname,', ',@initials)" />
                </xsl:otherwise>
              </xsl:choose>
              <xsl:if test="@role='editor'">
                <xsl:text>, Ed.</xsl:text>
              </xsl:if>
            </xsl:variable>
            <xsl:choose>
               <xsl:when test="address/email">
                <a>
                  <xsl:if test="$xml2rfc-linkmailto!='no'">
                    <xsl:attribute name="href">mailto:<xsl:value-of select="address/email" /></xsl:attribute>
                  </xsl:if>
                  <xsl:if test="organization/text()">
                    <xsl:attribute name="title"><xsl:value-of select="organization/text()"/></xsl:attribute>
                  </xsl:if>
                  <xsl:value-of select="$displayname" />
                </a>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="$displayname" />
              </xsl:otherwise>
            </xsl:choose>
            
            <xsl:if test="position()!=last() - 1">,&#0160;</xsl:if>
            <xsl:if test="position()=last() - 1"> and </xsl:if>
          </xsl:when>
          <xsl:when test="organization/text()">
            <xsl:choose>
              <xsl:when test="address/uri">
                <a href="{address/uri}"><xsl:value-of select="organization" /></a>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="organization" />
              </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="position()!=last() - 1">,&#0160;</xsl:if>
            <xsl:if test="position()=last() - 1"> and </xsl:if>
          </xsl:when>
          <xsl:otherwise />
        </xsl:choose>
      </xsl:for-each>
         
      <xsl:choose>
        <xsl:when test="string-length($target) &gt; 0">
          <xsl:text>"</xsl:text><a href="{$target}"><xsl:value-of select="front/title" /></a><xsl:text>"</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>"</xsl:text><xsl:value-of select="front/title" /><xsl:text>"</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
            
      <xsl:for-each select="seriesInfo">
        <xsl:text>, </xsl:text>
        <xsl:choose>
          <xsl:when test="not(@name) and not(@value) and ./text()"><xsl:value-of select="." /></xsl:when>
          <xsl:otherwise><xsl:value-of select="@name" /><xsl:if test="@value!=''">&#0160;<xsl:value-of select="@value" /></xsl:if></xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
      
      <xsl:if test="front/date/@year != '' and front/date/@year != '???'">
        <xsl:text>, </xsl:text>
        <xsl:if test="front/date/@month and front/date/@month!='???'"><xsl:value-of select="front/date/@month" />&#0160;</xsl:if>
        <xsl:value-of select="front/date/@year" />
      </xsl:if>
      
      <xsl:text>.</xsl:text>

      <xsl:for-each select="annotation">
        <br />
        <xsl:apply-templates />
      </xsl:for-each>

    </td>
  </tr>
  
  
</xsl:template>


<xsl:template match="references">

  <xsl:call-template name="insertTocLink">
    <xsl:with-param name="rule" select="true()" />
  </xsl:call-template>

  <xsl:variable name="name">
    <xsl:choose>
      <xsl:when test="not(preceding::references)" />
      <xsl:otherwise>
        <xsl:text>.</xsl:text><xsl:number/>      
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <h1>
    <!-- force page break before first reference section -->
    <xsl:if test="$name=''">
      <xsl:call-template name="insert-conditional-pagebreak"/>
    </xsl:if>
    
    <a name="{$anchor-prefix}.references{$name}">
      <xsl:choose>
        <xsl:when test="not(@title) or @title=''">References</xsl:when>
        <xsl:otherwise><xsl:value-of select="@title"/></xsl:otherwise>
      </xsl:choose>
    </a>
  </h1>
 
  <table summary="{@title}" border="0" cellpadding="2">
    <xsl:choose>
      <xsl:when test="$xml2rfc-sortrefs='yes'">
        <xsl:apply-templates>
          <xsl:sort select="@anchor" />
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates />
      </xsl:otherwise>
    </xsl:choose>
  </table>

</xsl:template>

<xsl:template match="rfc">
  
  <xsl:variable name="lang">
    <xsl:call-template name="get-lang" />
  </xsl:variable>

  <html lang="{$lang}">
    <head>
      <title><xsl:value-of select="front/title" /></title>
      <style type="text/css" title="Xml2Rfc (sans serif)">
        <xsl:call-template name="insertCss" />
      </style>
      <!-- <link rel="alternate stylesheet" type="text/css" media="screen" title="Plain (typewriter)" href="rfc2629tty.css" /> -->
            
      <!-- link elements -->
      <xsl:if test="$xml2rfc-toc='yes'">
        <link rel="Contents" href="#{$anchor-prefix}.toc" />
      </xsl:if>
      <link rel="Author" href="#{$anchor-prefix}.authors" />
      <xsl:if test="not($xml2rfc-private)">
        <link rel="Copyright" href="#{$anchor-prefix}.copyright" />
      </xsl:if>
      <xsl:if test="//iref">
        <link rel="Index" href="#{$anchor-prefix}.index" />
      </xsl:if>
      <xsl:apply-templates select="/" mode="links" />
      <xsl:for-each select="/rfc/ed:link">
        <link><xsl:copy-of select="@*" /></link>
      </xsl:for-each>
      <xsl:if test="/rfc/@number">
        <link rel="Alternate" title="Authorative ASCII version" href="http://www.ietf.org/rfc/rfc{/rfc/@number}" />
      </xsl:if>

      <!-- generator -->
      <xsl:variable name="gen">
        <xsl:call-template name="get-generator" />
      </xsl:variable>
      <meta name="generator" content="{$gen}" />
      
      <!-- keywords -->
      <xsl:if test="front/keyword">
        <xsl:variable name="keyw">
          <xsl:call-template name="get-keywords" />
        </xsl:variable>
        <meta name="keywords" content="{$keyw}" />
      </xsl:if>

      <xsl:if test="$xml2rfc-ext-support-rfc2731!='no'">
        <!-- Dublin Core Metadata -->
        <link rel="schema.DC" href="http://purl.org/dc/elements/1.1/" />
              
        <!-- DC creator, see RFC2731 -->
        <xsl:for-each select="/rfc/front/author">
          <meta name="DC.Creator" content="{concat(@surname,', ',@initials)}" />
        </xsl:for-each>
        
        <xsl:if test="not($xml2rfc-private)">
          <xsl:choose>
            <xsl:when test="/rfc/@number">
              <meta name="DC.Identifier" content="urn:ietf:rfc:{/rfc/@number}" />
            </xsl:when>
            <xsl:when test="/rfc/@docName">
              <meta name="DC.Identifier" content="urn:ietf:id:{/rfc/@docName}" />
            </xsl:when>
            <xsl:otherwise/>
          </xsl:choose>
          <xsl:variable name="month"><xsl:call-template name="get-month-as-num"/></xsl:variable>
          <meta name="DC.Date.Issued" scheme="ISO8601" content="{/rfc/front/date/@year}-{$month}" />
  
          <xsl:if test="/rfc/@obsoletes!=''">
            <xsl:call-template name="rfclist-for-dcmeta">
              <xsl:with-param name="list" select="/rfc/@obsoletes"/>
            </xsl:call-template>
          </xsl:if>
        </xsl:if>
  
        <xsl:if test="/rfc/front/abstract">
          <meta name="DC.Description.Abstract" content="{normalize-space(/rfc/front/abstract)}" />
        </xsl:if>      
      </xsl:if>      
    </head>
    <body>
      <!-- insert diagnostics -->
      <xsl:call-template name="insert-diagnostics"/>
    
      <xsl:apply-templates select="front" />
      <xsl:apply-templates select="middle" />
      <xsl:apply-templates select="back" />
    </body>
  </html>
</xsl:template>               


<xsl:template match="t">
  <xsl:variable name="paraNumber">
    <xsl:call-template name="sectionnumberPara" />
  </xsl:variable>
     
  <xsl:if test="string-length($paraNumber) &gt; 0">
    <div><a name="{$anchor-prefix}.section.{$paraNumber}" /></div>
  </xsl:if>

  <xsl:apply-templates mode="t-content" select="node()[1]" />
</xsl:template>



<!-- for t-content, dispatch to default templates if it's block-level content -->
<xsl:template mode="t-content" match="list|figure|texttable">
  <!-- <xsl:comment>t-content block-level</xsl:comment>  -->
  <xsl:apply-templates select="." />
  <xsl:apply-templates select="following-sibling::node()[1]" mode="t-content" />
</xsl:template>               
               
<!-- ... otherwise group into p elements -->
<xsl:template mode="t-content" match="*|node()">
  <p>
    <xsl:call-template name="insertInsDelClass"/>
    <xsl:call-template name="editingMark" />
    <xsl:apply-templates mode="t-content2" select="." />
  </p>
  <xsl:apply-templates mode="t-content" select="following-sibling::*[self::list or self::figure or self::texttable][1]" />
</xsl:template>               
               
<xsl:template mode="t-content2" match="*|node()">
  <xsl:apply-templates select="." />
  <xsl:if test="not(following-sibling::node()[1] [self::list or self::figure or self::texttable])">
    <xsl:apply-templates select="following-sibling::node()[1]" mode="t-content2" />
  </xsl:if>
</xsl:template>               

<xsl:template match="section|appendix">

  <xsl:variable name="sectionNumber">
    <xsl:choose>
      <xsl:when test="@myns:unnumbered"></xsl:when>
      <xsl:otherwise><xsl:call-template name="get-section-number" /></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
    
  <xsl:if test="not(ancestor::section) and not(@myns:notoclink)">
    <xsl:call-template name="insertTocLink">
      <xsl:with-param name="rule" select="true()" />
    </xsl:call-template>
  </xsl:if>
  
  <xsl:variable name="elemtype">
    <xsl:choose>
      <xsl:when test="count(ancestor::section) = 0">h1</xsl:when>
      <xsl:when test="count(ancestor::section) = 1">h2</xsl:when>
      <xsl:otherwise>h3</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <xsl:element name="{$elemtype}">
    <xsl:choose>
      <xsl:when test="$sectionNumber='1'">
        <!-- pagebreak, this the first section -->
        <xsl:attribute name="class">np</xsl:attribute>
      </xsl:when>
      <xsl:when test="not(ancestor::section) and not(@myns:notoclink)">
        <xsl:call-template name="insert-conditional-pagebreak"/>
      </xsl:when>
      <xsl:otherwise/>
    </xsl:choose>
    <xsl:call-template name="insertInsDelClass" />
    
    <!-- generate anchors for irefs that are immediate childs of this section -->
    <xsl:apply-templates select="iref"/>
    <xsl:if test="$sectionNumber!=''">
      <a name="{$anchor-prefix}.section.{$sectionNumber}"><xsl:value-of select="$sectionNumber" /></a>&#0160;
    </xsl:if>
    <xsl:choose>
      <xsl:when test="@anchor">
        <a name="{@anchor}"><xsl:value-of select="@title" /></a>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@title" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:element>
  <xsl:apply-templates select="*[not(self::iref)]" />
</xsl:template>

<xsl:template match="spanx[@style='emph' or not(@style)]">
  <em><xsl:apply-templates /></em>
</xsl:template>

<xsl:template match="spanx[@style='verb']">
  <samp><xsl:apply-templates /></samp>
</xsl:template>

<xsl:template match="spanx[@style='strong']">
  <strong><xsl:apply-templates /></strong>
</xsl:template>


<xsl:template match="vspace[not(@blankLines) or @blankLines=0]">
  <br />
</xsl:template>

<xsl:template match="vspace[@blankLines &gt; 0]">
  <br/><xsl:for-each select="//*[position() &lt;= @blankLines]"> <br /></xsl:for-each>
</xsl:template>

<!-- keep the root for the case when we process XSLT-inline markup -->
<xsl:variable name="src" select="/" />

<xsl:template match="xref[node()]">
  <xsl:variable name="target" select="@target" />
  <xsl:variable name="node" select="$src//*[@anchor=$target]" />
  <a href="#{$target}"><xsl:apply-templates /></a>
  <xsl:for-each select="$src/rfc/back/references/reference[@anchor=$target]">
    <xsl:text> </xsl:text><xsl:call-template name="referencename">
       <xsl:with-param name="node" select="." />
    </xsl:call-template>
  </xsl:for-each>
</xsl:template>
               
<xsl:template match="xref[not(node())]">
  <xsl:variable name="context" select="." />
  <xsl:variable name="target" select="@target" />
  <xsl:variable name="node" select="$src//*[@anchor=$target]" />
  <xsl:if test="count($node)=0 and not(ancestor::ed:del)">
    <xsl:message>Undefined target: <xsl:value-of select="@target" /></xsl:message>
    <span class="error">Undefined target: <xsl:value-of select="@target" /></span>
  </xsl:if>
  <a href="#{$target}">
    <xsl:choose>
      <xsl:when test="local-name($node)='section'">
        <xsl:variable name="refname">
          <xsl:for-each select="$node">
            <xsl:call-template name="get-section-type">
              <xsl:with-param name="prec" select="$context/preceding-sibling::node()[1]" />
            </xsl:call-template>
          </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="refnum">
          <xsl:for-each select="$node">
            <xsl:call-template name="get-section-number" />
          </xsl:for-each>
        </xsl:variable>
        <xsl:attribute name="title">
          <xsl:value-of select="$node/@title" />
        </xsl:attribute>
        <xsl:choose>
          <xsl:when test="@format='counter'">
            <xsl:value-of select="$refnum"/>
          </xsl:when>
          <xsl:when test="@format='title'">
            <xsl:value-of select="$node/@title"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="normalize-space(concat($refname,'&#160;',$refnum))"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="local-name($node)='figure'">
        <xsl:variable name="figcnt">
          <xsl:for-each select="$node">
            <xsl:number level="any" count="figure[@title!='' or @anchor!='']" />
          </xsl:for-each>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="@format='counter'">
            <xsl:value-of select="$figcnt" />
          </xsl:when>
          <xsl:when test="@format='title'">
            <xsl:value-of select="$node/@title" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="normalize-space(concat('Figure&#160;',$figcnt))"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="title"><xsl:value-of select="normalize-space($node/front/title)" /></xsl:attribute>
        <xsl:call-template name="referencename"><xsl:with-param name="node" select="$src/rfc/back/references/reference[@anchor=$target]" /></xsl:call-template></xsl:otherwise>
    </xsl:choose>
  </a>
</xsl:template>


<!-- mark unmatched elements red -->

<xsl:template match="*">
     <font color="red"><tt>&lt;<xsl:value-of select="name()" />&gt;</tt></font>
    <xsl:copy><xsl:apply-templates select="node()|@*" /></xsl:copy>
     <font color="red"><tt>&lt;/<xsl:value-of select="name()" />&gt;</tt></font>
</xsl:template>

<xsl:template match="/">
  <xsl:copy><xsl:apply-templates select="node()" /></xsl:copy>
</xsl:template>








<!-- utility templates -->

<xsl:template name="collectLeftHeaderColumn">
  <xsl:param name="mode" />
  <!-- default case -->
  <xsl:if test="not($xml2rfc-private)">
    <myns:item>Network Working Group</myns:item>
    <myns:item>
       <xsl:choose>
        <xsl:when test="/rfc/@ipr and $mode='nroff'">Internet Draft</xsl:when>
        <xsl:when test="/rfc/@ipr">INTERNET DRAFT</xsl:when>
        <xsl:otherwise>Request for Comments: <xsl:value-of select="/rfc/@number"/></xsl:otherwise>
      </xsl:choose>
    </myns:item>
    <xsl:if test="/rfc/@docName and $mode!='nroff'">
      <myns:item>
        &lt;<xsl:value-of select="/rfc/@docName" />&gt;
      </myns:item>
    </xsl:if>
    <xsl:if test="/rfc/@obsoletes and /rfc/@obsoletes!=''">
      <myns:item>
        Obsoletes: <xsl:call-template name="rfclist">
          <xsl:with-param name="list" select="normalize-space(/rfc/@obsoletes)" />
        </xsl:call-template>
        <xsl:if test="not(/rfc/@number)"> (if approved)</xsl:if>
      </myns:item>
    </xsl:if>
    <xsl:if test="/rfc/@seriesNo">
       <myns:item>
        <xsl:choose>
          <xsl:when test="/rfc/@category='bcp'">BCP: <xsl:value-of select="/rfc/@seriesNo" /></xsl:when>
          <xsl:when test="/rfc/@category='info'">FYI: <xsl:value-of select="/rfc/@seriesNo" /></xsl:when>
          <xsl:when test="/rfc/@category='std'">STD: <xsl:value-of select="/rfc/@seriesNo" /></xsl:when>
          <xsl:otherwise><xsl:value-of select="concat(/rfc/@category,': ',/rfc/@seriesNo)" /></xsl:otherwise>
        </xsl:choose>
      </myns:item>
    </xsl:if>
    <xsl:if test="/rfc/@updates and /rfc/@updates!=''">
      <myns:item>
          Updates: <xsl:call-template name="rfclist">
             <xsl:with-param name="list" select="normalize-space(/rfc/@updates)" />
          </xsl:call-template>
          <xsl:if test="not(/rfc/@number)"> (if approved)</xsl:if>
      </myns:item>
    </xsl:if>
    <xsl:if test="$mode!='nroff'">
      <myns:item>
         Category:
        <xsl:call-template name="get-category-long" />
      </myns:item>
    </xsl:if>
    <xsl:if test="/rfc/@ipr">
       <myns:item>Expires: <xsl:call-template name="expirydate" /></myns:item>
    </xsl:if>
  </xsl:if>
    
  <!-- private case -->
  <xsl:if test="$xml2rfc-private">
    <myns:item><xsl:value-of select="$xml2rfc-private" /></myns:item>
  </xsl:if>
</xsl:template>

<xsl:template name="collectRightHeaderColumn">
  <xsl:for-each select="author">
    <xsl:if test="@surname">
      <myns:item>
        <xsl:value-of select="concat(@initials,' ',@surname)" />
        <xsl:if test="@role">
          <xsl:choose>
            <xsl:when test="@role='editor'">
              <xsl:text>, Editor</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>, </xsl:text><xsl:value-of select="@role" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
      </myns:item>
    </xsl:if>
    <xsl:variable name="org">
      <xsl:choose>
        <xsl:when test="organization/@abbrev"><xsl:value-of select="organization/@abbrev" /></xsl:when>
        <xsl:otherwise><xsl:value-of select="organization" /></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="orgOfFollowing">
      <xsl:choose>
        <xsl:when test="following-sibling::*[1]/organization/@abbrev"><xsl:value-of select="following-sibling::*[1]/organization/@abbrev" /></xsl:when>
        <xsl:otherwise><xsl:value-of select="following-sibling::*/organization" /></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="$org != $orgOfFollowing">
      <myns:item><xsl:value-of select="$org" /></myns:item>
    </xsl:if>
  </xsl:for-each>
  <myns:item>
    <xsl:value-of select="concat(date/@month,' ',date/@year)" />
  </myns:item>
</xsl:template>


<xsl:template name="emitheader">
  <xsl:param name="lc" />
  <xsl:param name="rc" />

  <xsl:for-each select="$lc/myns:item | $rc/myns:item">
    <xsl:variable name="pos" select="position()" />
    <xsl:if test="$pos &lt; count($lc/myns:item) + 1 or $pos &lt; count($rc/myns:item) + 1"> 
      <tr>
        <td class="header-l"><xsl:call-template name="copynodes"><xsl:with-param name="nodes" select="$lc/myns:item[$pos]/node()" /></xsl:call-template>&#0160;</td>
        <td class="header-r"><xsl:call-template name="copynodes"><xsl:with-param name="nodes" select="$rc/myns:item[$pos]/node()" /></xsl:call-template>&#0160;</td>
      </tr>
    </xsl:if>
  </xsl:for-each>
</xsl:template>

<!-- convenience template that avoids copying namespace nodes we don't want -->
<xsl:template name="copynodes">
  <xsl:param name="nodes" />
  <xsl:for-each select="$nodes">
    <xsl:choose>
      <xsl:when test="namespace-uri()='http://www.w3.org/1999/xhtml'"><xsl:element name="{name()}" namespace="{namespace-uri()}"><xsl:copy-of select="@*|node()" /></xsl:element></xsl:when>
      <xsl:when test="self::*"><xsl:element name="{name()}"><xsl:copy-of select="@*|node()" /></xsl:element></xsl:when>
      <xsl:otherwise><xsl:copy-of select="." /></xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>
</xsl:template>


<xsl:template name="expirydate">
  <xsl:variable name="date" select="/rfc/front/date" />
  <xsl:choose>
      <xsl:when test="$date/@month='January'">July <xsl:value-of select="$date/@year" /></xsl:when>
      <xsl:when test="$date/@month='February'">August <xsl:value-of select="$date/@year" /></xsl:when>
      <xsl:when test="$date/@month='March'">September <xsl:value-of select="$date/@year" /></xsl:when>
      <xsl:when test="$date/@month='April'">October <xsl:value-of select="$date/@year" /></xsl:when>
      <xsl:when test="$date/@month='May'">November <xsl:value-of select="$date/@year" /></xsl:when>
      <xsl:when test="$date/@month='June'">December <xsl:value-of select="$date/@year" /></xsl:when>
      <xsl:when test="$date/@month='July'">January <xsl:value-of select="$date/@year + 1" /></xsl:when>
      <xsl:when test="$date/@month='August'">February <xsl:value-of select="$date/@year + 1" /></xsl:when>
      <xsl:when test="$date/@month='September'">March <xsl:value-of select="$date/@year + 1" /></xsl:when>
      <xsl:when test="$date/@month='October'">April <xsl:value-of select="$date/@year + 1" /></xsl:when>
      <xsl:when test="$date/@month='November'">May <xsl:value-of select="$date/@year + 1" /></xsl:when>
      <xsl:when test="$date/@month='December'">June <xsl:value-of select="$date/@year + 1" /></xsl:when>
        <xsl:otherwise>WRONG SYNTAX FOR MONTH</xsl:otherwise>
     </xsl:choose>
</xsl:template>

<xsl:template name="get-month-as-num">
  <xsl:variable name="date" select="/rfc/front/date" />
  <xsl:choose>
      <xsl:when test="$date/@month='January'">01</xsl:when>
      <xsl:when test="$date/@month='February'">02</xsl:when>
      <xsl:when test="$date/@month='March'">03</xsl:when>
      <xsl:when test="$date/@month='April'">04</xsl:when>
      <xsl:when test="$date/@month='May'">05</xsl:when>
      <xsl:when test="$date/@month='June'">06</xsl:when>
      <xsl:when test="$date/@month='July'">07</xsl:when>
      <xsl:when test="$date/@month='August'">08</xsl:when>
      <xsl:when test="$date/@month='September'">09</xsl:when>
      <xsl:when test="$date/@month='October'">10</xsl:when>
      <xsl:when test="$date/@month='November'">11</xsl:when>
      <xsl:when test="$date/@month='December'">12</xsl:when>
        <xsl:otherwise>WRONG SYNTAX FOR MONTH</xsl:otherwise>
     </xsl:choose>
</xsl:template>

<!-- produce back section with author information -->
<xsl:template name="insertAuthors">

  <!-- insert link to TOC including horizontal rule -->
  <xsl:call-template name="insertTocLink">
    <xsl:with-param name="rule" select="true()" />
  </xsl:call-template>
    
  <h1>
    <xsl:call-template name="insert-conditional-pagebreak"/>
    <a name="{$anchor-prefix}.authors" />Author's Address<xsl:if test="count(/rfc/front/author) &gt; 1">es</xsl:if>
  </h1>

  <table summary="Authors" width="99%" border="0" cellpadding="0" cellspacing="0">
    <xsl:apply-templates select="/rfc/front/author" />
  </table>
</xsl:template>



<!-- insert copyright statement -->

<xsl:template name="insertCopyright" xmlns="">

  <section title="Intellectual Property Statement" anchor="{$anchor-prefix}.ipr" myns:unnumbered="unnumbered" myns:is-rfc2629="true">
    <xsl:choose>
      <xsl:when test="$ipr-rfc3667">
        <t myns:is-rfc2629="true">
          The IETF takes no position regarding the validity or scope of any
          Intellectual Property Rights or other rights that might be claimed to
          pertain to the implementation or use of the technology described in
          this document or the extent to which any license under such rights
          might or might not be available; nor does it represent that it has
          made any independent effort to identify any such rights. Information
          on the IETF's procedures with respect to rights in IETF Documents
          can be found in BCP 78 and BCP 79.
        </t>       
        <t myns:is-rfc2629="true">
          Copies of IPR disclosures made to the IETF Secretariat and any
          assurances of licenses to be made available, or the result of an
          attempt made to obtain a general license or permission for the use
          of such proprietary rights by implementers or users of this
          specification can be obtained from the IETF on-line IPR repository 
          at <eref target="http://www.ietf.org/ipr"/>.
        </t>       
        <t myns:is-rfc2629="true">
          The IETF invites any interested party to bring to its attention any
          copyrights, patents or patent applications, or other proprietary
          rights that may cover technology that may be required to implement
          this standard. Please address the information to the IETF at
          <eref target="mailto:ietf-ipr@ietf.org">ietf-ipr@ietf.org</eref>.
        </t>       
      </xsl:when>
      <xsl:otherwise>
        <t myns:is-rfc2629="true">
          The IETF takes no position regarding the validity or scope of
          any intellectual property or other rights that might be claimed
          to  pertain to the implementation or use of the technology
          described in this document or the extent to which any license
          under such rights might or might not be available; neither does
          it represent that it has made any effort to identify any such
          rights. Information on the IETF's procedures with respect to
          rights in standards-track and standards-related documentation
          can be found in BCP-11. Copies of claims of rights made
          available for publication and any assurances of licenses to
          be made available, or the result of an attempt made
          to obtain a general license or permission for the use of such
          proprietary rights by implementors or users of this
          specification can be obtained from the IETF Secretariat.
        </t>
        <t myns:is-rfc2629="true">
          The IETF invites any interested party to bring to its
          attention any copyrights, patents or patent applications, or
          other proprietary rights which may cover technology that may be
          required to practice this standard. Please address the
          information to the IETF Executive Director.
        </t>
        <xsl:if test="$xml2rfc-iprnotified='yes'">
          <t myns:is-rfc2629="true">
            The IETF has been notified of intellectual property rights
            claimed in regard to some or all of the specification contained
            in this document. For more information consult the online list
            of claimed rights.
          </t>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </section>
  
  <xsl:if test="$ipr-rfc3667">
    <section title="Disclaimer of Validity" anchor="{$anchor-prefix}.disclaimer" myns:unnumbered="unnumbered" myns:notoclink="notoclink" myns:is-rfc2629="true">
      <t myns:is-rfc2629="true">
        This document and the information contained herein are provided on an
        "AS IS" basis and THE CONTRIBUTOR, THE ORGANIZATION HE/SHE REPRESENTS
        OR IS SPONSORED BY (IF ANY), THE INTERNET SOCIETY AND THE INTERNET
        ENGINEERING TASK FORCE DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED,
        INCLUDING BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE 
        INFORMATION HEREIN WILL NOT INFRINGE ANY RIGHTS OR ANY IMPLIED 
        WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
      </t>
    </section>
  </xsl:if>

  <xsl:choose>
    <xsl:when test="$ipr-rfc3667">
      <section title="Copyright Statement" anchor="{$anchor-prefix}.copyright" myns:unnumbered="unnumbered" myns:notoclink="notoclink" myns:is-rfc2629="true">
        <t myns:is-rfc2629="true">
          Copyright (C) The Internet Society (<xsl:value-of select="/rfc/front/date/@year" />).
          This document is subject to the rights, licenses and restrictions
          contained in BCP 78, and except as set forth therein, the authors
          retain all their rights.
        </t>
      </section>    
    </xsl:when>
    <xsl:otherwise>
      <section title="Full Copyright Statement" anchor="{$anchor-prefix}.copyright" myns:unnumbered="unnumbered" myns:notoclink="notoclink" myns:is-rfc2629="true">
        <t myns:is-rfc2629="true">
          Copyright (C) The Internet Society (<xsl:value-of select="/rfc/front/date/@year" />). All Rights Reserved.
        </t>
        <t myns:is-rfc2629="true">
          This document and translations of it may be copied and furnished to
          others, and derivative works that comment on or otherwise explain it
          or assist in its implementation may be prepared, copied, published and
          distributed, in whole or in part, without restriction of any kind,
          provided that the above copyright notice and this paragraph are
          included on all such copies and derivative works. However, this
          document itself may not be modified in any way, such as by removing
          the copyright notice or references to the Internet Society or other
          Internet organizations, except as needed for the purpose of
          developing Internet standards in which case the procedures for
          copyrights defined in the Internet Standards process must be
          followed, or as required to translate it into languages other than
          English.
        </t>
        <t myns:is-rfc2629="true">
          The limited permissions granted above are perpetual and will not be
          revoked by the Internet Society or its successors or assignees.
        </t>
        <t myns:is-rfc2629="true">
          This document and the information contained herein is provided on an
          &quot;AS IS&quot; basis and THE INTERNET SOCIETY AND THE INTERNET ENGINEERING
          TASK FORCE DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
          BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION
          HEREIN WILL NOT INFRINGE ANY RIGHTS OR ANY IMPLIED WARRANTIES OF
          MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
        </t>
      </section>
    </xsl:otherwise>
  </xsl:choose>
  
  <section title="Acknowledgement" myns:unnumbered="unnumbered" myns:notoclink="notoclink" myns:is-rfc2629="true">
    <t myns:is-rfc2629="true">
      Funding for the RFC Editor function is currently provided by the
      Internet Society.
    </t>
  </section>

</xsl:template>


<!-- insert CSS style info -->

<xsl:template name="insertCss">
a {
  text-decoration: none
}
a:hover {
  text-decoration: underline
}
a:active {
  text-decoration: underline
}
body {
  <xsl:if test="$xml2rfc-background!=''">
  background: url(<xsl:value-of select="$xml2rfc-background" />) #ffffff left top;
  </xsl:if>
  color: #000000;
  font-family: helvetica, arial, sans-serif;
  font-size: 13px;
}
dl {
  margin-left: 2em;
}
h1 {
  color: #333333;
  font-size: 16px;
  line-height: 16px;
  font-family: helvetica, arial, sans-serif;
  page-break-after: avoid;
}
h1.np {
  page-break-before: always;
}
h2 {
  color: #000000;
  font-size: 14px;
  font-family: helvetica, arial, sans-serif;
  page-break-after: avoid;
}
h3 {
  color: #000000;
  font-size: 13px;
  font-family: helvetica, arial, sans-serif;
  page-break-after: avoid;
}
img {
  margin-left: 3em;
}
li {
  margin-left: 2em;
  margin-right: 2em;
}
ol {
  margin-left: 2em;
  margin-right: 2em;
}
p {
  margin-left: 2em;
  margin-right: 2em;
}
pre {
  margin-left: 3em;
  background-color: lightyellow;
}
table {
  margin-left: 2em;
}
table.header {
  width: 66%;
}
td.top {
  vertical-align: top;
}
td.topnowrap {
  vertical-align: top;
  white-space: nowrap; 
}
td.right {
  text-align: right;
}
td.header-l {
  width: 33%;
  color: #ffffff;
  background-color: #666666;
  font-size: 10px;
  font-family: arial, helvetica, sans-serif;
  vertical-align: top
}
td.header-r {
  width: 33%;
  color: #ffffff;
  background-color: #666666;
  font-size: 10px;
  font-family: arial, helvetica, sans-serif;
  vertical-align: top;
}
thead {
  display:table-header-group
}
.editingmark {
  background-color: khaki;
}
.error {
  font-size: 14pt;
  background-color: red;
}
.hotText {
  color:#ffffff;
  font-weight: normal;
  text-decoration: none;
  font-family: chelvetica, arial, sans-serif;
  font-size: 9px
}
.link2 {
  color:#ffffff;
  font-weight: bold;
  text-decoration: none;
  font-family: helvetica, arial, sans-serif;
  font-size: 9px
}
.toowide {
  color: red;
  font-weight: bold;
}
.RFC {
  color:#666666;
  font-weight: bold;
  text-decoration: none;
  font-family: helvetica, arial, sans-serif;
  font-size: 9px
}
.title {
  color: #990000;
  font-size: 22px;
  line-height: 22px;
  font-weight: bold;
  text-align: right;
  font-family: helvetica, arial, sans-serif
}
.figure {
  font-weight: bold;
  text-align: center;
  font-size: 12px;
}
.filename {
  color: #333333;
  font-weight: bold;
  font-size: 16px;
  line-height: 24px;
  font-family: helvetica, arial, sans-serif;
  text-align: right;
}
.warning {
  font-size: 14pt;
  background-color: yellow;
}
del {
  color: red;
  text-decoration: line-through;
}
.del {
  color: red;
  text-decoration: line-through;
}
ins {
  color: green;
  text-decoration: underline;
}
.ins {
  color: green;
  text-decoration: underline;
}

table.openissue {
  background-color: khaki;
  border-width: thin;
  border-style: solid;
  border-color: black;
}

table.closedissue {
  background-color: white;
  border-width: thin;
  border-style: solid;
  border-color: gray;
  color: gray; 
}

.closed-issue {
  border: solid;
  border-width: thin;
  background-color: lime;
  font-size: small;
  font-weight: bold;
}

.open-issue {
  border: solid;
  border-width: thin;
  background-color: red;
  font-size: small;
  font-weight: bold;
}

.editor-issue {
  border: solid;
  border-width: thin;
  background-color: yellow;
  font-size: small;
  font-weight: bold;
}

@media print {
  .noprint {
    display: none;
  }
}
</xsl:template>


<!-- generate the index section -->

<xsl:template name="insertSingleIref">
  <xsl:variable name="backlink">#<xsl:value-of select="$anchor-prefix"/>.iref.<xsl:number level="any" /></xsl:variable>
  &#0160;<a href="{$backlink}"><xsl:choose>
      <xsl:when test="@primary='true'"><b><xsl:call-template name="get-section-number" /></b></xsl:when>
      <xsl:otherwise><xsl:call-template name="get-section-number" /></xsl:otherwise>
    </xsl:choose>
  </a><xsl:if test="position()!=last()">, </xsl:if>
</xsl:template>


<xsl:template name="insertIndex">

  <!-- insert link to TOC including horizontal rule -->
  <xsl:call-template name="insertTocLink">
    <xsl:with-param name="rule" select="true()" />
  </xsl:call-template> 

  <h1>
    <xsl:call-template name="insert-conditional-pagebreak"/>
    <a name="{$anchor-prefix}.index" />Index
  </h1>

  <table summary="Index">

    <xsl:for-each select="//iref[generate-id(.) = generate-id(key('index-first-letter',translate(substring(@item,1,1),$lcase,$ucase)))]">
      <xsl:sort select="translate(@item,$lcase,$ucase)" />
            
      <tr>
        <td>
          <b><xsl:value-of select="translate(substring(@item,1,1),$lcase,$ucase)" /></b>
        </td>
      </tr>
            
      <xsl:for-each select="key('index-first-letter',translate(substring(@item,1,1),$lcase,$ucase))">
    
        <xsl:sort select="translate(@item,$lcase,$ucase)" />
         
        <xsl:if test="generate-id(.) = generate-id(key('index-item',@item))">
    
          <tr>
            <td>
              &#0160;&#0160;<xsl:value-of select="@item" />&#0160;
                
              <xsl:for-each select="key('index-item',@item)[not(@subitem) or @subitem='']">
                <xsl:sort select="translate(@item,$lcase,$ucase)" />
                <xsl:call-template name="insertSingleIref" />
              </xsl:for-each>
            </td>
          </tr>
                
          <xsl:for-each select="key('index-item',@item)[@subitem and @subitem!='']">
            <xsl:sort select="translate(@subitem,$lcase,$ucase)" />
            
             <xsl:if test="generate-id(.) = generate-id(key('index-item-subitem',concat(@item,'..',@subitem)))">
            <tr>
              <td>
                &#0160;&#0160;&#0160;&#0160;<xsl:value-of select="@subitem" />&#0160;
                  
                <xsl:for-each select="key('index-item-subitem',concat(@item,'..',@subitem))">
                  <xsl:sort select="translate(@item,$lcase,$ucase)" />                    
                  <xsl:call-template name="insertSingleIref" />
                </xsl:for-each>
              </td>
            </tr>
            </xsl:if>
          </xsl:for-each>
                
        </xsl:if>
                
      </xsl:for-each>            

    </xsl:for-each>
  </table>
</xsl:template>




<xsl:template name="insertPreamble" xmlns="">

  <section title="Status of this Memo" myns:unnumbered="unnumbered" myns:notoclink="notoclink" anchor="{$anchor-prefix}.status" myns:is-rfc2629="true">

  <xsl:choose>
    <xsl:when test="/rfc/@ipr">
      <t myns:is-rfc2629="true">
        <xsl:choose>
          
          <!-- RFC2026 -->
          <xsl:when test="/rfc/@ipr = 'full2026'">
            This document is an Internet-Draft and is 
            in full conformance with all provisions of Section 10 of RFC2026.    
          </xsl:when>
          <xsl:when test="/rfc/@ipr = 'noDerivativeWorks2026'">
            This document is an Internet-Draft and is 
            in full conformance with all provisions of Section 10 of RFC2026
            except that the right to produce derivative works is not granted.   
          </xsl:when>
          <xsl:when test="/rfc/@ipr = 'noDerivativeWorksNow'">
            This document is an Internet-Draft and is 
            in full conformance with all provisions of Section 10 of RFC2026
            except that the right to produce derivative works is not granted.
            (If this document becomes part of an IETF working group activity,
            then it will be brought into full compliance with Section 10 of RFC2026.)  
          </xsl:when>
          <xsl:when test="/rfc/@ipr = 'none'">
            This document is an Internet-Draft and is 
            NOT offered in accordance with Section 10 of RFC2026,
            and the author does not provide the IETF with any rights other
            than to publish as an Internet-Draft.
          </xsl:when>
          
          <!-- RFC3667 -->
          <xsl:when test="/rfc/@ipr = 'full3667'">
            By submitting this Internet-Draft, I certify that any applicable
            patent or other IPR claims of which I am aware have been disclosed,
            and any of which I become aware will be disclosed, in accordance
            with RFC 3668.
          </xsl:when>
          <xsl:when test="/rfc/@ipr = 'noModification3667'">
            By submitting this Internet-Draft, I certify that any applicable
            patent or other IPR claims of which I am aware have been disclosed,
            and any of which I become aware will be disclosed, in accordance
            with RFC 3668. This document may not be modified, and derivative
            works of it may not be created, except to publish it as an RFC and
            to translate it into languages other than English<xsl:if test="/rfc/@iprExtract">,
            other than to extract <xref target="{/rfc/@iprExtract}"/> as-is for separate use.</xsl:if>.
          </xsl:when>
          <xsl:when test="/rfc/@ipr = 'noDerivatives3667'">
            By submitting this Internet-Draft, I certify that any applicable
            patent or other IPR claims of which I am aware have been disclosed,
            and any of which I become aware will be disclosed, in accordance
            with RFC 3668. This document may not be modified, and derivative
            works of it may not be created<xsl:if test="/rfc/@iprExtract">,
            other than to extract <xref target="{/rfc/@iprExtract}"/> as-is for separate use.</xsl:if>..
          </xsl:when>
          
          <xsl:otherwise>CONFORMANCE UNDEFINED.</xsl:otherwise>
        </xsl:choose>
      </t>
      <t myns:is-rfc2629="true">
        Internet-Drafts are working documents of the Internet Engineering
        Task Force (IETF), its areas, and its working groups.
        Note that other groups may also distribute working documents as
        Internet-Drafts.
      </t>
      <t myns:is-rfc2629="true">
        Internet-Drafts are draft documents valid for a maximum of six months
        and may be updated, replaced, or obsoleted by other documents at any time.
        It is inappropriate to use Internet-Drafts as reference material or to cite
        them other than as "work in progress".
      </t>
      <t myns:is-rfc2629="true">
        The list of current Internet-Drafts can be accessed at
        <eref target='http://www.ietf.org/ietf/1id-abstracts.txt' myns:is-rfc2629="true" />.
      </t>
      <t myns:is-rfc2629="true">
        The list of Internet-Draft Shadow Directories can be accessed at
        <eref target='http://www.ietf.org/shadow.html' myns:is-rfc2629="true"/>.
      </t>
      <t myns:is-rfc2629="true">
        This Internet-Draft will expire in <xsl:call-template name="expirydate" />.
      </t>
    </xsl:when>

    <xsl:when test="/rfc/@category='bcp'">
      <t myns:is-rfc2629="true">
        This document specifies an Internet Best Current Practices for the Internet
        Community, and requests discussion and suggestions for improvements.
        Distribution of this memo is unlimited.
      </t>
    </xsl:when>
    <xsl:when test="/rfc/@category='exp'">
      <t myns:is-rfc2629="true">
        This memo defines an Experimental Protocol for the Internet community.
        It does not specify an Internet standard of any kind.
        Discussion and suggestions for improvement are requested.
        Distribution of this memo is unlimited.
      </t>
    </xsl:when>
    <xsl:when test="/rfc/@category='historic'">
      <t myns:is-rfc2629="true">
        This memo describes a historic protocol for the Internet community.
        It does not specify an Internet standard of any kind.
        Distribution of this memo is unlimited.
      </t>
    </xsl:when>
    <xsl:when test="/rfc/@category='info' or not(/rfc/@category)">
      <t myns:is-rfc2629="true">
        This memo provides information for the Internet community.
        It does not specify an Internet standard of any kind.
        Distribution of this memo is unlimited.
      </t>
    </xsl:when>
    <xsl:when test="/rfc/@category='std'">
      <t myns:is-rfc2629="true">
        This document specifies an Internet standards track protocol for the Internet
        community, and requests discussion and suggestions for improvements.
        Please refer to the current edition of the &quot;Internet Official Protocol
        Standards&quot; (STD 1) for the standardization state and status of this
        protocol. Distribution of this memo is unlimited.
      </t>
    </xsl:when>
    <xsl:otherwise>
      <t myns:is-rfc2629="true">UNSUPPORTED CATEGORY.</t>
    </xsl:otherwise>
  </xsl:choose>
  
  </section>

  <section title="Copyright Notice" myns:unnumbered="unnumbered" myns:notoclink="notoclink" anchor="{$anchor-prefix}.copyrightnotice" myns:is-rfc2629="true">
  <t myns:is-rfc2629="true">
    Copyright (C) The Internet Society (<xsl:value-of select="/rfc/front/date/@year" />). All Rights Reserved.
  </t>
  </section>
  
</xsl:template>

<!-- TOC generation -->

<xsl:template match="/" mode="toc">
  <xsl:call-template name="insertTocLink">
    <xsl:with-param name="includeTitle" select="true()" />
      <xsl:with-param name="rule" select="true()" />
  </xsl:call-template>

  <h1 class="np"> <!-- this pagebreak occurs always -->
    <a name="{$anchor-prefix}.toc">Table of Contents</a>
  </h1>

  <p>
    <xsl:apply-templates mode="toc" />
  </p>
</xsl:template>

<xsl:template name="insertTocLine">
  <xsl:param name="number" />
  <xsl:param name="target" />
  <xsl:param name="title" />

  <!-- handle tocdepth parameter -->
  <xsl:choose>  
    <xsl:when test="string-length(translate($number,'.ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890&#167;','.')) &gt;= $parsedTocDepth">
      <!-- dropped entry -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="starts-with($number,'del-')">
          <xsl:value-of select="'&#160;&#160;&#160;&#160;&#160;&#160;'"/>
          <del>
            <xsl:value-of select="$number" />&#0160;
            <a href="#{$target}"><xsl:value-of select="$title"/></a>
          </del>
        </xsl:when>
        <xsl:when test="$number=''">
          <b>
            &#0160;&#0160;
            <a href="#{$target}"><xsl:value-of select="$title"/></a>
          </b>
        </xsl:when>
        <xsl:otherwise>
          <b>
            <xsl:value-of select="translate($number,'.ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890&#167;','&#160;')"/>
            <xsl:value-of select="$number" />&#0160;
            <a href="#{$target}"><xsl:value-of select="$title"/></a>
          </b>
        </xsl:otherwise>
      </xsl:choose>
      <br />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<xsl:template match="back" mode="toc">

  <xsl:apply-templates select="references" mode="toc" />
  <xsl:apply-templates select="/rfc/front" mode="toc" />
  <xsl:apply-templates select="*[not(self::references)]" mode="toc" />

  <!-- copyright statements -->
  <xsl:if test="not($xml2rfc-private)">
    <xsl:call-template name="insertTocLine">
      <xsl:with-param name="number" select="'&#167;'"/>
      <xsl:with-param name="target" select="concat($anchor-prefix,'.ipr')"/>
      <xsl:with-param name="title" select="'Intellectual Property and Copyright Statements'"/>
    </xsl:call-template>
  </xsl:if>
  
  <!-- insert the index if index entries exist -->
  <xsl:if test="//iref">
    <xsl:call-template name="insertTocLine">
      <xsl:with-param name="number" select="'&#167;'"/>
      <xsl:with-param name="target" select="concat($anchor-prefix,'.index')"/>
      <xsl:with-param name="title" select="'Index'"/>
    </xsl:call-template>
  </xsl:if>

</xsl:template>

<xsl:template match="front" mode="toc">

  <xsl:variable name="title">
    <xsl:if test="count(author)=1">Author's Address</xsl:if>
    <xsl:if test="count(author)!=1">Author's Addresses</xsl:if>
  </xsl:variable>

  <xsl:call-template name="insertTocLine">
    <xsl:with-param name="number" select="'&#167;'"/>
    <xsl:with-param name="target" select="concat($anchor-prefix,'.authors')"/>
    <xsl:with-param name="title" select="$title"/>
  </xsl:call-template>

</xsl:template>

<xsl:template match="references" mode="toc">

  <xsl:variable name="num">
    <xsl:choose>
      <xsl:when test="not(preceding::references)" />
      <xsl:otherwise>
        <xsl:text>.</xsl:text><xsl:number/>      
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="title">
    <xsl:choose>
      <xsl:when test="@title!=''"><xsl:value-of select="@title" /></xsl:when>
      <xsl:otherwise>References</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:call-template name="insertTocLine">
    <xsl:with-param name="number" select="'&#167;'"/>
    <xsl:with-param name="target" select="concat($anchor-prefix,'.references',$num)"/>
    <xsl:with-param name="title" select="$title"/>
  </xsl:call-template>

</xsl:template>

<xsl:template match="section" mode="toc">
  <xsl:variable name="sectionNumber">
    <xsl:call-template name="get-section-number" />
  </xsl:variable>

  <xsl:variable name="target">
    <xsl:choose>
      <xsl:when test="@anchor"><xsl:value-of select="@anchor" /></xsl:when>
       <xsl:otherwise><xsl:value-of select="$anchor-prefix"/>.section.<xsl:value-of select="$sectionNumber" /></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:call-template name="insertTocLine">
    <xsl:with-param name="number" select="$sectionNumber"/>
    <xsl:with-param name="target" select="$target"/>
    <xsl:with-param name="title" select="@title"/>
  </xsl:call-template>

  <xsl:apply-templates mode="toc" />
</xsl:template>

<xsl:template match="middle" mode="toc">
  <xsl:apply-templates mode="toc" />
</xsl:template>

<xsl:template match="rfc" mode="toc">
  <xsl:apply-templates select="middle|back" mode="toc" />
</xsl:template>

<xsl:template match="ed:del|ed:ins|ed:replace" mode="toc">
  <xsl:apply-templates mode="toc" />
</xsl:template>

<xsl:template match="*" mode="toc" />


<xsl:template name="insertTocAppendix">
  
  <xsl:if test="//figure[@title!='' or @anchor!='']">
    <p>
      <xsl:for-each select="//figure[@title!='' or @anchor!='']">
        <xsl:variable name="title">Figure <xsl:value-of select="position()"/><xsl:if test="@title">: <xsl:value-of select="@title"/></xsl:if>
        </xsl:variable>
        <xsl:call-template name="insertTocLine">
          <xsl:with-param name="target" select="concat($anchor-prefix,'.figure.',position())" />
          <xsl:with-param name="title" select="$title" />
        </xsl:call-template>
      </xsl:for-each>
    </p>
  </xsl:if>
  
  <!-- experimental -->
  <xsl:if test="//ed:issue">
    <xsl:call-template name="insertIssuesList" />
  </xsl:if>

</xsl:template>

<xsl:template name="insertTocLink">
  <xsl:param name="includeTitle" select="false()" />
  <xsl:param name="rule" />
  <xsl:if test="$rule"><hr class="noprint"/></xsl:if>
  <xsl:if test="$includeTitle or $xml2rfc-toc='yes'">
    <table summary="link to TOC" class="noprint" style="margin-left: auto; margin-right: 0; float: right; width: 2.5em;">
      <xsl:if test="$includeTitle">
        <tr>
          <td style="background-color: #000000; text-align: center; vertical-align: middle; height: 2.5em;">
            <b><span class="RFC">&#0160;RFC&#0160;</span></b>
            <xsl:if test="/rfc/@number">
              <br />
              <span class="hotText"><xsl:value-of select="/rfc/@number"/></span>
            </xsl:if>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="$xml2rfc-toc='yes'">
        <tr>
          <td style="background-color: #990000; text-align: center; height: 1.5em;">
            <a href="#{$anchor-prefix}.toc"><b class="link2">&#0160;TOC&#0160;</b></a>
          </td>
        </tr>
      </xsl:if>
    </table>
  </xsl:if>
</xsl:template>


<xsl:template name="referencename">
  <xsl:param name="node" />
  <xsl:choose>
    <xsl:when test="$xml2rfc-symrefs='yes'">[<xsl:value-of select="$node/@anchor" />]</xsl:when>
    <xsl:otherwise><xsl:for-each select="$node">[<xsl:number level="any" />]</xsl:for-each></xsl:otherwise>
  </xsl:choose>
</xsl:template>



<xsl:template name="replace-substring">

  <xsl:param name="string" />
  <xsl:param name="replace" />
  <xsl:param name="by" />

  <xsl:choose>
    <xsl:when test="contains($string,$replace)">
      <xsl:value-of select="concat(substring-before($string, $replace),$by)" />
      <xsl:call-template name="replace-substring">
        <xsl:with-param name="string" select="substring-after($string,$replace)" />
        <xsl:with-param name="replace" select="$replace" />
        <xsl:with-param name="by" select="$by" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise><xsl:value-of select="$string" /></xsl:otherwise>
  </xsl:choose>

</xsl:template>

<xsl:template name="showArtworkLine">
  <xsl:param name="line" />
  <xsl:param name="mode" />
  
  <xsl:variable name="maxw" select="69" />
  
  <xsl:if test="string-length($line) &gt; $maxw">
    <xsl:message>Artwork exceeds maximum width: <xsl:value-of select="$line" /></xsl:message>
  </xsl:if>
  
  <xsl:choose>
    <xsl:when test="$mode='html'">
      <xsl:value-of select="substring($line,0,$maxw)" />
      <xsl:if test="string-length($line) &gt;= $maxw">
        <span class="toowide"><xsl:value-of select="substring($line,$maxw)" /></span>
      </xsl:if>
      <xsl:text>&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="$mode='wordml'">
      <r xmlns="http://schemas.microsoft.com/office/word/2003/wordml">
        <t><xsl:value-of select="translate($line,' ','&#160;')"/></t>
      </r>
    </xsl:when>
    <xsl:when test="$mode='nroff'">
      <xsl:variable name="cline">
        <xsl:call-template name="replace-substring">
          <xsl:with-param name="string" select="$line" />
          <xsl:with-param name="replace" select="'\'" />
          <xsl:with-param name="by" select="'\\'" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:value-of select="concat($cline,'&#10;')" />
    </xsl:when>
    <xsl:otherwise><xsl:value-of select="concat($line,'&#10;')" /></xsl:otherwise>
  </xsl:choose>
  
</xsl:template>

<xsl:template name="showArtwork">
  <xsl:param name="mode" />
  <xsl:param name="text" />
  <xsl:param name="initial" />
  <xsl:variable name="delim" select="'&#10;'" />
  <xsl:variable name="first" select="substring-before($text,$delim)" />
  <xsl:variable name="remainder" select="substring-after($text,$delim)" />
  
  <xsl:choose>
    <xsl:when test="not(contains($text,$delim))">
      <xsl:call-template name="showArtworkLine">
        <xsl:with-param name="line" select="$text" />
        <xsl:with-param name="mode" select="$mode" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <!-- suppress empty initial lines -->
      <xsl:if test="$initial!='yes' or normalize-space($first)!=''">
        <xsl:call-template name="showArtworkLine">
          <xsl:with-param name="line" select="$first" />
          <xsl:with-param name="mode" select="$mode" />
        </xsl:call-template>
        <xsl:if test="$mode='wordml' and $remainder!=''">
          <r xmlns="http://schemas.microsoft.com/office/word/2003/wordml">
            <br />
          </r>
        </xsl:if>
      </xsl:if>
      <xsl:if test="$remainder!=''">
        <xsl:call-template name="showArtwork">
          <xsl:with-param name="text" select="$remainder" />
          <xsl:with-param name="mode" select="$mode" />
        </xsl:call-template>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>
  
</xsl:template>


<!--<xsl:template name="dump">
  <xsl:param name="text" />
  <xsl:variable name="c" select="substring($text,1,1)"/>
  <xsl:choose>
    <xsl:when test="$c='&#9;'">&amp;#9;</xsl:when>
    <xsl:when test="$c='&#10;'">&amp;#10;</xsl:when>
    <xsl:when test="$c='&#13;'">&amp;#13;</xsl:when>
    <xsl:when test="$c='&amp;'">&amp;amp;</xsl:when>
    <xsl:otherwise><xsl:value-of select="$c" /></xsl:otherwise>
  </xsl:choose>
  <xsl:if test="string-length($text) &gt; 1">
    <xsl:call-template name="dump">
      <xsl:with-param name="text" select="substring($text,2)" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>-->


<xsl:template name="rfclist">
  <xsl:param name="list" />
  <xsl:choose>
    <xsl:when test="contains($list,',')">
      <xsl:variable name="rfcNo" select="substring-before($list,',')" />
      <a href="{concat($rfcUrlPrefix,$rfcNo,'.txt')}"><xsl:value-of select="$rfcNo" /></a>,
      <xsl:call-template name="rfclist">
        <xsl:with-param name="list" select="normalize-space(substring-after($list,','))" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="rfcNo" select="$list" />
      <a href="{concat($rfcUrlPrefix,$rfcNo,'.txt')}"><xsl:value-of select="$rfcNo" /></a>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="rfclist-for-dcmeta">
  <xsl:param name="list" />
  <xsl:choose>
    <xsl:when test="contains($list,',')">
      <xsl:variable name="rfcNo" select="substring-before($list,',')" />
      <meta name="DC.Relation.Replaces" content="urn:ietf:rfc:{$rfcNo}" />
      <xsl:call-template name="rfclist-for-dcmeta">
        <xsl:with-param name="list" select="normalize-space(substring-after($list,','))" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="rfcNo" select="$list" />
      <meta name="DC.Relation.Replaces" content="urn:ietf:rfc:{$rfcNo}" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="sectionnumberPara">
  <!-- get section number of ancestor section element, then add t or figure number -->
  <xsl:if test="ancestor::section and not(ancestor::section[@myns:unnumbered='unnumbered'])">
    <xsl:for-each select="ancestor::section[1]"><xsl:call-template name="get-section-number" />.p.</xsl:for-each><xsl:number count="t|figure" />
  </xsl:if>
</xsl:template>

<xsl:template name="editingMark">
  <xsl:if test="$xml2rfc-editing='yes' and ancestor::rfc">
    <sup class="editingmark"><span><xsl:number level="any" count="postamble|preamble|t"/></span>&#0160;</sup>
  </xsl:if>
</xsl:template>

<!-- experimental annotation support -->

<xsl:template match="ed:issue">
  <xsl:variable name="class">
    <xsl:choose>
      <xsl:when test="@status='closed'">closedissue</xsl:when>
      <xsl:otherwise>openissue</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <table summary="issue {@name}" class="{$class}">
    <tr>
      <td colspan="3">
        <a name="{$anchor-prefix}.issue.{@name}">
          <xsl:choose>
            <xsl:when test="@status='closed'">
              <xsl:attribute name="class">closed-issue</xsl:attribute>
            </xsl:when>
            <xsl:when test="@status='editor'">
              <xsl:attribute name="class">editor-issue</xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="class">open-issue</xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text>&#160;i&#160;</xsl:text>
        </a>
        <xsl:text>&#160;</xsl:text>
        <xsl:choose>
          <xsl:when test="@href">
            <em><a href="{@href}"><xsl:value-of select="@name" /></a></em>
          </xsl:when>
          <xsl:otherwise>
            <em><xsl:value-of select="@name" /></em>
          </xsl:otherwise>
        </xsl:choose>
        &#0160;
        (type: <xsl:value-of select="@type"/>, status: <xsl:value-of select="@status"/>)
      </td>
    </tr>
    <xsl:for-each select="ed:item">
      <tr>
        <td class="top">
          <a href="mailto:{@entered-by}?subject={/rfc/@docName}, {../@name}"><i><xsl:value-of select="@entered-by"/></i></a>
        </td>
        <td class="topnowrap">
          <xsl:value-of select="@date"/>
        </td>
        <td class="top">
          <xsl:call-template name="copynodes">
            <xsl:with-param name="nodes" select="node()" />
          </xsl:call-template>
        </td>
      </tr>
    </xsl:for-each>
    <xsl:for-each select="ed:resolution">
      <tr>
        <td class="top">
          <xsl:if test="@entered-by">
            <a href="mailto:{@entered-by}?subject={/rfc/@docName}, {../@name}"><i><xsl:value-of select="@entered-by"/></i></a>
          </xsl:if>
        </td>
        <td class="topnowrap">
          <xsl:value-of select="@datetime"/>
        </td>
        <td class="top">
          <em>Resolution:</em>&#0160;<xsl:copy-of select="node()" />
        </td>
      </tr>
    </xsl:for-each>      
  </table>
    
</xsl:template>

<xsl:template name="insertIssuesList">

  <h2><a name="{$anchor-prefix}.issues-list">Issues list</a></h2>
  <table summary="Issues list">
    <xsl:for-each select="//ed:issue">
      <xsl:sort select="@status" />
      <xsl:sort select="@name" />
      <tr>
        <td><a href="#{$anchor-prefix}.issue.{@name}"><xsl:value-of select="@name" /></a></td>
        <td><xsl:value-of select="@type" /></td>
        <td><xsl:value-of select="@status" /></td>
        <td><xsl:value-of select="ed:item[1]/@date" /></td>
        <td><a href="mailto:{ed:item[1]/@entered-by}?subject={/rfc/@docName}, {@name}"><xsl:value-of select="ed:item[1]/@entered-by" /></a></td>
      </tr>
    </xsl:for-each>
  </table>
  
</xsl:template>

<xsl:template name="formatTitle">
  <xsl:if test="@who">
    <xsl:value-of select="@who" />
  </xsl:if>
  <xsl:if test="@datetime">
    <xsl:value-of select="concat(' (',@datetime,')')" />
  </xsl:if>
  <xsl:if test="@reason">
    <xsl:value-of select="concat(': ',@reason)" />
  </xsl:if>
  <xsl:if test="@cite">
    <xsl:value-of select="concat(' &lt;',@cite,'&gt;')" />
  </xsl:if>
</xsl:template>

<xsl:template name="insert-diagnostics">
  
  <!-- check anchor names -->
  <xsl:variable name="badAnchors" select="//*[starts-with(@anchor,concat($anchor-prefix,'.'))]" />
  <xsl:if test="$badAnchors">
    <p class="warning">
      The following anchor names may collide with internally generated anchors because of their prefix "<xsl:value-of select="$anchor-prefix" />":
      <xsl:for-each select="$badAnchors">
        <xsl:value-of select="@anchor"/><xsl:if test="position()!=last()">, </xsl:if>
      </xsl:for-each>
    </p>
    <xsl:message>
      The following anchor names may collide with internally generated anchors because of their prefix "<xsl:value-of select="$anchor-prefix" />":
      <xsl:for-each select="$badAnchors">
        <xsl:value-of select="@anchor"/><xsl:if test="position()!=last()">, </xsl:if>
      </xsl:for-each>
    </xsl:message>
  </xsl:if>
  
  <!-- check IDs -->
  <xsl:variable name="badTargets" select="//xref[not(@target=//@anchor) and not(ancestor::ed:del)]" />
  <xsl:if test="$badTargets">
    <p class="error">
      The following target names do not exist:
      <xsl:for-each select="$badTargets">
        <xsl:value-of select="@target"/><xsl:if test="position()!=last()">, </xsl:if>
      </xsl:for-each>
    </p>
    <xsl:message>
      The following target names do not exist:
      <xsl:for-each select="$badTargets">
        <xsl:value-of select="@target"/><xsl:if test="position()!=last()">, </xsl:if>
      </xsl:for-each>
    </xsl:message>
  </xsl:if>
 
  
</xsl:template>

<!-- special change mark support, not supported by RFC2629 yet -->

<xsl:template match="@ed:*" />

<xsl:template match="ed:del">
  <xsl:call-template name="insert-issue-pointer"/>
  <del>
    <xsl:copy-of select="@*[namespace-uri()='']"/>
    <xsl:if test="not(@title) and ancestor-or-self::*[@ed:entered-by] and @datetime">
      <xsl:attribute name="title"><xsl:value-of select="concat(@datetime,', ',ancestor-or-self::*[@ed:entered-by][1]/@ed:entered-by)"/></xsl:attribute>
    </xsl:if>
    <xsl:apply-templates />
  </del>
</xsl:template>

<xsl:template match="ed:ins">
  <xsl:call-template name="insert-issue-pointer"/>
  <ins>
    <xsl:copy-of select="@*[namespace-uri()='']"/>
    <xsl:if test="not(@title) and ancestor-or-self::*[@ed:entered-by] and @datetime">
      <xsl:attribute name="title"><xsl:value-of select="concat(@datetime,', ',ancestor-or-self::*[@ed:entered-by][1]/@ed:entered-by)"/></xsl:attribute>
    </xsl:if>
    <xsl:apply-templates />
  </ins>
</xsl:template>

<xsl:template name="insert-issue-pointer">
  <xsl:if test="@ed:resolves">
    <xsl:variable name="resolves" select="@ed:resolves"/>
    <xsl:choose>
      <xsl:when test="not(ancestor::t)">
        <div><a class="open-issue" href="#{$anchor-prefix}.issue.{$resolves}" title="resolves: {$resolves}">
          <xsl:choose>
            <xsl:when test="//ed:issue[@name=$resolves and @status='closed']">
              <xsl:attribute name="class">closed-issue</xsl:attribute>
            </xsl:when>
            <xsl:when test="//ed:issue[@name=$resolves and @status='editor']">
              <xsl:attribute name="class">editor-issue</xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="class">open-issue</xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text>&#160;i&#160;</xsl:text>
        </a></div>
      </xsl:when>
      <xsl:otherwise>
        <a class="open-issue" href="#{$anchor-prefix}.issue.{$resolves}" title="resolves: {$resolves}">
          <xsl:choose>
            <xsl:when test="//ed:issue[@name=$resolves and @status='closed']">
              <xsl:attribute name="class">closed-issue</xsl:attribute>
            </xsl:when>
            <xsl:when test="//ed:issue[@name=$resolves and @status='editor']">
              <xsl:attribute name="class">editor-issue</xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="class">open-issue</xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text>&#160;i&#160;</xsl:text>
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<xsl:template match="ed:replace">
  <xsl:if test="@cite">
    <a class="editor-issue" href="{@cite}" target="_blank" title="see {@cite}">
      <xsl:text>&#160;i&#160;</xsl:text>
    </a>
  </xsl:if>
  <xsl:call-template name="insert-issue-pointer"/>
  <xsl:if test="ed:del">
    <del>
      <xsl:copy-of select="@*[namespace-uri()='']"/>
      <xsl:if test="not(@title) and ancestor-or-self::*[@ed:entered-by] and @datetime">
        <xsl:attribute name="title"><xsl:value-of select="concat(@datetime,', ',ancestor-or-self::*[@ed:entered-by][1]/@ed:entered-by)"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="ed:del/node()" />
    </del>
  </xsl:if>
  <xsl:if test="ed:ins">
    <ins>
      <xsl:copy-of select="@*[namespace-uri()='']"/>
      <xsl:if test="not(@title) and ancestor-or-self::*[@ed:entered-by] and @datetime">
        <xsl:attribute name="title"><xsl:value-of select="concat(@datetime,', ',ancestor-or-self::*[@ed:entered-by][1]/@ed:entered-by)"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="ed:ins/node()" />
    </ins>
  </xsl:if>
</xsl:template>

<!-- convenience template for helping Mozilla (pre/ins inheritance problem) -->
<xsl:template name="insertInsDelClass">
  <xsl:if test="ancestor::ed:del">
    <xsl:attribute name="class">del</xsl:attribute>
  </xsl:if>
  <xsl:if test="ancestor::ed:ins">
    <xsl:attribute name="class">ins</xsl:attribute>
  </xsl:if>
</xsl:template>


<xsl:template name="sectionnumberAndEdits">
  <xsl:choose>
    <xsl:when test="ancestor::ed:del">del-<xsl:number count="ed:del//section" level="any"/></xsl:when>
    <xsl:when test="self::section and parent::ed:ins and local-name(../..)='replace'">
      <xsl:for-each select="../.."><xsl:call-template name="sectionnumberAndEdits" /></xsl:for-each>
      <xsl:for-each select="..">
        <xsl:if test="parent::ed:replace">
          <xsl:for-each select="..">
            <xsl:if test="parent::section">.</xsl:if><xsl:value-of select="1+count(preceding-sibling::section|preceding-sibling::ed:ins/section|preceding-sibling::ed:replace/ed:ins/section)" />
          </xsl:for-each>
        </xsl:if>
      </xsl:for-each>
    </xsl:when>
    <xsl:when test="self::section[parent::ed:ins]">
      <xsl:for-each select="../.."><xsl:call-template name="sectionnumberAndEdits" /></xsl:for-each>
      <xsl:for-each select="..">
        <xsl:if test="parent::section">.</xsl:if><xsl:value-of select="1+count(preceding-sibling::section|preceding-sibling::ed:ins/section|preceding-sibling::ed:replace/ed:ins/section)" />
      </xsl:for-each>
    </xsl:when>
    <xsl:when test="self::section">
      <xsl:for-each select=".."><xsl:call-template name="sectionnumberAndEdits" /></xsl:for-each>
      <xsl:if test="parent::section">.</xsl:if>
      <xsl:choose>
        <xsl:when test="parent::back">
          <xsl:number format="A" value="1+count(preceding-sibling::section|preceding-sibling::ed:ins/section|preceding-sibling::ed:replace/ed:ins/section)" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:number value="1+count(preceding-sibling::section|preceding-sibling::ed:ins/section|preceding-sibling::ed:replace/ed:ins/section)" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="self::middle or self::back"><!-- done --></xsl:when>
    <xsl:otherwise>
      <!-- go up one level -->
      <xsl:for-each select=".."><xsl:call-template name="sectionnumberAndEdits" /></xsl:for-each>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- experimental table formatting -->

<xsl:template match="texttable">
  <xsl:apply-templates select="preamble" />
  <table summary="{preamble}" border="1" cellpadding="3" cellspacing="0">
    <thead>
      <tr>
        <xsl:apply-templates select="ttcol" />
      </tr>
    </thead>
    <tbody>
      <xsl:variable name="columns" select="count(ttcol)" />
      <xsl:for-each select="c[(position() mod $columns) = 1]">
        <tr>
          <xsl:for-each select=". | following-sibling::c[position() &lt; $columns]">
            <td class="top">
              <xsl:variable name="pos" select="position()" />
              <xsl:variable name="col" select="../ttcol[position() = $pos]" />
              <xsl:if test="$col/@align">
                <xsl:attribute name="style">text-align: <xsl:value-of select="$col/@align" />;</xsl:attribute>
              </xsl:if>
              <xsl:apply-templates select="node()" />
              &#0160;
            </td>
          </xsl:for-each>
        </tr>
      </xsl:for-each>
    </tbody>
  </table>
  <xsl:apply-templates select="postamble" />
</xsl:template>

<xsl:template match="ttcol">
  <th valign="top">
    <xsl:variable name="width">
      <xsl:if test="@width">width: <xsl:value-of select="@width" />; </xsl:if>
    </xsl:variable>
    <xsl:variable name="align">
      <xsl:choose>
        <xsl:when test="@align">text-align: <xsl:value-of select="@align" />;</xsl:when>
        <xsl:otherwise>text-align: left;</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:attribute name="style"><xsl:value-of select="concat($width,$align)" /></xsl:attribute>
    <xsl:apply-templates />
  </th>
</xsl:template>

<!-- Chapter Link Generation -->

<xsl:template match="*" mode="links"><xsl:apply-templates mode="links"/></xsl:template>
<xsl:template match="text()" mode="links" />

<xsl:template match="/*/middle//section[not(myns:unnumbered) and not(ancestor::section)]" mode="links">
  <xsl:variable name="sectionNumber"><xsl:call-template name="get-section-number" /></xsl:variable>
  <link rel="Chapter" title="{$sectionNumber} {@title}" href="#{$anchor-prefix}.section.{$sectionNumber}" />
  <xsl:apply-templates mode="links" />
</xsl:template>

<xsl:template match="/*/back//section[not(myns:unnumbered) and not(ancestor::section)]" mode="links">
  <xsl:variable name="sectionNumber"><xsl:call-template name="get-section-number" /></xsl:variable>
  <link rel="Appendix" title="{$sectionNumber} {@title}" href="#{$anchor-prefix}.section.{$sectionNumber}" />
  <xsl:apply-templates mode="links" />
</xsl:template>

<!-- convenience templates -->

<xsl:template name="get-author-summary">
  <xsl:choose>
    <xsl:when test="count(/rfc/front/author)=1">
      <xsl:value-of select="/rfc/front/author[1]/@surname" />
    </xsl:when>
    <xsl:when test="count(/rfc/front/author)=2">
      <xsl:value-of select="concat(/rfc/front/author[1]/@surname,' &amp; ',/rfc/front/author[2]/@surname)" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="concat(/rfc/front/author[1]/@surname,', et al.')" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="get-authors">
  <xsl:for-each select="/rfc/front/author">
    <xsl:value-of select="@fullname" />
    <xsl:if test="position()!=last()">, </xsl:if>
  </xsl:for-each>
</xsl:template>

<xsl:template name="get-category-long">
  <xsl:choose>
    <xsl:when test="$xml2rfc-footer"><xsl:value-of select="$xml2rfc-footer" /></xsl:when>
    <xsl:when test="$xml2rfc-private"/> <!-- private draft, footer not set -->
    <xsl:when test="/rfc/@category='bcp'">Best Current Practice</xsl:when>
    <xsl:when test="/rfc/@category='historic'">Historic</xsl:when>
    <xsl:when test="/rfc/@category='info' or not(/rfc/@category)">Informational</xsl:when>
    <xsl:when test="/rfc/@category='std'">Standards Track</xsl:when>
    <xsl:when test="/rfc/@category='exp'">Experimental</xsl:when>
    <xsl:otherwise>(category unknown)</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="get-header-center">
  <xsl:choose>
    <xsl:when test="string-length(/rfc/front/title/@abbrev) &gt; 0">
      <xsl:value-of select="/rfc/front/title/@abbrev" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="/rfc/front/title" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="get-header-left">
  <xsl:choose>
    <xsl:when test="$xml2rfc-header"><xsl:value-of select="$xml2rfc-header" /></xsl:when>
    <xsl:when test="$xml2rfc-private"/> <!-- private draft, header not set -->
    <xsl:when test="/rfc/@ipr">INTERNET DRAFT</xsl:when>
    <xsl:otherwise>RFC <xsl:value-of select="/rfc/@number"/></xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="get-generator">
  <xsl:variable name="gen">
    <xsl:text>http://greenbytes.de/tech/webdav/rfc2629.xslt, </xsl:text>
    <!-- when RCS keyword substitution in place, add version info -->
    <xsl:if test="contains('$Revision: 1.149 $',':')">
      <xsl:value-of select="concat('Revision ',normalize-space(translate(substring-after('$Revision: 1.149 $', 'Revision: '),'$','')),', ')" />
    </xsl:if>
    <xsl:value-of select="concat('XSLT vendor: ',system-property('xsl:vendor'),' ',system-property('xsl:vendor-url'))" />
  </xsl:variable>
  <xsl:value-of select="$gen" />
</xsl:template>

<xsl:template name="get-header-right">
  <xsl:value-of select="concat(/rfc/front/date/@month,' ',/rfc/front/date/@year)" />
</xsl:template>

<xsl:template name="get-keywords">
  <xsl:variable name="keyw">
    <xsl:for-each select="/rfc/front/keyword">
      <xsl:value-of select="translate(.,',',' ')" />
      <xsl:if test="position()!=last()">, </xsl:if>
    </xsl:for-each>
  </xsl:variable>
  <xsl:value-of select="normalize-space($keyw)" />
</xsl:template>

<!-- get language from context node. nearest ancestor or return the default of "en" -->
<xsl:template name="get-lang">
  <xsl:choose>
    <xsl:when test="ancestor-or-self::*[@xml:lang]"><xsl:value-of select="ancestor-or-self::*/@xml:lang" /></xsl:when>
    <xsl:otherwise>en</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="get-section-number">
  <xsl:variable name="hasEdits" select="count(//ed:del|//ed:ins)!=0" />
  <xsl:choose>
    <xsl:when test="$hasEdits">
      <xsl:call-template name="sectionnumberAndEdits" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="ancestor::back"><xsl:number count="ed:del|ed:ins|section|appendix" level="multiple" format="A.1.1.1.1.1.1.1" /></xsl:when>
        <xsl:when test="self::appendix"><xsl:number count="ed:del|ed:ins|appendix" level="multiple" format="A.1.1.1.1.1.1.1" /></xsl:when>
        <xsl:otherwise><xsl:number count="ed:del|ed:ins|section" level="multiple"/></xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="get-section-type">
  <xsl:param name="prec" />
  <xsl:choose>
    <xsl:when test="ancestor::back">Appendix</xsl:when>
    <xsl:otherwise>Section</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="insert-conditional-pagebreak">
  <xsl:if test="$xml2rfc-compact!='yes'">
    <xsl:attribute name="class">np</xsl:attribute>
  </xsl:if>
</xsl:template>


</xsl:stylesheet>
