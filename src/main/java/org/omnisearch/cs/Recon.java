package org.omnisearch.cs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

public class Recon {
    public static final String js = "let csvContent = \"Text,Link,Industry,Location,Followers\\n\";\n" +
            "\n" +
            "[...document.querySelectorAll('li.reusable-search__result-container')].forEach(li => {\n" +
            "    let aElement = li.querySelector('a.app-aware-link:not(.scale-down)');\n" +
            "    let industryLocationElement = li.querySelector('div.entity-result__primary-subtitle.t-14.t-black.t-normal');\n" +
            "    let followersElement = li.querySelector('div.entity-result__secondary-subtitle.t-14.t-normal');\n" +
            "\n" +
            "    if (aElement && industryLocationElement && followersElement) {\n" +
            "        let textContent = '';\n" +
            "        let href = aElement.getAttribute('href');\n" +
            "        let nodes = Array.from(aElement.childNodes);\n" +
            "\n" +
            "        for (let i = 0; i < nodes.length; i++) {\n" +
            "            let node = nodes[i];\n" +
            "            if (node.nodeType === Node.COMMENT_NODE && node.nodeValue.trim() === '') {\n" +
            "                if (nodes[i + 1] && nodes[i + 1].nodeType === Node.TEXT_NODE) {\n" +
            "                    textContent = nodes[i + 1].textContent.trim();\n" +
            "                    break;\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        let industryLocationContent = '';\n" +
            "        let industryLocationNodes = Array.from(industryLocationElement.childNodes);\n" +
            "\n" +
            "        for (let i = 0; i < industryLocationNodes.length; i++) {\n" +
            "            let node = industryLocationNodes[i];\n" +
            "            if (node.nodeType === Node.COMMENT_NODE && node.nodeValue.trim() === '') {\n" +
            "                if (industryLocationNodes[i + 1] && industryLocationNodes[i + 1].nodeType === Node.TEXT_NODE) {\n" +
            "                    industryLocationContent = industryLocationNodes[i + 1].textContent.trim();\n" +
            "                    break;\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        let industry = '';\n" +
            "        let location = '';\n" +
            "\n" +
            "        if (industryLocationContent.includes('•')) {\n" +
            "            [industry, location] = industryLocationContent.split('•').map(s => s.trim());\n" +
            "        }\n" +
            "\n" +
            "        let followersContent = '';\n" +
            "        let followersNodes = Array.from(followersElement.childNodes);\n" +
            "\n" +
            "        for (let i = 0; i < followersNodes.length; i++) {\n" +
            "            let node = followersNodes[i];\n" +
            "            if (node.nodeType === Node.COMMENT_NODE && node.nodeValue.trim() === '') {\n" +
            "                if (followersNodes[i + 1] && followersNodes[i + 1].nodeType === Node.TEXT_NODE) {\n" +
            "                    followersContent = followersNodes[i + 1].textContent.trim();\n" +
            "                    break;\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        // Extract only the number from the followersContent\n" +
            "        let followersNumber = followersContent.match(/\\d+/) ? followersContent.match(/\\d+/)[0] : '';\n" +
            "\n" +
            "        if (textContent) { // Only add entry if textContent is not empty\n" +
            "            csvContent += `\"${textContent}\",\"${href}\",\"${industry}\",\"${location}\",\"${followersNumber}\"\\n`;\n" +
            "        }\n" +
            "    }\n" +
            "});\n" +
            "\n" +
            "console.log(csvContent);\n";
    private static final String URL = "https://www.linkedin.com/search/results/companies/?companyHqGeo=%5B%22104769905%22%5D&companySize=%5B%22B%22%5D&industryCompanyVertical=%5B%2280%22%5D&keywords=";
    private static final String URL_SUFFIX = "&origin=FACETED_SEARCH&sid=Z~i";
    public static void main(String args[]){
        // Set the path to your WebDriver executable (e.g., chromedriver)
        System.setProperty("webdriver.chrome.driver", Main.WEBDRIVER_PATH);

        // Initialize WebDriver (using ChromeDriver in this example)
        WebDriver driver = new ChromeDriver();

        // Cast WebDriver to JavascriptExecutor
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

        try {
            // Iterate through the English alphabet
            for (char letter = 'A'; letter <= 'Z'; letter++) {
                // Construct the URL with the keyword parameter set to the current letter
                String url = URL + letter + URL_SUFFIX;
                Util.importCookies(driver, Main.LINKEDIN_COOKIES);
                // Navigate to the constructed URL
                driver.get(url);

                // Execute the JavaScript and get the result
                String result = (String) jsExecutor.executeScript(js);

                // Print the result of the JavaScript execution
                System.out.println("Keyword: " + letter + ", Result: \n" + result);
            }
        } finally {
            // Close the browser
            driver.quit();
        }
    }
}
