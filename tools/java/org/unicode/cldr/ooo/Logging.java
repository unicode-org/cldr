/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

/**
 * static disgnostic logging class
 */
public class Logging
{
    static final int LOG_LELEL_1 = 0x002;
    static final int LOG_LELEL_2 = 0x004;
    static final int LOG_LELEL_3 = 0x008;
    static final int LOG_LELEL_4 = 0x020;
    
    static private int m_logLevel = 0;
    
    public static void setLevel(boolean bLevel1, boolean bLevel2, boolean bLevel3, boolean bLevel4)
    {
        int level1=0, level2=0, level3=0, level4=0;
        
        if (bLevel1 == true) level1 = 1;
        if (bLevel2 == true) level2 = 2;
        if (bLevel3 == true) level3 = 3;
        if (bLevel4 == true) level4 = 4;
        
        m_logLevel |= (1 << level1);
        m_logLevel |= (1 << level2);
        m_logLevel |= (1 << level3);
        m_logLevel |= (1 << level4);
    }
    
    public static void Log(int level, String msg)
    {
        if ((m_logLevel & level) != 0)
            System.out.println(msg);
        
    }
    
    public static void Log1(String msg)
    {
        String info = "INFO: " + msg;
        Log(LOG_LELEL_1, info);
    }
    
    public static void Log2(String msg)
    {
        String info = "INFO: " + msg;
        Log(LOG_LELEL_2, info);
    }
    
    public static void Log3(String msg)
    {
        String info = "INFO: " + msg;
        Log(LOG_LELEL_3, info);
    }
    
    public static void Log4(String msg)
    {
        String info = "INFO: " + msg;
        Log(LOG_LELEL_4, info);
    }
    
    public static void LogError(String msg)
    {
        String error = "ERROR: " + msg;
        Log(LOG_LELEL_1, error);
    }
    
    public static void LogWarning(String msg)
    {
        String warning = "WARNING: " + msg;
        Log(LOG_LELEL_1, warning);
    }
}
