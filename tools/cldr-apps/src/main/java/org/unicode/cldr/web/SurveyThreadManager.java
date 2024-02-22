/** 2010-2021 */
package org.unicode.cldr.web;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.hc.core5.concurrent.DefaultThreadFactory;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;

public class SurveyThreadManager {
    static final Logger logger = SurveyLog.forClass(SurveyThreadManager.class);
    private static final String JAVA_COMP_DEFAULT_MANAGED_THREAD_FACTORY =
            "java:comp/DefaultManagedThreadFactory";
    private static final String DEFAULT_MANAGED_EXECUTOR =
            "java:comp/DefaultManagedExecutorService";
    private static final String DEFAULT_MANAGED_SCHEDULED_EXECUTOR =
            "java:comp/DefaultManagedScheduledExecutorService";

    private static ThreadFactory gFactory = null;
    private static ExecutorService gExecutor = null;
    private static ScheduledExecutorService gScheduledExecutor = null;

    /**
     * Get our instance of ThreadFactory
     *
     * @return
     */
    public static ThreadFactory getThreadFactory() {
        if (gFactory == null) {
            if (CLDRConfig.getInstance().getEnvironment() == Environment.UNITTEST) {
                // Allow this to run in unit tests
                System.err.println(
                        "SurveyThread: in UNITTEST, spinning up a new DefaultThreadFactory");
                gFactory = new DefaultThreadFactory("SurveyThread");
            } else {
                InitialContext context = DBUtils.getInitialContext();
                ManagedThreadFactory managedThreadFactory = null;
                try {
                    managedThreadFactory =
                            (ManagedThreadFactory)
                                    context.lookup(JAVA_COMP_DEFAULT_MANAGED_THREAD_FACTORY);
                } catch (NamingException e) {
                    SurveyMain.busted(
                            "Could not look up " + JAVA_COMP_DEFAULT_MANAGED_THREAD_FACTORY, e);
                    throw new RuntimeException(e);
                }
                logger.finer("SurveyThread: got ManagedThreadFactory: " + managedThreadFactory);
                gFactory = managedThreadFactory;
            }
        }
        return gFactory;
    }

    /**
     * Get our instance of ExecutorService
     *
     * @return
     */
    public static ExecutorService getExecutorService() {
        if (gExecutor == null) {
            if (CLDRConfig.getInstance().getEnvironment() == Environment.UNITTEST) {
                gExecutor = Executors.newCachedThreadPool(getThreadFactory());
            } else {
                InitialContext context = DBUtils.getInitialContext();
                ManagedExecutorService service = null;
                try {
                    service = (ManagedExecutorService) context.lookup(DEFAULT_MANAGED_EXECUTOR);
                } catch (NamingException e) {
                    SurveyMain.busted("Could not look up " + DEFAULT_MANAGED_EXECUTOR, e);
                    throw new RuntimeException(e);
                }
                logger.finer("SurveyThread: got ManagedExecutorService: " + service);
                gExecutor = service;
            }
        }
        return gExecutor;
    }

    /**
     * Get our instance of ScheduledExecutorService
     *
     * @return
     */
    public static ScheduledExecutorService getScheduledExecutorService() {
        if (gScheduledExecutor == null) {
            if (CLDRConfig.getInstance().getEnvironment() == Environment.UNITTEST) {
                gScheduledExecutor = Executors.newScheduledThreadPool(2);
            } else {
                InitialContext context = DBUtils.getInitialContext();
                ManagedScheduledExecutorService service = null;
                try {
                    service =
                            (ManagedScheduledExecutorService)
                                    context.lookup(DEFAULT_MANAGED_SCHEDULED_EXECUTOR);
                } catch (NamingException e) {
                    SurveyMain.busted("Could not look up " + DEFAULT_MANAGED_SCHEDULED_EXECUTOR, e);
                    throw new RuntimeException(e);
                }
                logger.finer("SurveyThread: got ManagedScheduledExecutorService: " + service);
                gScheduledExecutor = service;
            }
        }
        return gScheduledExecutor;
    }

    public void shutdown() {
        logger.finer("SurveyThreadManager: The container should manage all threads.");
        gExecutor = null;
        gFactory = null;
        gScheduledExecutor = null;
    }
}
