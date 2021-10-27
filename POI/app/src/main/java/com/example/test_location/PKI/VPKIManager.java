package com.example.test_location.PKI;

import com.example.test_location.models.PeerInfo;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class VPKIManager implements X509TrustManager {
    final private Certificate ca;
    public VPKIManager(Certificate ca) {
        this.ca = ca;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        System.out.println("client checked");
        for (X509Certificate cert : chain) {
            // Make sure that it hasn't expired.
            cert.checkValidity();

            PeerInfo.getInstance().setPublicKey(cert.getPublicKey());
            // Verify the certificate's public key chain.
            try {
                cert.verify(((X509Certificate) ca).getPublicKey());
            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                throw new CertificateException();
            }
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        System.out.println("server checked");
        for (X509Certificate cert : chain) {
            // Make sure that it hasn't expired.
            cert.checkValidity();

            PeerInfo.getInstance().setPeerCerInfo(cert.getSubjectX500Principal().getName());

            PeerInfo.getInstance().setPublicKey(cert.getPublicKey());

            // Verify the certificate's public key chain.
            try {
                cert.verify(((X509Certificate) ca).getPublicKey());
            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                throw new CertificateException();
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
