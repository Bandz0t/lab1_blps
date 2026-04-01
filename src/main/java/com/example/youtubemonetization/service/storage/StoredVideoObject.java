package com.example.youtubemonetization.service.storage;

import java.io.InputStream;

public record StoredVideoObject(InputStream stream, String contentType, long size) {
}
