/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.treasuredata.client.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.treasuredata.client.TDClient;
import com.treasuredata.client.TDClientConfig;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An abstraction of TD API request, which will be translated to Jetty's http client request.
 * We need this abstraction to create multiple http request objects upon API call retry since
 * Jetty's Request instances are not reusable.
 */
public class ApiRequest
{
    public static class Builder
    {
        private static final Map<String, String> EMPTY_MAP = ImmutableMap.of();

        private HttpMethod method;
        private String uri;
        private Map<String, String> queryParams;
        private Map<String, String> headerParams;

        Builder(HttpMethod method, String uri)
        {
            this.method = method;
            this.uri = uri;
        }

        public static Builder GET(String uri)
        {
            return new Builder(HttpMethod.GET, uri);
        }

        public static Builder POST(String uri)
        {
            return new Builder(HttpMethod.POST, uri);
        }

        public Builder addHeader(String key, String value)
        {
            if(headerParams == null) {
                headerParams = new HashMap<>();
            }
            headerParams.put(urlEncode(key), urlEncode(value));
            return this;
        }

        public Builder addQueryParam(String key, String value)
        {
            if(queryParams == null) {
                queryParams = new HashMap<>();
            }
            queryParams.put(key, value);
            return this;
        }

        public ApiRequest build()
        {
            return new ApiRequest(
                    method,
                    uri,
                    queryParams != null ? queryParams : EMPTY_MAP,
                    headerParams != null ? headerParams : EMPTY_MAP);
        }
    }

    public static String urlEncode(String value)
    {
        try {
            return URLEncoder.encode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }


    private final HttpMethod method;
    private final String uri;
    private final Map<String, String> queryParams;
    private final Map<String, String> headerParams;

    ApiRequest(HttpMethod method, String uri, Map<String, String> queryParams, Map<String, String> headerParams)
    {
        this.method = checkNotNull(method, "method is null");
        this.uri = checkNotNull(uri, "uri is null");
        this.queryParams = checkNotNull(queryParams, "queryParms is null");
        this.headerParams = checkNotNull(headerParams, "headerParams is null");
    }

    public String getUri() {
        return uri;
    }

    public Request newJettyRequest(HttpClient client, TDClientConfig config)
    {
        String queryStr = "";
        String requestUri = uri;
        if (!queryParams.isEmpty()) {
            List<String> queryParamList = new ArrayList<String>(queryParams.size());
            for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
                queryParamList.add(String.format("%s=%s", queryParam.getKey(), queryParam.getValue()));
            }
            queryStr = Joiner.on("&").join(queryParamList);
            if (method == HttpMethod.GET) {
                requestUri += "?" + queryStr;
            }
        }
        Request request = client.newRequest(requestUri);
        request.method(method);
        request.agent("TDClient " + TDClient.getVersion());
        request.header("Authorization", "TD1 " + config.getApiKey());
        for (Map.Entry<String, String> entry : headerParams.entrySet()) {
            request.header(entry.getKey(), entry.getValue());
        }
        String dateHeader = setDateHeader(request);
        if (method == HttpMethod.POST) {
            if (queryStr != null) {
                request.content(new StringContentProvider(queryStr), "application/x-www-form-urlencoded");
            }
            else {
                request.header("Content-Length", "0");
            }
        }
        return request;
    }

    private static final ThreadLocal<SimpleDateFormat> RFC2822_FORMAT =
            new ThreadLocal<SimpleDateFormat>()
            {
                @Override
                protected SimpleDateFormat initialValue()
                {
                    return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                }
            };

    private static String setDateHeader(Request request)
    {
        Date currentDate = new Date();
        String dateHeader = RFC2822_FORMAT.get().format(currentDate);
        request.header("Date", dateHeader);
        return dateHeader;
    }

    private static final ThreadLocal<MessageDigest> SHA1 =
            new ThreadLocal<MessageDigest>()
            {
                @Override
                protected MessageDigest initialValue()
                {
                    try {
                        return MessageDigest.getInstance("SHA-1");
                    }
                    catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("SHA-1 digest algorithm must be available but not found", e);
                    }
                }
            };
    private static final char[] hexChars = new char[16];

    static {
        for (int i = 0; i < 16; i++) {
            hexChars[i] = Integer.toHexString(i).charAt(0);
        }
    }

    @VisibleForTesting
    static String sha1HexFromString(String string)
    {
        MessageDigest sha1 = SHA1.get();
        sha1.reset();
        sha1.update(string.getBytes());
        byte[] bytes = sha1.digest();

        // convert binary to hex string
        char[] array = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = (int) bytes[i];
            array[i * 2] = hexChars[(b & 0xf0) >> 4];
            array[i * 2 + 1] = hexChars[b & 0x0f];
        }
        return new String(array);
    }
}
