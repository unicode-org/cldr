/**
 * Copyright (c) 2008 IBM Corporation and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.PrintWriter;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Pooled (data source) listener. Disabled by default. 
 * @author srl
 * @see SurveyMain#doStartupDB
 */
public class STPoolingListener implements ServletContextListener {

    public static final String ST_ATTRIBUTE = "STPool";
    private static final String ST_DATABASE = "jdbc/SurveyTool";

    /**
     * 
     */
    public STPoolingListener() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0) {
        // TODO Auto-generated method stub
        System.err.println("Destroyed Ctx for " + ST_DATABASE);

    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent evt) {
        // from http://onjava.com/lpt/a/6555
        try {
            Context ourContext = (Context) new InitialContext().lookup("java:comp/env");
            DataSource ds = (DataSource) ourContext.lookup(ST_DATABASE);
            evt.getServletContext().setAttribute(ST_ATTRIBUTE, ds);
            if(true==false) {
                ds.setLogWriter(new PrintWriter(System.err));
            }
            System.err.println("Initialized Ctx for " + ST_DATABASE);
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
