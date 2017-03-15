/*
 ******************************************************************************
 * Copyright (C) 2004-2013, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.unicode.cldr.util.Pair;

import com.ibm.icu.text.CompactDecimalFormat;

/**
 * Reads 'chunked' format as follows:
 *
 * <RECORDSEP>record header\n <fieldsep><fieldname1>field1 ...\n ... \n
 * <fieldsep><fieldname2>field2 ...\n ... \n <fieldsep><fieldname3>field3\n
 * <RECORDSEP>record header\n ...
 *
 *
 * @author srl
 *
 */
public class ChunkyReader implements Runnable {
    private String recordSep;
    private File fileName;
    private String fieldSep;

    public class Entry implements Comparable<ChunkyReader.Entry>, JSONString {
        private long time = -1;

        private StringBuilder header = new StringBuilder();

        /**
         * @return the header
         */
        public String getHeader() {
            return header.toString();
        }

        /**
         * @param header
         *            the header to set
         */
        private void addHeader(String header) {
            this.header.append(header);
        }

        /**
         * @param time
         *            the time to set
         */
        private void setTime(long time) {
            this.time = time;
        }

        private void addField(String field, String value) {
            fields.add(new Pair<String, String>(field, value));
        }

        private List<Pair<String, String>> fields = new ArrayList<Pair<String, String>>();

        public long getTime() {
            return time;
        }

        @Override
        public int compareTo(Entry o) {
            // descending order
            if (this.time == o.time) {
                return 0;
            } else if (this.time < o.time) {
                return -1;
            } else {
                return 1;
            }
        }

        public String toString() {
            return recordSep + " " + getTime() + " - " + new Date(getTime()) + " - " + getHeader();
        }

        @Override
        public String toJSONString() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("time", getTime());
            o.put("header", getHeader());
            JSONArray a = new JSONArray();
            for (Pair<String, String> e : fields) {
                String f = e.getFirst();
                if (f.equals("DATE") || f.equals("LOGSITE") || f.equals("CTX") || f.equals("UPTIME")) {
                    o.put(f, e.getSecond());
                } else {
                    a.put(new JSONObject().put(f, e.getSecond()));
                }
            }
            o.put("fields", a);
            return o.toString();
        }
    }

    InputStreamReader reader = null;

    long lastModTime = -1;
    long lastSize = -1;
    boolean stale = true;
    private String dateField;

    ChunkyReader(File fileName, String recordSep, String fieldSep, String dateField) {
        this.fileName = fileName;
        this.recordSep = recordSep;
        this.fieldSep = fieldSep;
        this.dateField = dateField;

        init();
    }

    private void init() {
        nudge();

        SurveyMain.addPeriodicTask(this);
    }

    @Override
    public void run() {
        nudge();
    }

    /**
     * Check everything again.
     */
    public synchronized void nudge() {
        if (!fileName.canRead()) {
            stale = true; // it's been deleted
            lastModTime = -1;
            lastSize = -1;
            return;
        }

        long nextModTime = fileName.lastModified();
        long nextSize = fileName.length();

        if (nextModTime != lastModTime || nextSize != lastSize) {
            stale = true;
        }
        lastModTime = nextModTime;
        lastSize = nextSize;
    }

    private Map<Long, Entry> cache = new TreeMap<Long, Entry>(new Comparator<Long>() {

        @Override
        public int compare(Long arg0, Long arg1) {
            return arg1.compareTo(arg0);
        }
    });

    public long getLastTime() throws IOException {
        Entry lastItem = getLastEntry();
        if (lastItem != null) {
            return lastItem.getTime();
        }
        return -1;
    }

    public synchronized Entry getLastEntry() throws IOException {
        return getEntryBelow(Long.MAX_VALUE); // get highest numbered item
    }

    private static final long tooBig = 32768000;

    public synchronized Entry getEntryBelow(long value) throws IOException {
        nudge();
        if (stale) {
            reader = null;
            cache.clear();
        }

        // System.err.println("Looking for entry below " + value);

        for (Entry e : cache.values()) {
            // System.err.println("CR: Considering: " + e.getTime());
            if (e.getTime() < value) {
                return e;
            }
        }

        if (!stale) {
            // System.err.println("CR: Nothing found before " + value);
            return null; // sorry
        }
        BufferedReader br = null;
        InputStream file = null;
        try {
            cache.clear();
            file = new FileInputStream(fileName);
            InputStreamReader reader = new InputStreamReader(file, "UTF-8");
            //Map<String, String> entry = new TreeMap<String, String>();
            if (fileName.length() > tooBig) {
                long skipThis = fileName.length() - tooBig;
                file.skip(skipThis);

                Entry fakeEntry = new Entry();
                fakeEntry.setTime(System.currentTimeMillis() + 1000);
                fakeEntry.addHeader("Logfile too big - skipping the first " + bigNum(skipThis) + " bytes");
                cache.put(fakeEntry.getTime(), fakeEntry);
            }
            br = new BufferedReader(reader, 65536);
            String line = null;
            Entry e = null;
            String lastField = null;
            StringBuilder lastFieldValue = new StringBuilder();
            int lineno = 0;
            while ((line = br.readLine()) != null) {
                lineno++;
                // System.err.println("CR>> " + line);
                if (line.startsWith(recordSep)) {
                    e = new Entry();
                    e.addHeader(line.substring(recordSep.length()));
                    lastField = null;
                } else if (e == null) {
                    continue; // skip junk line
                } else if (line.startsWith(fieldSep)) {
                    if (lastField != null) {
                        e.addField(lastField, lastFieldValue.toString());
                    }
                    lastFieldValue.setLength(0);
                    lastField = null;

                    line = line.substring(fieldSep.length());
                    String splits[] = line.split(" ");
                    if (splits != null && splits.length > 0) {
                        lastField = splits[0];
                    } else {
                        throw new IllegalArgumentException("Can't read " + fileName.getAbsolutePath() + ":" + lineno
                            + " - bad field string " + line);
                    }
                    line = line.substring(lastField.length());
                    if (line.startsWith(" ")) {
                        line = line.substring(1);
                    }
                    lastFieldValue.append(line);
                    if (lastField.equals(dateField)) {
                        if (splits.length > 1) {
                            Long theTime;
                            try {
                                theTime = Long.parseLong(splits[1]);
                            } catch (NumberFormatException nf) {
                                theTime = null;
                            }
                            // System.err.println("e="+e);
                            // System.err.println("splits="+splits);
                            // System.err.println("splits[1]="+splits[1]);
                            // System.err.println("theTime="+theTime);
                            if (theTime == null) {
                                System.err.println("Skipping bad time " + splits[1] + " of " + line);
                                continue;
                            }
                            e.setTime(theTime); // time
                            cache.put(e.getTime(), e);
                            // System.err.println("** CR: Got " + e);
                        }
                    }
                } else if (lastField == null) {
                    e.addHeader(line);
                } else {
                    lastFieldValue.append('\n').append(line);
                }
            }
            br.close();
            if (e != null && lastField != null) { // get the lastone.
                e.addField(lastField, lastFieldValue.toString());
            }
            stale = false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            if (br != null) br.close();
            if (file != null) file.close();
        }
        for (Entry e : cache.values()) {
            if (e.getTime() < value) {
                return e;
            }
        }

        return null;
    }

    private static String bigNum(long skipThis) {
        try {
            return CompactDecimalFormat.getInstance().format(skipThis);
        } catch (Throwable t) {
            System.err.println("err using CDF " + t);
            t.printStackTrace();
            return Long.toString(skipThis);
        }
    }
}
