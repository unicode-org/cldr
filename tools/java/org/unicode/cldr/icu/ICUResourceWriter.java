/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
/**
 * @author Ram Viswanadha
 * @author Brian Rower - June 2008 - added writeBinary methods
 */
package org.unicode.cldr.icu;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

import com.ibm.icu.text.UTF16;

public class ICUResourceWriter {
    private static final String CHARSET         = "UTF-8";
    private static final String OPENBRACE       = "{";
    private static final String CLOSEBRACE      = "}";
    private static final String OPENPAREN       = "(";
    private static final String CLOSEPAREN      = ")";
    private static final String COLON           = ":";
    private static final String COMMA           = ",";
    private static final String QUOTE           = "\"";
    private static final String COMMENTSTART    = "/**";
    private static final String COMMENTEND      = " */";
    private static final String COMMENTMIDDLE   = " * ";
    private static final String INDENT          = "    ";
    private static final String EMPTY           = "";
    private static final String BIN             = "bin";
    private static final String INTS            = "int";
    private static final String TABLE           = "table";
    private static final String IMPORT          = "import";
    private static final String INCLUDE         = "include";
    private static final String PROCESS         = "process";
    private static final String ALIAS           = "alias";
    private static final String INTVECTOR       = "intvector";
    //private static final String ARRAYS          = "array";
    private static final String LINESEP         = System.getProperty("line.separator");
    private static final String STRTERM = "\0";
    
    public static final int SIZE_OF_INT = 4; 
    public static final int SIZE_OF_CHAR = 2;
    
    public static final String UCA_RULES = "uca_rules";
    public static final String TRANSLITERATOR = "transliaterator";
    public static final String COLLATION = "collation";
    public static final String DEPENDENCY = "dependency";
    
    public static final int BIN_ALIGNMENT = 16;
    /**
     * This integer is a count of ALL the resources in this tree (not including the root object)
     */
    public static int maxTableLength;
   
    public static class Resource{
        public class MalformedResourceError extends Error {
            private static final long serialVersionUID = -5943014701317383613L;
            public Resource offendingResource ;
            public MalformedResourceError(String str, Resource res) {
                super(str);
                offendingResource = res;
            }
        }

        String[] note = new String[20];
        int noteLen = 0;
        String translate;
        /**
         * This is a comment which will appear on the item.
         */
        String comment;
        /**
         * This is the resource's name, or, 'key'
         */
        public String name;
        /**
         * This links to the next sibling of this item in the list
         */
        public Resource next;
        boolean noSort = false;
        /**
         * If this item contains other items, this points to the first item in its list 
         */
        public Resource first=null; 
        
        /**
         * A counter for how many children there are below this object.
         */
        public int numChildren;
        
        /**
         * Stores how many bytes are used by the children of this object.
         */
        public int sizeOfChildren;
        
        /**
         * Stores how many bytes are used by this resource.
         */
        public int size; 
        
        /**
         * This integer stores the number of bytes from the beginning of the "key string"
         * this resources key starts. For more information see the comment in LDML2ICUBinaryWriter above
         * the writeKeyString method.
         */
        public int keyStringOffset;
        
        public boolean hasKey = true;
        
        
        public boolean isTop = false;
       
     
        
        /**
         * This method will set the size of the resource. Overwritten for each child object
         */
        public void setSize()
        {
        	size = 0;
        }
        
        
        public Resource()
        {
        	isTop = false;
        }
        /**
         * @return the end of this chain, by repeatedly calling next
         * @see next
         */
        public Resource end() {
            ICUResourceWriter.Resource current = this;
            while (current != null) {
                if (current.next == null) {
                    return current;
                }
                current = current.next;
            }
            return current;
        }
        
        /**
         * Append to a basic list.
         *  Usage:
         *    Resource list = null; list = Resource.addAfter(list, res1); list = Resource.addAfter(list,res2); ...
         * @param list the list to append to (could be null)
         * @param res the item to add
         * @return the beginning of the list
         */
        static final Resource addAfter(Resource list, Resource res) {
            if(list == null) {
                list = res;
            } else {
                // go to end of the list
                Resource last = list.end();
                last.next = res;
            }
            // return the beginning
            return list;
        }
        
        /**
         * Appends 'res' to the end of 'this' (next sibling chain)
         * @param res the item (or items) to be added
         * @return the new beginning of the chain (this)
         */
        public Resource addAfter(Resource res) {
            return addAfter(this, res);
        }
        
        /**
         * Replace the contents (first) of this object with the parameter
         * @param res The object to be replaced
         * @return the old contents
         */
        public Resource replaceContents(Resource res) {
            Resource old = first;
            first = res;
            return old;
        }
        
        /**
         * Append the contents (first) of this object with the parameter
         * @param res the object to be added to the contents
         * @return the end of the contents chain
         */
        public Resource appendContents(Resource res) {
            if(first == null) {
                first = res;
            } else {
                first.end().next = res;
            }
            return res.end();
        }
        
        /**
         * Check whether this item has contents
         * @return true if this item is empty (first==null)
         */
        public boolean isEmpty() {
            return (first == null);
        }
        
         /**
         * @param val
         * @return
         */
        
        public StringBuffer escapeSyntaxChars(String val){
            // escape the embedded quotes
            if(val==null) {
                System.err.println("Resource.escapeSyntaxChars: error, resource '" + name + "': string value is NULL - assuming 'empty'");
                throw new MalformedResourceError("Resource.escapeSyntaxChars: error, resource '" + name + "': string value is NULL - assuming 'empty'", this);
//                return new StringBuffer("");
            }
            char[] str = val.toCharArray();
            StringBuffer result = new StringBuffer();
            for(int i=0; i<str.length; i++){
                switch (str[i]){
                    case '\u0022':
                        result.append('\\'); //append backslash
                    default:
                        result.append(str[i]);
                }      
            }
            return result;
        }
        public void write(OutputStream writer, int numIndent, boolean bare){
            while(next!=null){
                next.write(writer, numIndent+1, false);
            }
        }
        public void writeIndent(OutputStream writer, int numIndent){
            for(int i=0; i< numIndent; i++){
                write(writer,INDENT);
            }
        }
        
        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        	//should never get called
        	System.err.println("Unexpected type: " + this.getClass().toString());
        	System.err.println("Resource Name: " + this.name);
        	return usedOffset;
        }
    
        public void write(OutputStream writer, String value){
            try {
                byte[] bytes = value.getBytes(CHARSET);
                writer.write(bytes, 0, bytes.length);
                
            } catch(Exception e) {
                System.err.println(e);
                System.exit(1);
            }
        }
        public void writeComments(OutputStream writer, int numIndent){
            if(comment!=null || translate != null || noteLen > 0){
                // print the start of the comment
                writeIndent(writer, numIndent);
                write(writer, COMMENTSTART+LINESEP);
                
                // print comment if any
                if(comment!=null){
                    int index = comment.indexOf('\n');
                    if(index>-1){
                        StringBuffer indent = new StringBuffer("\n");
                        for(int i=0; i<numIndent; i++){
                            indent.append(INDENT);
                        }
                        indent.append(COMMENTMIDDLE);
                        comment = comment.replaceAll("\n", indent.toString());
                    }
                    writeIndent(writer, numIndent);
                    write(writer, COMMENTMIDDLE);
                    write(writer, comment);
                    write(writer, LINESEP);
                    
                }
                  
                // terminate the comment
                writeIndent(writer, numIndent);
                write(writer, COMMENTEND+LINESEP);
            }          
        }
        public void sort(){
//            System.out.println("In sort");
            return;
        }
        public void swap(){
            return;
        }
        
        boolean findResourcePath(StringBuffer str, Resource res) {
            if(name != null) {
                str.append(name);
            }
            if(res == this) {
                return true;
            }
            str.append('/');
            // process the siblings of the children...
            int n = 0;
            int oldLen = str.length();
            for(Resource child = first; child != null; child = child.next) {
                if(child.name == null) {
                    str.append("#"+n);
                }
                if(child.findResourcePath(str,res)) {
                    return true;
                }
                n++;
                str.setLength(oldLen); // reset path length
            }
            return false;
        }
        
        String findResourcePath(Resource res) {
            if(next != null) {
                throw new IllegalArgumentException("Don't call findResourcePath(Resource res) on resources which have siblings");
            }
            StringBuffer str = new StringBuffer();
            if(findResourcePath(str, res)) {
                return str.toString();
            } else {
                return null;
            }
        }
    }
    
    /* ***************************END Resource ************/
   
    /* All the children resource types below***************/
    
    public static class  ResourceAlias extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+ALIAS+ OPENBRACE+QUOTE+escapeSyntaxChars(val)+QUOTE+CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name! "+ name);
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }

        /**
         * Writes this object to the provided output stream in binary format. Copies formating from Genrb (in ICU4C tools.
         * 
         * @param out A File output stream which has already been set up to write to.
         */
        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        	byte[] valLenBytes;
        	byte[] valBytes;
        	byte[] padding;
        	
        	valLenBytes = intToBytes(val.length());

            try
        	{
            	valBytes = (val + STRTERM).getBytes(LDML2ICUBinaryWriter.CHARSET16);
                padding = create32Padding(valBytes.length); 	
        		out.write(valLenBytes);
        		LDML2ICUBinaryWriter.written += valLenBytes.length;
        		
        		out.write(valBytes);
        		LDML2ICUBinaryWriter.written += valBytes.length;
        		
        		if(padding != null)
        		{
        			out.write(padding);
        			LDML2ICUBinaryWriter.written += padding.length;
        		}

        		
        	}
        	catch (UnsupportedEncodingException e)
        	{
        		errUnsupportedEncoding();
        	}
        	catch (IOException e)
        	{
        		errIO();
        	}
        	return usedOffset;
        }
        
        
        public void setSize()
        {
        	//a pointer + the string
        	size = SIZE_OF_INT + ((val.length() + 1) * SIZE_OF_CHAR);
        }
    }
    
    public static class  ResourceArray extends Resource{
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(name!=null){
                write(writer, name+OPENBRACE+LINESEP);
            }else{
                write(writer, OPENBRACE+LINESEP);
            }
            numIndent++;
            Resource current = first;
            while(current != null){
                current.write(writer, numIndent, true);
                if(current instanceof ResourceTable ||
                   current instanceof ResourceArray){
                    
                }else{
                    write(writer, COMMA+LINESEP);
                }
                current = current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
        }
        public void sort(){
            if(noSort == true){
                return;
            }
            Resource current = first;
            while(current!=null){
                current.sort();
                current = current.next;
            }
        }

       
        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        	int count = 0;
        	int[] resources = new int[numChildren];
        	byte[] resourceBytes;
        	Resource current = this.first;
        	
        	//if there are items in the array
        	if(current != null)
        	{
          	   	//start at the first one and loop
        		while(current != null)
        		{
        			//if it's an int: resources[i] = (current->fType << 28) | (current->u.fIntValue.fValue & 0xFFFFFFF);
        			if(current instanceof ResourceInt)
        			{
        				int value = 0;
        				
        				try
        				{
        					value = Integer.parseInt(((ResourceInt)current).val);
        				}
        				catch(NumberFormatException e)
        				{
        					System.err.println("Error converting string to int: " + e.getMessage());
        					System.exit(1);
        				}
        				resources[count] = LDML2ICUBinaryWriter.URES_INT << 28 | (value & 0xFFFFFFF);
        			}
        			else
        			{
        				//write the current object
        				usedOffset = current.writeBinary(out, usedOffset);
        				
        				// write 32 bits for identification?
        				if(current instanceof ResourceString)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_STRING << 28 | usedOffset >>> 2;
        				}
        				else if(current instanceof ResourceTable)
        				{
        					if(((ResourceTable)current).is32Bit())
        					{
        						resources[count] = LDML2ICUBinaryWriter.URES_TABLE32 << 28 | usedOffset >>> 2;
        					}
        					else
        					{
        						resources[count] = LDML2ICUBinaryWriter.URES_TABLE << 28 | usedOffset >>> 2;	
        					}
        					
        				}
        				else if(current instanceof ResourceAlias)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_ALIAS << 28 | usedOffset >>> 2;
        				}
        				else if(current instanceof ResourceArray)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_ARRAY << 28 | usedOffset >>> 2;
        				}
        				else if (current instanceof ResourceIntVector)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_INT_VECTOR << 28 | usedOffset >>> 2;
        				}
        				
        				usedOffset += current.size + pad32(current.size);        					
        			}
        			count++;
        			current = current.next;
        		}
        		
        		//convert the resource array into the resourceBytes
        		resourceBytes = intArrayToBytes(resources);
        		
        		try
        		{
        			//write the array count (int32)
        			out.write(intToBytes(count));
        			LDML2ICUBinaryWriter.written += intToBytes(count).length;
        			
            		//write the resources array...should be size of int32 * array count
        			out.write(resourceBytes);
        			LDML2ICUBinaryWriter.written += resourceBytes.length;
        		}
        		catch (IOException e)
        		{
        			errIO();
        		}
        		
        	}
        	else //Empty array
        	{
        		try
        		{
        			out.write(intToBytes(0));
        			LDML2ICUBinaryWriter.written += intToBytes(0).length;
        		}
        		catch (IOException e)
        		{
        			errIO();
        		}
        	}
        	return usedOffset;
        	
        }
        
        /**
         * This method will set the size of the resource. 
         */
        public void setSize()
        {
        	//Arrays have children. 
        	int x = 0;
        	Resource current = this.first;
        	
        	this.sizeOfChildren = 0;
        	
        	while(current != null)
        	{
        		x++;
        		
        		this.sizeOfChildren += current.size + pad32(current.size);
        		
        		if(current instanceof ResourceTable || current instanceof ResourceArray)
        		{
        			this.sizeOfChildren += current.sizeOfChildren;
        		}
        		 
        		current = current.next;
        	}
        	
        	//pointer to the key + pointer to each member
        	size = SIZE_OF_INT + (x * SIZE_OF_INT);
        	
        }
    }
    
    public static class ResourceInt extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+INTS+ OPENBRACE + val +CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name: " +name);
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
        
        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        	return usedOffset;
        }
        
        /**
         * This method will set the size of the resource. Overwritten for each child object
         */
        public void setSize()
        {
        	size = 0;
        	
        }
    }
    
    public static class ResourceIntVector extends Resource{
        public String smallComment = null;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            write(writer, name+COLON+INTVECTOR+OPENBRACE);
            if(smallComment != null) {
                write(writer, " "+COMMENTSTART+" "+smallComment+" "+COMMENTEND);
            }
            write(writer, LINESEP);
            numIndent++;
            ResourceInt current = (ResourceInt) first;
            while(current != null){
                //current.write(writer, numIndent, true);
                writeIndent(writer, numIndent);
                write(writer, current.val);
                write(writer, COMMA+LINESEP);
                current = (ResourceInt) current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
        }
        
        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        	int count = 0;
        	int[] numbers = new int[numChildren];
        	byte[] numBytes;
        	Resource current = this.first;
        	
        	while(current != null)
        	{
        		numbers[count] = Integer.parseInt(((ResourceInt)current).val);
        		count++;
        		current = current.next;
        	}
        	
        	numBytes = intArrayToBytes(numbers);
        	
        	try
        	{
        		out.write(intToBytes(count));
        		LDML2ICUBinaryWriter.written += intToBytes(count).length;
        		
        		out.write(numBytes);
        		LDML2ICUBinaryWriter.written += numBytes.length;
        	}
        	catch (IOException e)
        	{
        		errIO();
        	}
        	return usedOffset;
        }
        
        /**
         * This method will set the size of the resource. Overwritten for each child object
         */
        public void setSize()
        {
           	//has children
        	int x = 0;
        	Resource current = this.first;
        	
        	while(current != null)
        	{
        		x++;
        		current = current.next;
        	}
        	
        	//this resources key offset + each int
        	size = SIZE_OF_INT + (x * SIZE_OF_INT);
        }
    }
    
    public static class ResourceString extends Resource{
        public ResourceString() {
        }
        public ResourceString(String name, String val) {
            this.name = name;
            this.val = val;
        }
        public String val;
        /**
         * one-line comment following the value. ignored unless in bare mode.
         */
        public String smallComment = null;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name! " + name);
                }
                
                write(writer,QUOTE+escapeSyntaxChars(val)+QUOTE); 
                if(smallComment != null) {
                    write(writer, " "+ COMMENTSTART + " " + smallComment + " " + COMMENTEND);
                }
            }else{
                StringBuffer str = escapeSyntaxChars(val);
                
                int colLen = 80-(numIndent*4);
                int strLen = str.length();
                if(strLen>colLen){
                    int startIndex = 0;
                    int endIndex = 0;
                    write(writer, name + OPENBRACE +LINESEP);
                    numIndent++;
                    boolean isRules = name.equals("Sequence"); 
                    // Find a safe point where we can insert a line break!
                    while(endIndex < strLen){
                        startIndex = endIndex;
                        endIndex = startIndex+colLen;
                        if(endIndex>strLen){
                            endIndex = strLen;
                        }
                        if(isRules){
                            // look for the reset tag only if we are writing 
                            // collation rules!
                            int firstIndex = str.indexOf("&", startIndex);
                            
                            if(firstIndex >-1 ){
                                if(startIndex!= (firstIndex -1)&& startIndex!=firstIndex && firstIndex < endIndex ){
                                    if(str.charAt(firstIndex-1)!=0x27){
                                        endIndex = firstIndex;
                                    }
                                }
                                int nextIndex  = 0;
                                while((nextIndex = str.indexOf( "&", firstIndex+1))!=-1 && nextIndex < endIndex){
                                    
                                    if(nextIndex>-1 && firstIndex!=nextIndex){
                                        if(str.charAt(nextIndex-1)!=0x27){
                                            endIndex = nextIndex;
                                            break;
                                        }else {
                                            firstIndex = nextIndex;
                                        }
                                    }
                                }
                            }
                        }
                        int indexOfEsc = 0;
                        if((indexOfEsc =str.lastIndexOf("\\u",endIndex))>-1 && (endIndex-indexOfEsc)<6 ||
                           (indexOfEsc = str.lastIndexOf("\\U",endIndex))>-1 && (endIndex-indexOfEsc)<10 ||
                           (indexOfEsc = str.lastIndexOf("'\'",endIndex))>-1 && (endIndex-indexOfEsc)<3){
                            
                            endIndex = indexOfEsc;
                        }
                        if(indexOfEsc > -1 && str.charAt(indexOfEsc-1)==0x0027){
                            endIndex = indexOfEsc-1;
                        }
                        if(endIndex<strLen && UTF16.isLeadSurrogate(str.charAt(endIndex-1))){
                            endIndex--;
                        }
                        
                        writeIndent(writer, numIndent);
                        write(writer, QUOTE);
                        write(writer, str.substring(startIndex,endIndex));
                        write(writer, QUOTE+LINESEP);
                    }
                    numIndent--;
                    writeIndent(writer, numIndent);
                    write(writer,CLOSEBRACE + LINESEP);
                    
                }else{
                    write(writer,name + OPENBRACE + QUOTE + str.toString() + QUOTE + CLOSEBRACE + LINESEP);
                }
                
            }
        }
    
        
        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        	
           	//clean up quotes if any
			if(this.val.indexOf("\"") >= 0)
			{
				this.val = LDML2ICUBinaryWriter.removeQuotes(this.val);
			}
        	
        	String valPlusTerm = val + STRTERM;
        	byte[] valBytes;
        	byte[] valLenBytes;
        	byte[] padding;
        	
        	valLenBytes = intToBytes(val.length());

        	try
        	{
        		valBytes = valPlusTerm.getBytes(LDML2ICUBinaryWriter.CHARSET16);
        		padding = create32Padding(valBytes.length);
        		out.write(valLenBytes); 	//32 bit int
        		LDML2ICUBinaryWriter.written += valLenBytes.length;
        		
            	out.write(valBytes); 		//The string plus a null terminator
            	LDML2ICUBinaryWriter.written += valBytes.length;
            	
            	if(padding != null)
            	{
            		out.write(padding);
            		LDML2ICUBinaryWriter.written += padding.length;
            	}
        	}
        	catch (UnsupportedEncodingException e)
        	{
        		System.err.print("Problems converting string resource to " + LDML2ICUBinaryWriter.CHARSET16);
        		System.exit(1);
        	}
        	catch (IOException e)
        	{
        		System.err.print("Problems writing the string resource to file.");
        		System.exit(1);
        	}
        	return usedOffset;
        }
        
        /**
         * This method will set the size of the resource. Overwritten for each child object
         */
        public void setSize()
        {
        	//a pointer to the key + a string
        	size = SIZE_OF_INT + (SIZE_OF_CHAR * (val.length() + 1));
        }
    }
    
    public static class ResourceTable extends Resource{
        public String annotation;
        public static final String NO_FALLBACK= "nofallback";
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(annotation==null){
                write(writer, name+OPENBRACE+LINESEP);
            }else{
                write(writer, name+COLON+TABLE+OPENPAREN+annotation+CLOSEPAREN+OPENBRACE+LINESEP);
            }
            numIndent++;
            Resource current = first;
            while(current != null){
                current.write(writer, numIndent, false);
                current = current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
        }
        // insertion sort of the linked list
        // from Algorithms in C++ Sedgewick
        public void sort(){
            if(noSort == true){
                return;
            }
            //System.out.println("Entering sort of table: "+name);
            Resource b =new Resource();
            Resource a = first;
            Resource t,u,x;
            for(t = a; t!=null; t=u ){
                u=t.next;
                for(x=b;x.next!=null; x=x.next){
//                    if(x.next == null) {
//                        throw new InternalError("Null NEXT node from " + x.name+","+x.toString());
//                    } else if(x.next.name == null) {
//                        throw new InternalError("Null NEXT name from " + x.name+","+x.toString()+" -> " + x.next.toString());
//                    }
                    if(x.next.name.compareTo(t.name)>0){
                        break;
                    }
                }
                t.next = x.next;
                x.next = t;
            }
//            System.out.println("Exiting sort of table");
            if(b.next!=null){
                first = b.next;   
            }
            
            Resource current = first;
//            if(current == this) {
//                throw new InternalError("I'm my own child.. name="+name);
//            }
            while(current!=null){
                current.sort();
//                if(current.next == current) {
//                    throw new InternalError("Sibling links to self: " + current.name);
//                }
                current = current.next;
            }
            
        } // end sort()
        
        public boolean is32Bit()
        {
        	Resource current = this.first;
        	boolean mustBe32 = false;
        	
        	while(current != null)
        	{
        		if(current.keyStringOffset > 0xFFFF)
        		{
        			mustBe32 = true;
        		}
        		current = current.next;
        	}
        	return mustBe32;
        }

        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        	int count = 0;
        	int pad;
        	Resource current = this.first;
        	int[] resources = new int[numChildren];
        	short[] keys16 = null;
        	int[] keys32 = null;
        	boolean is32Bit = this.is32Bit();
        	byte[] padding;
        	             	
        	if(is32Bit)
        	{
        		keys32 = new int[numChildren];
        	}
        	else
        	{
        		keys16 = new short[numChildren];
        	}
        	
        	//if the table has objects in it
        	if(current != null)
        	{
        		
        		//loop through them all
        		while(current != null)
        		{
        			//get the key ptr for current (size depends on table size, store in array
        			if(is32Bit)
            		{
            			keys32[count] = current.keyStringOffset;
            		}
            		else
            		{
            			keys16[count] = (short)current.keyStringOffset;
            		}
        			
        			//if INT
        			if(current instanceof ResourceInt)
        			{
        				//resources[i] = (current->fType << 28) | (current->u.fIntValue.fValue & 0xFFFFFFF);
        				int value = 0;
        				
        				try
        				{
        					value = Integer.parseInt(((ResourceInt)current).val);
        				}
        				catch(NumberFormatException e)
        				{
        					System.err.println("Error converting string to int: " + e.getMessage());
        					System.exit(1);
        				}
        				resources[count] = LDML2ICUBinaryWriter.URES_INT << 28 | (value & 0xFFFFFFF);
        				
        			}
        			else
        			{
        				//write the current object
        				usedOffset = current.writeBinary(out, usedOffset);
        				
        				// write 32 bits for identification?
        				if(current instanceof ResourceString)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_STRING << 28 | usedOffset >>> 2;
        				}
        				else if(current instanceof ResourceTable)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_TABLE << 28 | usedOffset >>> 2;
        				}
        				else if(current instanceof ResourceAlias)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_ALIAS << 28 | usedOffset >>> 2;
        				}
        				else if(current instanceof ResourceArray)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_ARRAY << 28 | usedOffset >>> 2;
        				}
        				else if (current instanceof ResourceIntVector)
        				{
        					resources[count] = LDML2ICUBinaryWriter.URES_INT_VECTOR << 28 | usedOffset >>> 2;
        				}
        				
        				usedOffset += current.size + pad32(current.size);       
        			}
        			count++;
        			current = current.next;
        		}
        		
        		//write the member count and the key offsets
        		if(is32Bit)
        		{
        			try
        			{
        				//write a 32 bit block with the number of items in this table
            			out.write(intToBytes(count));
            			LDML2ICUBinaryWriter.written += intToBytes(count).length;
            			
            			//write all the 32 bit keys which were added to the array.
            			out.write(intArrayToBytes(keys32));
            			LDML2ICUBinaryWriter.written += intArrayToBytes(keys32).length;
            			
            			out.write(intArrayToBytes(resources));
            			LDML2ICUBinaryWriter.written += intArrayToBytes(resources).length;
        			}
        			catch (IOException e)
        			{
        				errIO();
        			}
        			
        		}
        		else
        		{
        			try
        			{
        				//write 2 byte block with the number of items in this table
            			out.write(shortToBytes((short)count));
            			LDML2ICUBinaryWriter.written += shortToBytes((short)count).length;
            			
            			//write all the 2 byte keys which were added to an array
            			out.write(shortArrayToBytes(keys16));
            			LDML2ICUBinaryWriter.written += shortArrayToBytes(keys16).length;
            			
            			pad = pad32(this.size);
            			padding = createPadding(pad);
            			if(padding != null)
            			{
            				out.write(padding);
            				LDML2ICUBinaryWriter.written += padding.length;
            			}
            		           			
            			out.write(intArrayToBytes(resources));
            			LDML2ICUBinaryWriter.written += intArrayToBytes(resources).length; 
            				
            			
        			}
        			catch (IOException e)
        			{
        				errIO();
        			}
        			
        		}
        	}
        	else //else (the table is empty)
        	{
        		short zero = 0;
        		        		       		
        		//We'll write it as a 16 bit table, because it's empty...
        		try 
        		{
        			//write a 16 bit zero.
        			out.write(shortToBytes(zero));
        			LDML2ICUBinaryWriter.written += shortToBytes(zero).length;
        			
        			//pad it
        			padding = createPadding(pad16Bytes(2));
        			if(padding != null)
        			{
        				out.write(padding);
        				LDML2ICUBinaryWriter.written += padding.length;
        			}
        			
        			
        		}
        		catch(IOException e)
        		{
        			errIO();
        		}
        	}
        	return usedOffset;
        }
        
        /**
         * This method will set the size of the resource. Overwritten for each child object
         */
        public void setSize()
        {
        	//Tables have children.
        	int x = 0;
        	Resource current = this.first;
        	this.sizeOfChildren = 0;
        	while(current != null)
        	{
        		x++;
        		this.sizeOfChildren += current.size + pad32(current.size);
        		
        		if(current instanceof ResourceTable || current instanceof ResourceArray)
        		{
        			this.sizeOfChildren += current.sizeOfChildren;
        		}
        		 
        		current = current.next;
        	}
        	
        	if(x > maxTableLength)
        	{
        		maxTableLength = x;
        	}
        	
        	if(this.is32Bit())
        	{
        		//this resources key offset + a key offset for each child + a pointer to their resource object.
        		size = SIZE_OF_INT + (x * 2 * SIZE_OF_INT);
        	}
        	else
        	{
        		//this resources key offset + a pointer to each childs resource + a 16 bit pointer to each childs key
        		size = SIZE_OF_INT/2 + (x * (SIZE_OF_INT + (SIZE_OF_INT / 2)));
        	}
        }
    }
    
    /* Currently there is nothing in LDML which converts to a Binary resource. So this type is currently unused. */
    public static class ResourceBinary extends Resource
    {
        String internal;
        String external;
        byte[] data;
        
        public void write(OutputStream writer, int numIndent, boolean bare)
        {
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(internal==null)
            {
                String line = ((name==null) ? EMPTY : name)+COLON+IMPORT+ OPENBRACE+QUOTE+external+QUOTE+CLOSEBRACE + ((bare==true) ?  EMPTY : LINESEP);
                write(writer, line);
            }
            else
            {
                String line = ((name==null) ? EMPTY : name)+COLON+BIN+ OPENBRACE+internal+CLOSEBRACE+ ((bare==true) ?  EMPTY : LINESEP);
                write(writer,line);
            }
        }
        
        public void setSize()
        {
        	//sizeof(int32_t) + sizeof(uint8_t) * length + BIN_ALIGNMENT;
        	size = SIZE_OF_INT + data.length + BIN_ALIGNMENT;
        }
        
        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        	int pad = 0;
        	int extrapad = pad32(this.size);
        	int dataStart = usedOffset + SIZE_OF_INT; 
        	
        	try
        	{
        		
        	
	        	//write some padding
				if (dataStart % BIN_ALIGNMENT != 0) 
				{
				  pad = (BIN_ALIGNMENT - (dataStart % BIN_ALIGNMENT));
				  out.write(createPadding(pad));
				  usedOffset += pad;
				}
				
				//write the length of the data
				out.write(intToBytes(data.length));
				
				//if there is data, write it
				if (data.length > 0)
				{
					out.write(data);
				}
				
				//write some more padding
				out.write(createPadding(BIN_ALIGNMENT - pad + extrapad));
        	}
        	catch(Exception e)
        	{
        		System.err.println("Had problems writing Binary Resource");
        	}
        	return usedOffset;
        }
    }
    
    
    
    public static class  ResourceProcess extends Resource{
        String val;
        String ext;
        public void write(OutputStream writer, int numIndent, boolean bare)
        {
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+PROCESS+
                             OPENPAREN + ext + CLOSEPAREN + OPENBRACE+QUOTE+escapeSyntaxChars(val)+QUOTE+CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name! " + name);
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
        
        public int writeBinary(FileOutputStream out, int usedOffset)
        {
        		if(this.name.equals("depends"))
        		{
        			
        		}
        		else
        		{
        			
        			//should never get called
                	System.err.println("Unexpected type: " + this.getClass().toString());
                	System.err.println("Resource Name: " + this.name);
                	return usedOffset;
        		}
        		return usedOffset;
        }
    }
    
    public static class  ResourceImport extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+IMPORT+ OPENBRACE+QUOTE+escapeSyntaxChars(val)+QUOTE+CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name! " + name);
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
    }
    
    /* Seems to be unused. Never parsed in */
    public static class  ResourceInclude extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+INCLUDE+ OPENBRACE+QUOTE+escapeSyntaxChars(val)+QUOTE+CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name! "+ name);
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
    } 
    /* END Resources ******************************************************************************/
    
    
    /* Helper methods. ****************************************************************************/
    /**
     * Convenience function
     * @param name
     * @param val 
     * @return new ResourceString
     */
    public static Resource createString(String name, String val) {
        return new ResourceString(name,val);
    }
    
    private static int pad32(int x)
	{
		return ((x % SIZE_OF_INT) == 0)? 0 : (SIZE_OF_INT - (x % SIZE_OF_INT));
	}
    
    private static byte[] create32Padding(int x)
    {
    	byte[] b = new byte[pad32(x)];
    	if(pad32(x) == 0)
    	{
    		return null;
    	}
    	
    	for(int z = 0; z < b.length; z++)
    	{
    		b[z] = 0;
    	}
    	return b;
    }
    
    private static int pad16Bytes(int x)
	{
		return ((x % 16) == 0)? 0 : (16 - (x % 16));
	}
    
    
    /**
	 * Takes a 32 bit integer and returns an array of 4 bytes.
	 * 
	 */
	private static byte[] intToBytes(int x)
	{
		byte[] b = new byte[4];
		b[3] = (byte)(x); // just the last byte
		
		x = x >>> 8; //shift each byte over one spot.
		b[2] = (byte)(x); //just the last byte
		
		x = x >>> 8; //shift each byte over one spot.
		b[1] = (byte)(x); //just the last byte
		
		x = x >>> 8; //shift each byte over one spot.
		b[0] = (byte)(x); //just the last byte
		
		return b;
	}
	
	/**
	 * Takes an array of integers and returns a byte array of the memory representation.
	 * 
	 * @param x
	 * @return
	 */
	private static byte[] intArrayToBytes(int[] x)
	{
		byte[] b = new byte[x.length * 4];
		byte[] temp;
		int i, z;
		
		for(i = 0; i < x.length; i++)
		{
			temp = intToBytes(x[i]);
			for(z = 0; z < temp.length; z++)
			{
				b[i*temp.length+z] = temp[z];
			}
		}
		return b;
	}
	
	private static byte[] shortArrayToBytes(short[] x)
	{
		byte[] b = new byte[x.length * 2];
		byte[] temp;
		int i, z;
		
		for(i = 0; i < x.length; i++)
		{
			temp = shortToBytes(x[i]);
			for(z = 0; z < temp.length; z++)
			{
				b[i*temp.length+z] = temp[z];
			}
		}
		return b;
	}
	
	private static byte[] shortToBytes(short x)
	{
		byte[] b = new byte[2];
		b[1] = (byte)(x); //bitwise AND with the lower byte
		b[0] = (byte)(x >>> 8) ; //shift four bits to the right and fill with zeros, and then bitwise and with the lower byte
		return b;
	}
	
	private static void errUnsupportedEncoding()
	{
		System.err.print("Unsupported Encoding");
		System.exit(1);
	}
	
	private static void errIO()
	{
		System.err.print("An error occured while writing to file.");
		System.exit(1);
	}
	
	private static byte[] createPadding(int length)
	{
		byte x = (byte)0x00;
		byte[] b = new byte[length];
		if(length == 0)
		{
			return null;
		}
		for(int z = 0; z < b.length; z++)
		{
			b[z] = x;
		}
		
		return b;
	}
	
}
