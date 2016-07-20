package com.arthas.volleyrequest.ssl;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;

@SuppressWarnings("all")
class SslSocketFactory extends SSLSocketFactory {

    public SslSocketFactory(InputStream keyStore, String keyStorePassword) throws GeneralSecurityException {
        super(createSSLContext(keyStore, keyStorePassword), STRICT_HOSTNAME_VERIFIER);
//        setHostnameVerifier(new AllowAllHostnameVerifier());
    }

    private static SSLContext createSSLContext(InputStream keyStore, String keyStorePassword) throws GeneralSecurityException {
        SSLContext sslcontext;
        try {
            sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[]{new SsX509TrustManager(keyStore, keyStorePassword)}, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Failure initializing default SSL context", e);
        }
        return sslcontext;
    }

}    