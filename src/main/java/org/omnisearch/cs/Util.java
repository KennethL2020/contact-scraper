package org.omnisearch.cs;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static boolean isDate(String text) {
        // Regex pattern for detecting dates
        String regex = "\\b\\d{4}[-/\\. ](0[1-9]|1[0-2])[-/\\. ](0[1-9]|[12][0-9]|3[01])\\b";

        // Compile the pattern
        Pattern pattern = Pattern.compile(regex);

        // Create a matcher for the input text
        Matcher matcher = pattern.matcher(text);

        // Find and print all matching dates
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    public static String trimToDomain(String url) {
        try {
            // Add http scheme if missing to parse the URL correctly
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();

            // Check if it's already a domain without any path or query
            if (parsedUrl.getPath().isEmpty() && parsedUrl.getQuery() == null) {
                return host;
            }

            // Return the domain part
            return host;
        } catch (MalformedURLException e) {
            // If the URL is malformed, assume it's a plain domain name
            return url;
        }
    }

    public static boolean isValidLength(String phoneNumber) {
        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        // Check if the total number of digits is between 10 and 15
        int length = digitsOnly.length();
        if (digitsOnly.startsWith("0"))
            return length == 10;
        else if (digitsOnly.startsWith("13"))
            return length == 10 || length == 6;
        else
            return length >= 8 && length <= 11;
    }

    public static String removeDuplicates(String input) {
        if (input == null)
            return "";
        // Split the input string by semicolon
        String[] substrings = input.split(";");

        // Set to store normalized substrings
        Set<String> normalizedSet = new HashSet<>();

        // StringBuilder to build the result
        StringBuilder result = new StringBuilder();

        for (String substring : substrings) {
            // Normalize the substring by removing spaces and punctuations, and converting to lowercase
            String normalized = normalize(substring);

            // Add the original substring if its normalized version is not already in the set
            if (!normalizedSet.contains(normalized)) {
                if (result.length() > 0) {
                    result.append(";");
                }
                result.append(substring);
                normalizedSet.add(normalized);
            }
        }

        return result.toString();
    }
    public static boolean compareLastPart(String url1, String url2) {
        try {
            URL u1 = new URL(url1);
            URL u2 = new URL(url2);

            String path1 = u1.getPath();
            String path2 = u2.getPath();

            // Remove trailing slashes if present
            if (path1.endsWith("/")) {
                path1 = path1.substring(0, path1.length() - 1);
            }
            if (path2.endsWith("/")) {
                path2 = path2.substring(0, path2.length() - 1);
            }

            String lastPart1 = path1.substring(path1.lastIndexOf('/') + 1);
            String lastPart2 = path2.substring(path2.lastIndexOf('/') + 1);

            return lastPart1.equals(lastPart2);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static String removeInvalidNumbers(String input) {
        // Split the input string by semicolon
        String[] substrings = input.split(";");

        // StringBuilder to build the result
        StringBuilder result = new StringBuilder();

        for (String substring : substrings) {

            if (result.length() > 0) {
                result.append(";");
            }

            if (AustralianPhoneNumberValidator.isValidAustralianPhoneNumber(substring.replaceAll("[^\\d]", "")))
                result.append(substring.replaceAll("[^\\d]", ""));
        }

        return result.toString();
    }

    public static String normalize(String str) {
        // Remove all spaces and punctuations, and convert to lowercase
        return str.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
    }

    public static void importCookies(WebDriver driver, String filePath) {
        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(filePath);
            Type cookieType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> cookies = gson.fromJson(reader, cookieType);
            reader.close();

            for (Map<String, Object> cookieMap : cookies) {
                Cookie cookie = new Cookie.Builder((String) cookieMap.get("name"), (String) cookieMap.get("value"))
                        .domain((String) cookieMap.get("domain"))
                        .path((String) cookieMap.get("path"))
                        .isSecure((Boolean) cookieMap.get("secure"))
                        .build();
                driver.manage().addCookie(cookie);
            }
        } catch (Exception e) {
            ErrorLogger.logError(e, Main.DEBUG);
        }
    }
}
