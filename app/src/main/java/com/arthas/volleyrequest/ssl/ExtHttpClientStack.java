package com.arthas.volleyrequest.ssl;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.HttpStack;
import com.arthas.volleyrequest.VolleyRequest;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpDelete;
import cz.msebera.android.httpclient.client.methods.HttpEntityEnclosingRequestBase;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpPut;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.params.HttpParams;

@SuppressWarnings("all")
public class ExtHttpClientStack implements HttpStack {

    private final static String HEADER_CONTENT_TYPE = "Content-Type";
    protected final HttpClient mClient;

    public ExtHttpClientStack(SslHttpClient client) {
        mClient = client;
    }

    private static void addHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            httpRequest.setHeader(key, headers.get(key));
        }
    }

    private static List<NameValuePair> getPostParameterPairs(Map<String, String> postParams) {
        List<NameValuePair> result = new ArrayList<>(postParams.size());
        for (String key : postParams.keySet()) {
            result.add(new BasicNameValuePair(key, postParams.get(key)));
        }
        return result;
    }

    /**
     * Creates the appropriate subclass of HttpUriRequest for passed in request.
     */
    static HttpUriRequest createHttpRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws AuthFailureError {
        switch (request.getMethod()) {
            case Method.DEPRECATED_GET_OR_POST: {
                // This is the deprecated way that needs to be handled for backwards compatibility.
                // If the request's post body is null, then the assumption is that the request is
                // GET.  Otherwise, it is assumed that the request is a POST.
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    HttpPost postRequest = new HttpPost(request.getUrl());
                    postRequest.addHeader(HEADER_CONTENT_TYPE, request.getPostBodyContentType());
                    HttpEntity entity;
                    entity = new ByteArrayEntity(postBody);
                    postRequest.setEntity(entity);
                    return postRequest;
                } else {
                    return new HttpGet(request.getUrl());
                }
            }
            case Method.GET:
                return new HttpGet(request.getUrl());
            case Method.DELETE:
                return new HttpDelete(request.getUrl());
            case Method.POST: {
                HttpPost postRequest = new HttpPost(request.getUrl());
                postRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
                setEntityIfNonEmptyBody(postRequest, request);
                return postRequest;
            }
            case Method.PUT: {
                HttpPut putRequest = new HttpPut(request.getUrl());
                putRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
                setEntityIfNonEmptyBody(putRequest, request);
                return putRequest;
            }
            default:
                throw new IllegalStateException("Unknown request method.");
        }
    }

    private static void setEntityIfNonEmptyBody(HttpEntityEnclosingRequestBase httpRequest, Request<?> request) throws AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            HttpEntity entity = new ByteArrayEntity(body);
            httpRequest.setEntity(entity);
        }
    }

    @Override
    public org.apache.http.HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        HttpUriRequest httpRequest = createHttpRequest(request, additionalHeaders);
        addHeaders(httpRequest, additionalHeaders);
        addHeaders(httpRequest, request.getHeaders());
        onPrepareRequest(httpRequest);
        HttpParams httpParams = httpRequest.getParams();
        int timeoutMs = request.getTimeoutMs();
        // data collection and possibly different for wifi vs. 3G.
        HttpConnectionParams.setConnectionTimeout(httpParams, VolleyRequest.TIMEOUT_VALUE);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);

        HttpResponse resp = mClient.execute(httpRequest);

        return convertResponseNewToOld(resp);
    }

    private org.apache.http.HttpResponse convertResponseNewToOld(HttpResponse resp)
            throws IllegalStateException, IOException {
        ProtocolVersion protocolVersion = new ProtocolVersion(resp.getProtocolVersion()
                .getProtocol(),
                resp.getProtocolVersion().getMajor(),
                resp.getProtocolVersion().getMinor());

        StatusLine responseStatus = new BasicStatusLine(protocolVersion,
                resp.getStatusLine().getStatusCode(),
                resp.getStatusLine().getReasonPhrase());

        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        org.apache.http.HttpEntity ent = convertEntityNewToOld(resp.getEntity());
        response.setEntity(ent);

        for (Header h : resp.getAllHeaders()) {
            org.apache.http.Header header = convertHeaderNewToOld(h);
            response.addHeader(header);
        }
        return response;
    }

    private org.apache.http.HttpEntity convertEntityNewToOld(HttpEntity ent)
            throws IllegalStateException, IOException {
        BasicHttpEntity ret = new BasicHttpEntity();
        if (ent != null) {
            ret.setContent(ent.getContent());
            ret.setContentLength(ent.getContentLength());
            Header h;
            h = ent.getContentEncoding();
            if (h != null) {
                ret.setContentEncoding(convertHeaderNewToOld(h));
            }
            h = ent.getContentType();
            if (h != null) {
                ret.setContentType(convertHeaderNewToOld(h));
            }
        }

        return ret;
    }

    private org.apache.http.Header convertHeaderNewToOld(Header header) {
        return new BasicHeader(header.getName(), header.getValue());
    }

    /**
     * Called before the request is executed using the underlying HttpClient.
     * Overwrite in subclasses to augment the request.
     */
    protected void onPrepareRequest(HttpUriRequest request) throws IOException {
        // Nothing.
    }

}
