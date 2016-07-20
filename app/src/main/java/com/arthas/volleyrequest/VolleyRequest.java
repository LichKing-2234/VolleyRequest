package com.arthas.volleyrequest;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.arthas.volleyrequest.ssl.ExtHttpClientStack;
import com.arthas.volleyrequest.ssl.SslHttpClient;
import com.arthas.volleyrequest.utils.LogUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;

@SuppressWarnings("all")
public class VolleyRequest {

    // 超时时间
    public static int TIMEOUT_VALUE = 20000;
    // 重试请求次数
    public static int RETRY_TIME = 0;

    private static RequestQueue requestQueue;
    private static VolleyRequest manager;

    private static RequestKey key;

    private VolleyRequest() {
    }

    public static void init(Context application, RequestKey key) {
        init(application, null, null, key);
    }

    public static void init(Context application, Integer sslResId, String passWord, RequestKey requestKey) {
        InputStream keyStore = null;
        if (sslResId != null) {
            keyStore = application.getResources().openRawResource(sslResId);
        }
        if (keyStore == null) {
            requestQueue = Volley.newRequestQueue(application);
        } else {
            requestQueue = Volley.newRequestQueue(application,
                    new ExtHttpClientStack(new SslHttpClient(keyStore, passWord)));
        }
        key = requestKey;
    }

    public static <T> void httpPost(String url, Map<String, String> params, Map<String, String> headers,
                                    ResponseListener<T> listener) {
        httpRequest(Request.Method.POST, url, params, headers, listener);
    }

    public static <T> void httpGet(String url, Map<String, String> params, Map<String, String> headers,
                                   ResponseListener<T> listener) {
        httpRequest(Request.Method.GET, url, params, headers, listener);
    }

    private static <T> void httpRequest(int method, String url, Map<String, String> params,
                                        Map<String, String> headers, ResponseListener<T> listener) {
        if (requestQueue == null) {
            throw new RuntimeException("VolleyRequest must init in your application before use it!");
        }

        DataRequest dataRequest = new DataRequest(method, url, params, headers, listener);
        dataRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_VALUE, RETRY_TIME,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(dataRequest);
    }

    private static class DataRequest extends StringRequest {

        private Map<String, String> params, headers;

        public <T> DataRequest(int method, final String url, final Map<String, String> params,
                               final Map<String, String> headers, final ResponseListener<T> listener) {
            super(method, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    logRequest(url, params, headers, response, false);

                    if (listener != null) {
                        Type type = listener.getTypeToken().getType();
                        try {
                            JSONObject json = new JSONObject(response);
                            if (key.successCode == json.getInt(key.codeName)) {
                                String data = json.getString(key.dataName);
                                T t;
                                if (String.class.equals(type)) {
                                    t = (T) data;
                                } else {
                                    t = new Gson().fromJson(data, type);
                                }
                                listener.onSuccess(t);
                            } else {
                                listener.onFail(json.getInt(key.codeName), json.getString(key.msgName));
                            }
                        } catch (JSONException e) {
                            if (String.class.equals(type)) {
                                listener.onSuccess((T) response);
                            } else {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    logRequest(url, params, headers, error.toString(), true);

                    if (listener != null) {
                        listener.onError(error);
                    }
                }
            });
            this.params = params;
            this.headers = headers;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            if (headers != null) {
                return headers;
            }
            return super.getHeaders();
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            if (params != null) {
                return params;
            }
            return super.getParams();
        }
    }

    /**
     * 打印请求记录
     */
    private static void logRequest(String url, Map<String, String> params, Map<String, String> headers,
                                   String response, boolean isError) {
        LogUtils.i("url: " + url);
        LogUtils.i("params: " + (params == null ? "null" : params.toString()));
        LogUtils.i("headers: " + (headers == null ? "null" : headers.toString()));
        if (isError) {
            LogUtils.e("error: " + response);
        } else {
            LogUtils.d("response: " + response);
        }
    }

    /**
     * 网络请求回调
     */
    public interface ResponseListener<T> {
        void onSuccess(T data);

        void onFail(int code, String msg);

        void onError(VolleyError error);

        TypeToken<T> getTypeToken();
    }

    public static class RequestKey {
        private String codeName;
        private int successCode;
        private String msgName;
        private String dataName;

        private RequestKey() {
        }

        public RequestKey(String codeName, int successCode, String msgName, String dataName) {
            this.codeName = codeName;
            this.successCode = successCode;
            this.msgName = msgName;
            this.dataName = dataName;
        }
    }

}
