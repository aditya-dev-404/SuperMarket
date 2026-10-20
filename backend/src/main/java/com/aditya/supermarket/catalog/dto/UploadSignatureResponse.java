package com.aditya.supermarket.catalog.dto;

public record UploadSignatureResponse(String cloudName, String apiKey, long timestamp,
                                      String folder, String signature) {
}
