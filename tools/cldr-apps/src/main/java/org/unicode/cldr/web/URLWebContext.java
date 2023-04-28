//
//  URLWebContext.java
//  sea-eel
//
//  Created by Steven R. Loomis on 05/06/2007.
//  Copyright 2007 IBM. All rights reserved.
//

// a WebContext based upon a url, for testing purposes

package org.unicode.cldr.web;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class URLWebContext extends WebContext {
    URL url;
    Map<String, String> params = new HashMap<>();

    public URLWebContext(URLWebContext other) {
        super(other);
        url = other.url;
        params = other.params; // assume readonly
    }

    /*
    public URLWebContext(String url) throws IOException, java.net.MalformedURLException {
        super(true);
        setURL(url);
    }
    */

    public void setURL(String urlString) throws java.net.MalformedURLException {
        this.url = new URL(urlString);
        String work = this.url.getQuery(); // .replaceAll("+"," ");
        if (work == null) return;
        if (work.length() == 0) return;
        String splits[] = work.split("&");
        if (splits != null) {
            for (String q : splits) {
                String n[] = q.split("=");
                System.err.println(n[0] + " == " + n[1]);
                params.put(n[0], n[1]);
            }
        }
    }

    // imps

    @Override
    public Map<String, String> getParameterMap() {
        return params;
        // throw new InternalError("unsupported");
    }

    @Override
    public boolean hasField(String x) {
        return params.get(x) != null;
    }

    @Override
    String userIP() {
        return "127.0.0.1";
    }

    @Override
    String serverName() {
        return SurveyMain.localhost();
    }

    @Override
    String serverHostport() {
        int port = url.getPort();
        if (port == url.getDefaultPort()) {
            return serverName();
        } else {
            return serverName() + ":" + port;
        }
    }

    @Override
    String schemeHostPort() {
        return url.getProtocol() + "://" + serverHostport();
    }

    @Override
    public String context() {
        return "/cldr-apps";
    }

    @Override
    public String field(String x, String def) {
        String res = params.get(x);
        if (res == null) {
            return def;
        } else {
            return res;
        }
    }

    @Override
    public String base() {
        return context() + "/survey";
    }

    @Override
    public Object clone() {
        return new URLWebContext(this);
    }
}
