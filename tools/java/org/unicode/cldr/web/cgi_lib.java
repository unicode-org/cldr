package org.unicode.cldr.web;
import java.util.*;
import java.io.*;

/**
 * Original Author: Pat L. Durante 
 * Source: JavaWorld, January 1997. Copyright (C) 1997 JavaWorld.com, an IDG Company
 * http://www.javaworld.com/javaworld/jw-01-1997/jw-01-cgiscripts-p2.html
 * 
 * Used by permission from Jennifer Orr, Senior Editor at JavaWorld 3/30/2005
 * "You have our permission to use the code as long as you attribute it to 
 *  the author as the creator and to JavaWorld as the original publisher. 
 *  Also, please include the article's URL in the acknowledgment."
 *
 * Some modifications by Steven R. Loomis, srl@icu-project.org  to form handling code
 *  and the myGetProperty function.
 */

/**
 * cgi_lib.java<p>
 *
 * <p>
 *
 * Usage:  This library of java functions, which I have encapsulated inside
 *         a class called cgi_lib as class (static) member functions,
 *         attempts to duplicate the standard PERL CGI library (cgi-lib.pl).
 *
 *         You must invoke any Java program that uses this library from
 *         within a UNIX script, Windows batch file or equivalent.  As you
 *         will see in the following example, all of the CGI environment
 *         variables must be passed from the script into the Java application
 *         using the -D option of the Java interpreter.  This example
 *         UNIX script uses the "main" routine of this class as a
 *         CGI script:
 *
 * <pre>
 * (testcgi.sh)
 *
 * #!/bin/sh
 *
 * java \
 *  -Dcgi.content_type=$CONTENT_TYPE \
 *  -Dcgi.content_length=$CONTENT_LENGTH \
 *  -Dcgi.request_method=$REQUEST_METHOD \
 *  -Dcgi.query_string=$QUERY_STRING \
 *  -Dcgi.server_name=$SERVER_NAME \
 *  -Dcgi.server_port=$SERVER_PORT \
 *  -Dcgi.script_name=$SCRIPT_NAME \
 *  -Dcgi.path_info=$PATH_INFO \
 * cgi_lib
 *
 * </pre>
 *
 *         Question and comments can be sent to pldurante@tasc.com.<p>
 *
 * @version 1.0
 * @author Pat L. Durante
 *
 */
class cgi_lib
{

   /**
    * Added by Steven R. Loomis <srl@icu-project.org> 
    * translate between -D format CGI parameters, and normal shell parameters.
    *
    * Also, return empty string "" instead of null.
    */
    
   static String myGetProperty(String what)
    {
      if(false) {
        if(what.equals("QUERY_STRING")) {
            what = "cgi.query_string";
        } else if(what.equals("REQUEST_METHOD")) {
            what = "cgi.request_method";
        } else if(what.equals("CONTENT_LENGTH")) {
            what = "cgi.content_length";
        } else if(what.equals("CONTENT_TYPE")) {
            what = "cgi.content_type";
        } else if(what.equals("SERVER_PORT")) {
            what = "cgi.server_port";
        } else if(what.equals("SCRIPT_NAME")) {
            what = "cgi.script_name";
        } else if(what.equals("PATH_INFO")) {
            what = "cgi.path_info";
        } else if(what.equals("SERVER_NAME")) {
            what = "cgi.server_name";
        }
     }

        String x = System.getProperty(what);
        if(x == null) {
          //  System.err.println("[[ empty prop: " + what + "]]");
            return "";
        } else {
            return x;
        }
    }
  /**
   *
   * Parse the form data passed from the browser into
   * a Hashtable.  The names of the input fields on the HTML form will
   * be used as the keys to the Hashtable returned.  If you have a form
   * that contains an input field such as this,<p>
   *
   * <pre>
   *   &ltINPUT SIZE=40 TYPE="text" NAME="email" VALUE="pldurante@tasc.com"&gt
   * </pre>
   *
   * then after calling this method like this,<p>
   *
   * <pre>
   *    Hashtable form_data = cgi_lib.ReadParse(System.in);
   * </pre>
   *
   * you can access that email field as follows:<p>
   *
   * <pre>
   *    String email_addr = (String)form_data.get("email");
   * </pre>
   *
   * @param inStream The input stream from which the form data can be read.
   * (Only used if the form data was posted using the POST method. Usually,
   * you will want to simply pass in System.in for this parameter.)
   *
   * @return The form data is parsed and returned in a Hashtable
   * in which the keys represent the names of the input fields.
   *
   */
  public static Hashtable ReadParse(InputStream inStream)
  {
      Hashtable form_data = new Hashtable();

      String inBuffer = "";

      if (MethGet())
      {
          inBuffer = myGetProperty("QUERY_STRING");
      }
      else
      {
          //
          //  TODO:  I should probably use the cgi.content_length property when
          //         reading the input stream and read only that number of
          //         bytes.  The code below does not use the content length
          //         passed in through the CGI API.
          //
            DataInput d = new DataInputStream(inStream);
            Integer len = new
            Integer(System.getProperty("CONTENT_LENGTH"));
            byte [] bs = new byte[len.intValue()];
            try {
            /*
            while((line = d.readLine()) != null) {
            inBuffer = inBuffer + line; }
            */
            d.readFully(bs);
            inBuffer = new String(bs, 0);
            } catch (IOException ignored) { }
      }

      //
      //  Split the name value pairs at the ampersand (&)
      //
      StringTokenizer pair_tokenizer = new StringTokenizer(inBuffer,"&");

      while (pair_tokenizer.hasMoreTokens())
      {
          String pair = urlDecode(pair_tokenizer.nextToken());
          //
          // Split into key and value
          //
          StringTokenizer keyval_tokenizer = new StringTokenizer(pair,"=");
          String key = new String();
          String value = new String();
          if (keyval_tokenizer.hasMoreTokens())
            key = keyval_tokenizer.nextToken();
          else ; // ERROR - shouldn't ever occur
          if (keyval_tokenizer.hasMoreTokens())
            value = keyval_tokenizer.nextToken();
          else ; // ERROR - shouldn't ever occur
          //
          // Add key and associated value into the form_data Hashtable
          //
          form_data.put(key,value);
      }

      return form_data;

  }

  /**
   *
   * URL decode a string.<p>
   *
   * Data passed through the CGI API is URL encoded by the browser.
   * All spaces are turned into plus characters (+) and all "special"
   * characters are hex escaped into a %dd format (where dd is the hex
   * ASCII value that represents the original character).  You probably
   * won't ever need to call this routine directly; it is used by the
   * ReadParse method to decode the form data.
   *
   * @param in The string you wish to decode.
   *
   * @return The decoded string.
   *
   */

  public static String urlDecode(String in)
  {
      StringBuffer out = new StringBuffer(in.length());
      int i = 0;
      int j = 0;

      while (i < in.length())
      {
         char ch = in.charAt(i);
         i++;
         if (ch == '+') ch = ' ';
         else if (ch == '%')
         {
            ch = (char)Integer.parseInt(in.substring(i,i+2), 16);
            i+=2;
         }
         out.append(ch);
         j++;
      }
      return new String(out);
  }

  /**
   *
   * Generate a standard HTTP HTML header.
   *
   * @return A String containing the standard HTTP HTML header.
   *
   */
  public static String Header()
  {
      return "Content-type: text/html\n\n";
  }

  /**
   *
   * Generate some vanilla HTML that you usually
   * want to include at the top of any HTML page you generate.
   *
   * @param Title The title you want to put on the page.
   *
   * @return A String containing the top portion of an HTML file.
   *
   */
  public static String HtmlTop(String Title)
  {
      String Top = new String();
      Top = "<html>\n";
      Top+= "<head>\n";
      Top+= "<title>\n";
      Top+= Title;
      Top+= "\n";
      Top+= "</title>\n";
      Top+= "</head>\n";
      Top+= "<body>\n";

      return Top;

  }

  /**
   *
   * Generate some vanilla HTML that you usually
   * want to include at the bottom of any HTML page you generate.
   *
   * @return A String containing the bottom portion of an HTML file.
   *
   */
  public static String HtmlBot()
  {
      return "</body>\n</html>\n";
  }

  /**
   *
   * Determine if the REQUEST_METHOD used to
   * send the data from the browser was the GET method.
   *
   * @return true, if the REQUEST_METHOD was GET.  false, otherwise.
   *
   */
  public static boolean MethGet()
  {
     String RequestMethod = myGetProperty("REQUEST_METHOD");
     boolean returnVal = false;

     if (RequestMethod != null)
     {
         if (RequestMethod.equals("GET") ||
             RequestMethod.equals("get"))
         {
             returnVal=true;
         }
     }
     return returnVal;
  }

  /**
   *
   * Determine if the REQUEST_METHOD used to
   * send the data from the browser was the POST method.
   *
   * @return true, if the REQUEST_METHOD was POST.  false, otherwise.
   *
   */
  public static boolean MethPost()
  {
     String RequestMethod = myGetProperty("REQUEST_METHOD");
     boolean returnVal = false;

     if (RequestMethod != null)
     {
         if (RequestMethod.equals("POST") ||
             RequestMethod.equals("post"))
         {
             returnVal=true;
         }
     }
     return returnVal;
  }

  /**
   *
   * Determine the Base URL of this script.
   * (Does not include the QUERY_STRING (if any) or PATH_INFO (if any).
   *
   * @return The Base URL of this script as a String.
   *
   */
  public static String MyBaseURL()
  {
      String returnString = new String();
      returnString = "http://" +
                     myGetProperty("SERVER_NAME");
      if (!(myGetProperty("SERVER_PORT").equals("80")))
          returnString += ":" + myGetProperty("SERVER_PORT");
      returnString += myGetProperty("SCRIPT_NAME");

      return returnString;
  }
  
  public static String MyTinyURL()
  {
    return myGetProperty("SCRIPT_NAME");
  }

  /**
   *
   * Determine the Full URL of this script.
   * (Includes the QUERY_STRING (if any) or PATH_INFO (if any).
   *
   * @return The Full URL of this script as a String.
   *
   */
  public static String MyFullURL()
  {
      String returnString;
      returnString = MyBaseURL();
      returnString += myGetProperty("PATH_INFO");
      String queryString = myGetProperty("QUERY_STRING");
      if (queryString.length() > 0)
         returnString += "?" + queryString;
      return returnString;
  }

  /**
   *
   * Neatly format all of the CGI environment variables
   * and the associated values using HTML.
   *
   * @return A String containing an HTML representation of the CGI environment
   * variables and the associated values.
   *
   */
  public static String Environment()
  {
      String returnString;

      returnString = "<dl compact>\n";
      returnString += "<dt><b>CONTENT_TYPE</b> <dd>:<i>" +
                      myGetProperty("CONTENT_TYPE") +
                      "</i>:<br>\n";
      returnString += "<dt><b>CONTENT_LENGTH</b> <dd>:<i>" +
                      myGetProperty("CONTENT_LENGTH") +
                      "</i>:<br>\n";
      returnString += "<dt><b>REQUEST_METHOD</b> <dd>:<i>" +
                      myGetProperty("REQUEST_METHOD") +
                      "</i>:<br>\n";
      returnString += "<dt><b>QUERY_STRING</b> <dd>:<i>" +
                      myGetProperty("QUERY_STRING") +
                      "</i>:<br>\n";
      returnString += "<dt><b>SERVER_NAME</b> <dd>:<i>" +
                      myGetProperty("SERVER_NAME") +
                      "</i>:<br>\n";
      returnString += "<dt><b>SERVER_PORT</b> <dd>:<i>" +
                      myGetProperty("SERVER_PORT") +
                      "</i>:<br>\n";
      returnString += "<dt><b>SCRIPT_NAME</b> <dd>:<i>" +
                      myGetProperty("SCRIPT_NAME") +
                      "</i>:<br>\n";
      returnString += "<dt><b>PATH_INFO</b> <dd>:<i>" +
                      myGetProperty("PATH_INFO") +
                      "</i>:<br>\n";

      returnString += "</dl>\n";

      return returnString;
  }

  /**
   *
   * Neatly format all of the form data using HTML.
   *
   * @param form_data The Hashtable containing the form data which was
   * parsed using the ReadParse method.
   *
   * @return A String containing an HTML representation of all of the 
   * form variables and the associated values.
   *
   */
  public static String Variables(Hashtable form_data)
  {

      String returnString;

      returnString = "<dl compact>\n";

      for (Enumeration e = form_data.keys() ; e.hasMoreElements() ;)
      {
          String key = (String)e.nextElement();
          String value = (String)form_data.get(key);
          returnString += "<dt><b>" + key + "</b> <dd>:<i>" +
                          value +
                          "</i>:<br>\n";
      } 

      returnString += "</dl>\n";

      return returnString;

  }

  /**
   *
   * The main routine is included here as a test CGI script to
   * demonstrate the use of all of the methods provided above.
   * You can use it to test your ability to execute a CGI script written
   * in Java.  See the sample UNIX script file included above to see
   * how you would invoke this routine.<p>
   *
   * Please note that this routine references the member functions directly
   * (since they are in the same class), but you would have to
   * reference the member functions using the class name prefix to
   * use them in your own CGI application:<p>
   * <pre>
   *     System.out.println(cgi_lib.HtmlTop());
   * </pre>
   *
   * @param args An array of Strings containing any command line
   * parameters supplied when this program in invoked.  Any
   * command line parameters supplied are ignored by this routine.
   *
   */
  public static void main( String args[] )
  {

      //
      // This main program is simply used to test the functions in the
      // cgi_lib class.  
      //
      // That said, you can use this main program as a test cgi script.  All
      // it does is echo back the form inputs and enviroment information to
      // the browser.  Use the testcgi UNIX script file to invoke it.  You'll
      // notice that the script you use to invoke any Java application that
      // uses the cgi_lib functions MUST pass all the CGI enviroment variables
      // into it using the -D parameter.  See testcgi for more details.
      //

      //
      // Print the required CGI header.
      //
      System.out.println(Header());

      //
      // Create the Top of the returned HTML
      // page (the parameter becomes the title).
      //
      System.out.println(HtmlTop("Hello World"));
      System.out.println("<hr>");

      //
      // Determine the request method used by the browser.
      //
      if (MethGet())
          System.out.println("REQUEST_METHOD=GET");
      if (MethPost())
          System.out.println("REQUEST_METHOD=POST");
      System.out.println("<hr>");

      //
      // Determine the Base URL of this script.
      //
      System.out.println("Base URL: " + MyBaseURL());
      System.out.println("<hr>");

      //
      // Determine the Full URL used to invoke this script.
      //
      System.out.println("Full URL: " + MyFullURL());
      System.out.println("<hr>");

      //
      //  Print all the CGI environment variables
      //  (usually only used while testing CGI scripts).
      //
      //
      System.out.println(Environment());
      System.out.println("<hr>");

      //
      //  Parse the form data into a Hashtable.
      //
      Hashtable form_data = ReadParse(System.in);

      //
      //  Print out each of the name/value pairs
      //  from sent from the browser.
      //
      System.out.println(Variables(form_data));
      System.out.println("<hr>");

      //
      //  Access a particular form value.
      //  (This assumes the form contains a "name" input field.)
      //
      String name = (String)form_data.get("name");
      System.out.println("Name=" + name);
      System.out.println("<hr>");

      //
      // Create the Bottom of the returned HTML page - which closes it cleanly.
      //
      System.out.println(HtmlBot());

  }
}

