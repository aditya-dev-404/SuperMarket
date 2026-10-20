package com.aditya.supermarket.catalog.service;

import com.aditya.supermarket.catalog.config.CloudinaryProperties;
import com.aditya.supermarket.catalog.dto.UploadSignatureResponse;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.vendor.entity.Vendor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class CloudinarySignatureService {

    private final CloudinaryProperties props;

    public UploadSignatureResponse sign(Vendor vendor) {
        if (isBlank(props.getCloudName()) || isBlank(props.getApiKey()) || isBlank(props.getApiSecret())) {
            throw new BusinessRuleException("Image upload is not configured");
        }
        long timestamp = Instant.now().getEpochSecond();
        String folder = props.getFolder() + "/vendor-" + vendor.getId();
        // Cloudinary signs alphabetically sorted params: folder, timestamp
        String toSign = "folder=" + folder + "&timestamp=" + timestamp + props.getApiSecret();
        return new UploadSignatureResponse(props.getCloudName(), props.getApiKey(),
            timestamp, folder, sha1Hex(toSign));
    }

    public boolean isOurUrl(String url) {
        return url != null && url.startsWith("https://res.cloudinary.com/" + props.getCloudName() + "/");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String sha1Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
