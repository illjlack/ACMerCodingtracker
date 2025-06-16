package com.codingtracker.crawler;

import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AtCoderCrawler {
    public static Map<String, String> parseCookies(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return Map.of();
        }

        return Arrays.stream(cookieHeader.split(";"))
                .map(String::trim)
                .filter(s -> s.contains("="))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }
}