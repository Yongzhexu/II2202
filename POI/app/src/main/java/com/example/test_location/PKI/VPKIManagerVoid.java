package com.example.test_location.PKI;

import javax.net.ssl.X509TrustManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class VPKIManagerVoid implements X509TrustManager {

    public VPKIManagerVoid() {

    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509Certificate cert : chain) {
/*
            // Make sure that it hasn't expired.
            cert.checkValidity();

            // Verify the certificate's public key chain.
            try {
                cert.verify(((X509Certificate) ca).getPublicKey());
            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                throw new CertificateException();
            }

 */
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509Certificate cert : chain) {
/*
            // Make sure that it hasn't expired.
            cert.checkValidity();

            // Verify the certificate's public key chain.
            try {
                System.out.println(cert.getSignature().length);

                cert.verify(((X509Certificate) ca).getPublicKey());
            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                throw new CertificateException();
            }

 */
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
