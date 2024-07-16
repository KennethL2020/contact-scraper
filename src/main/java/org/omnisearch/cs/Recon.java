package org.omnisearch.cs;

import com.opencsv.CSVWriter;
import org.omnisearch.cs.dto.Company;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class Recon {
    public static final String js = "let csvContent = \"\";\n" +
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
            "return csvContent;\n";
    private static final String URL = "https://www.linkedin.com/search/results/companies/?companyHqGeo=%5B%22104769905%22%5D&companySize=%5B%22B%22%5D&industryCompanyVertical=%5B%22126%22%5D&keywords=";
    private static final String URL_SUFFIX = "&origin=GLOBAL_SEARCH_HEADER&sid=M%2C1";
    private static final boolean COMPLETE = false;
    public static void main(String args[]){
        // Set the path to your WebDriver executable (e.g., chromedriver)
        System.setProperty("webdriver.chrome.driver", Main.WEBDRIVER_PATH);

        // Initialize WebDriver (using ChromeDriver in this example)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://www.linkedin.com/");
        } catch (TimeoutException e){
            ErrorLogger.logError(e, Main.DEBUG);
        }
        try {
            Util.importCookies(driver, Main.LINKEDIN_COOKIES);
        } catch (Exception e){
            ErrorLogger.logError(e, Main.DEBUG);
        }
        try {
            driver.get("https://www.linkedin.com/");
        } catch (TimeoutException e){
            ErrorLogger.logError(e, Main.DEBUG);
        }

        try {
            // Iterate through the English alphabet
            for (char letter = 'A'; letter <= 'Z'; letter++) {
                // Construct the URL with the keyword parameter set to the current letter
                if (COMPLETE)
                    letter = '\0';
                String url = URL + letter + URL_SUFFIX;
                // Navigate to the constructed URL
                driver.get(url);
                Thread.sleep(3000);
                try {
                    scrollToBottom(driver);
                } catch (TimeoutException | InterruptedException e){
                    ErrorLogger.logError(e, Main.DEBUG);
                }
                int pages = 1;
                try {
                    pages = findLargestPaginationValue(driver);
                } catch (Exception e){
                    ErrorLogger.logError(e, Main.DEBUG);
                }
                for (int i = 1; i <= pages; i++){
                    url = URL + letter + "&page=" + i + URL_SUFFIX;
                    driver.get(url);
                    // Execute the JavaScript and get the result
                    // Cast WebDriver to JavascriptExecutor
                    JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
                    String result = (String) jsExecutor.executeScript(js);

                    // Print the result of the JavaScript execution
                    if (!removeHeader(result).isEmpty())
                        result = removeHeader(result);
                    System.out.println(result);
                    logResult(result);
                }
                if (COMPLETE)
                    break;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // Close the browser
            driver.quit();
        }
    }

    public static int findLargestPaginationValue(WebDriver driver) {
        // Find all button elements with aria-label containing 'Page'
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> buttonElements = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//button[contains(@aria-label, 'Page')]")));
        // List<WebElement> buttonElements = driver.findElements(By.xpath("//button[contains(@aria-label, 'Page')]"));

        int maxPaginationNumber = 1;

        for (WebElement buttonElement : buttonElements) {
            try {
                // Find the span element contained by the button element
                WebElement spanElement = buttonElement.findElement(By.tagName("span"));

                // Extract the number inside the span element
                String spanText = spanElement.getText();
                int paginationNumber = Integer.parseInt(spanText);

                // Update the max pagination number if the current value is larger
                if (paginationNumber > maxPaginationNumber) {
                    maxPaginationNumber = paginationNumber;
                }
            } catch (NoSuchElementException e) {
                // Handle the exception if no span element is found
                System.err.println("No span element found in button element: " + buttonElement);
            } catch (NumberFormatException e) {
                // Handle the exception if the span text does not contain a valid integer
                System.err.println("Invalid number in span element: " + buttonElement);
            }
        }

        return maxPaginationNumber;
    }
    public static String removeHeader(String csvContent) {
        // Split the CSV content into lines
        String[] lines = csvContent.split("\n");

        // Iterate through the lines to find and remove the header
        StringBuilder result = new StringBuilder();
        boolean headerRemoved = false;

        for (String line : lines) {
            if (!headerRemoved && line.contains("Text,Link,Industry,Location,Followers")) {
                headerRemoved = true;
                continue; // Skip this line (header line)
            }
            result.append(line).append("\n");
        }

        // Convert StringBuilder to String and trim any trailing newline
        return result.toString().trim();
    }
    private static void scrollToBottom(WebDriver driver) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
        //Thread.sleep(2000); // wait for 2 seconds to ensure all content is loaded
    }
    private static void logResult(String result) {
        boolean fileExists = new File(Main.RECON_PATH).exists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Main.RECON_PATH, true))) {
            // Write header if file does not exist
            if (!fileExists) {
                writer.write(String.join(",",Main.HEADERS) + "\n");
            }
            writer.write(result + "\n");
        } catch (IOException e) {
            ErrorLogger.logError(e, Main.DEBUG);
        }
    }
}
