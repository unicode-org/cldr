/**
 * Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
 */
//##header J2SE15

package org.unicode.cldr.unittest.web;

import java.io.File;

import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp.ConnectionFactory;
import org.apache.tomcat.dbcp.dbcp.DriverManagerConnectionFactory;
import org.apache.tomcat.dbcp.dbcp.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.dbcp.PoolingDataSource;
import org.apache.tomcat.dbcp.pool.KeyedObjectPool;
import org.apache.tomcat.dbcp.pool.KeyedObjectPoolFactory;
import org.apache.tomcat.dbcp.pool.ObjectPool;
import org.apache.tomcat.dbcp.pool.impl.GenericKeyedObjectPool;
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.web.CLDRProgressIndicator;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;
import com.ibm.icu.dev.test.TestLog;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {

  private static final String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String DERBY_PREFIX="jdbc:derby:";

public static void main(String[] args) {
    new TestAll().run(args);
  }

  public TestAll() {
    super(
            new String[] {
            		// use class.getName so we are in sync with name changes and removals (if not additions)
            	TestIntHash.class.getName(),
            	TestXPathTable.class.getName(),
            	TestCacheAndDataSource.class.getName()
            },
    "All tests in CLDR Web");
  }

  public static final String CLASS_TARGET_NAME  = "CLDR.Web";

  /**
   * 
   * @author srl
   * @see TestInfo
   */
  public static class WebTestInfo {
    private static WebTestInfo INSTANCE = null;
   
    private SupplementalDataInfo supplementalDataInfo;
    private StandardCodes sc;
    private Factory cldrFactory;
    private CLDRFile english;
    private CLDRFile root;
    private RuleBasedCollator col;

    public static WebTestInfo getInstance() {
      synchronized (WebTestInfo.class) {
        if (INSTANCE == null) {
          INSTANCE = new WebTestInfo();
        }
      }
      return INSTANCE;
    }

    private WebTestInfo() {}

    public SupplementalDataInfo getSupplementalDataInfo() {
        synchronized(this) {
          if (supplementalDataInfo == null) {
            supplementalDataInfo = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
          }
        }
        return supplementalDataInfo;
      }
      public StandardCodes getStandardCodes() {
        synchronized(this) {
          if (sc == null) {
            sc = StandardCodes.make();
          }
        }
        return sc;
      }
      public Factory getCldrFactory() {
        synchronized(this) {
          if (cldrFactory == null) {
            cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
          }
        }
        return cldrFactory;
      }
      public CLDRFile getEnglish() {
        synchronized(this) {
          if (english == null) {
            english = getCldrFactory().make("en", true);
          }
        }
        return english;
      }
      public CLDRFile getRoot() {
        synchronized(this) {
          if (root == null) {
            root = getCldrFactory().make("root", true);
          }
        }
        return root;
      }
      public Collator getCollator() {
        synchronized(this) {
          if (col == null) {
            col = (RuleBasedCollator) Collator.getInstance();
            col.setNumericCollation(true);
          }
        }
        return col;
      }
  }
  
  static boolean dbSetup = false;
  /**
   * Set up the CLDR db
   */
  public synchronized static void setupTestDb() {
	  if(dbSetup==false) {
		  DBUtils.makeInstanceFrom(getDataSource());
		  dbSetup=true;
	  }
  }
  
  public static void shutdownDb() throws SQLException {
	  setupTestDb();
	  DBUtils.getInstance().doShutdown();
  }
  
  public static final String CLDR_WEBTEST_DIR = "cldr_webtest_dir";
  static File baseDir = null;
  public static File getBaseDir() {
	  if(baseDir==null) {
		  String where = System.getProperty(CLDR_WEBTEST_DIR, System.getProperty("user.home")+File.separator+"cldr_db_test");
		  baseDir = new File(where);
		  if(!baseDir.exists()) {
			  baseDir.mkdir();
		  }
		  if(!baseDir.isDirectory()) {
			  throw new IllegalArgumentException("Bad dir ["+CLDR_WEBTEST_DIR+"]: " + baseDir.getAbsolutePath());
		  }
		  System.err.println("Note: using test dir ["+CLDR_WEBTEST_DIR+"]: "+baseDir.getAbsolutePath());
	  }
	  return baseDir;
  }
  public static File getDir(String forWhat) {
	  return new File(getBaseDir(),forWhat);
  }
  public static File getEmptyDir(String forWhat) {
	  return emptyDir(getDir(forWhat));
  }
  public static File emptyDir(File dir) {
	  if(dir.isDirectory()) {
	      File cachedBFiles[] = dir.listFiles();
	      if(cachedBFiles != null) {
	          for(File f : cachedBFiles) {
	              if(f.isFile()) {
	                  f.delete();
	              }
	          }
	      }
	  } else {
		  dir.mkdir();
	  }
      return dir;
  }
  
  static DataSource getDataSource() {
	  System.err.println();
	  System.err.println("DB setup");
	  try {
		Class.forName(DERBY_DRIVER);
	  } catch (ClassNotFoundException e) {
		throw new RuntimeException(e);
	  }
	  
	  return setupDerbyDataSource( getDir("db"));
  }
  
  // from http://svn.apache.org/viewvc/commons/proper/dbcp/trunk/doc/ManualPoolingDataSourceExample.java?view=co
  public static DataSource setupDerbyDataSource(File theDir) {
	  String connectURI = DERBY_PREFIX+theDir.getAbsolutePath();
	  ObjectPool connectionPool = new GenericObjectPool(null);
	  
	  if(!theDir.exists()) {
		  System.err.println("Using new: " + theDir.getAbsolutePath() + " baseDir = " + getBaseDir().getAbsolutePath());

		  String createURI = connectURI+";create=true";
		  try {	
			  new DriverManagerConnectionFactory(createURI,null).createConnection().close();
		  } catch (SQLException e) {
			  System.err.println("Error on connect to " + createURI + " - "+ DBUtils.unchainSqlException(e));
		  }
		  System.err.println("Connect/close to " + createURI);
	  } else {
		  System.err.println("Using existing: " + theDir.getAbsolutePath() + " baseDir = " + getBaseDir().getAbsolutePath());
	  }
	  Properties props = new Properties();
	  props.put("poolPreparedStatements","true");
	  props.put("maxOpenPreparedStatements","150");
	  /*
	   * 			            maxActive="8"
			            maxIdle="4"
			removeAbandoned="true"
			                        removeAbandonedTimeout="60"
			                    logAbandoned="true"
			defaultAutoCommit="false"
			poolPreparedStatements="true"
			maxOpenPreparedStatements="150"

	   */
	  ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI,props);
	  PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,new KeyedObjectPoolFactory(){

		@Override
		public KeyedObjectPool createPool() throws IllegalStateException {
			// TODO Auto-generated method stub
			return new GenericKeyedObjectPool();
		}}
	  		,null,false,true);
	  PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
	  System.err.println("New datasource off and running: " + connectURI);
	  return dataSource;
  }
  
  public static CLDRProgressIndicator getProgressIndicator(TestLog t) {
	  final TestLog test = t;
	  return new CLDRProgressIndicator(){

			@Override
			public CLDRProgressTask openProgress(String what) {
				// TODO Auto-generated method stub
				return openProgress(what,0);
			}

			@Override
			public CLDRProgressTask openProgress(String what, int max) {
				// TODO Auto-generated method stub
				final String whatP = what;
				return new CLDRProgressTask(){

					@Override
					public void close() {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void update(int count) {
						update(count,"");
						
					}

					@Override
					public void update(int count, String what) {
						test.logln(whatP+" Update: "+what+", "+count);
					}

					@Override
					public void update(String what) {
						update(0,what);
					}

					@Override
					public long startTime() {
						// TODO Auto-generated method stub
						return 0;
					}};
			}};
  }

}
