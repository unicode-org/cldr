# (c) 2007 IBM Corporation and Others. All Rights Reserved.
# Python module for scanning and mirroring CLDR references.
#
# Steven R. Loomis. Oct 30th, 2007
#
#
# usage:  refmirror.pl  /path/to/cldr/common   /path/to/nonexistent/refmirror-output-dir
#
# note:
#  - does condense duplicate URLs within a locale, to only download once
#      (should condense globally?)
# 
# todo:
#  - only handles <references> formats - so CLDR 1.5 main/ but NOT collation/
#  - doesn't escape UTF-8 URLs such as wikipedia ( writes out url in utf-8, does not %-encode )
#  - should probably pass "-n 2" or such to wget to shorten hang time
#

from xml.dom import minidom
import sys
import os
import codecs

progname = sys.argv[0]

if len(sys.argv) != 3:
    raise RuntimeError, "Usage: %s  <cldrroot> <output dir>"%(sys.argv[0])

cldrdir = sys.argv[1]
htmldir = sys.argv[2]

print "# creating %s (shouldn't exist)" % htmldir
os.mkdir(htmldir)

print "# walking %s" % cldrdir

dirs = os.walk(cldrdir)

for dir in dirs:
    name = dir[0]
    subdirs = dir[1]
    files = dir[2]
    if(name.endswith("/CVS")):
        continue
    leaf=name[len(cldrdir):]
    if(leaf.startswith('/')):
        leaf=leaf[1:]
    print "dir: %s" % str(leaf)
    out = "%s/%s" % (htmldir,leaf)
    if(len(leaf)>0):
        os.mkdir(out)
    for file in files:
        if not file.endswith('.xml'):
            continue
        
        # hash of already read items
        alreadyread = {}
        
        # stub?
        stub = file
        xmldir = "%s/%s" % (out,file)
        
        # read file
        filepath = "%s/%s"%(name,file)
        dom = minidom.parse(filepath)
        
        nodes = dom.childNodes
        
        refNode = nodes[1].getElementsByTagName('references')
        
        if not refNode:
            #print "## no refnode %s" % filepath
            continue
        print "## got refnode %s" % filepath
        os.mkdir(xmldir)
        for ref in refNode[0].getElementsByTagName('reference'):
            #print "## - ref %s" % str(ref)
            if not ref.hasAttribute('type'):
                print "## untyped reference in %s" % filepath
            else:
                type = ref.getAttribute('type')
                
                if ref.hasAttribute('alt'):
                    type = "%s-%s" % (type,ref.getAttribute('alt'))
                
                typedir = "%s/%s" % (xmldir,type)
                
                
                if not ref.hasAttribute('uri'):
                    #print "# No 'uri' attribute on %s / %s"%(file,type)
                    continue
                uri = ref.getAttribute('uri')
                if uri.startswith('urn:'):
                    uri = uri[len('urn:'):]
                if uri.startswith('isbn'):
                    # assume ISBN can fend for itself
                    continue
                if uri.startswith('ISBN'):
                    # assume ISBN can fend for itself
                    continue
                if not uri.startswith('http'):
                    print "# Not a known scheme: %s on %s / %s"%(uri,file,type)
                    continue;
                #print uri
                # write the Info file
                file = open("%s.xml"%typedir, 'w')
                file.write( codecs.BOM_UTF8 )
                file.write(ref.toxml().encode( "utf-8" ))
                file.close()
                # make the dir..
                os.mkdir(typedir)
                
                # already read it?
                if uri in alreadyread.keys():
                    already = alreadyread[uri]
                    alfile = open("%s/duplicate.txt"%typedir,'w')
                    alfile.write( ("%s\n"%already).encode("utf-8"))
                    alfile.close()
                else:
                    alreadyread[uri] = type
                    cmd = "wget -P '%s' -nd -np -k -p '%s' 2>&1 > %s.err"%(typedir,uri,typedir)
                    print cmd.encode("utf-8")
                    try:
                        os.system(cmd)
                    except Exception,e:
                        exfile = open("%s.exc"%typedir,'w')
                        exfile.write( ("exception: %s\n"%str(e)).encode("utf-8"))
                        exfile.close()
                        print "%s - exception %s"%(typedir,str(e))
