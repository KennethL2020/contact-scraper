package org.omnisearch.cs;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataCrawler {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent" +
                "\\Iteration 2\\potential partners\\contact-scraper\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless");
        options.addArguments("--remote-allow-origins=*");
        WebDriver driver = new ChromeDriver(options);

        String companyName = "EDR AUTO HUB";
        String socialMediaLink = "https://www.linkedin.com/company/edr-auto-hub/";
        String industry = "Vehicle Repair and Maintenance";
        String location = "Wetherill Park, New South Wales";

        String websiteUrl = "";
        String phoneNumber = "";
        String addresses = "";

        try {
            // Step 1: Navigate to the company's social media /about/ page
            driver.get(socialMediaLink + "about/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // Store the current URL
            String initialUrl = driver.getCurrentUrl();
            // Wait until the URL changes
            wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(initialUrl)));
            // Store the new URL
            String finalUrl = driver.getCurrentUrl();
            // Wait until the URL no longer changes
            while (!finalUrl.equals(driver.getCurrentUrl())) {
                wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(finalUrl)));
                finalUrl = driver.getCurrentUrl();
            }
            Thread.sleep(3000);

            // Step 2: Extract website URL and phone number
            try {
                WebElement websiteElement = driver.findElement(By.xpath("//dt[contains(@class, 'mb1 text-heading-medium') and text()='Website']/following-sibling::dd//a"));
                websiteUrl = websiteElement.getAttribute("href");
            } catch (Exception e) {
                googleSearch(driver, companyName + " " + industry + " " + location);
                try {
                    WebElement websiteButton = driver.findElement(By.xpath("//a[contains(@class, 'ab_button')]/div[text()='Website']"));
                    websiteUrl = websiteButton.getAttribute("href");
                } catch (Exception ex) {
                    try {
                        WebElement websiteElement = driver.findElement(By.xpath("(//a[@jsname][@href][br][h3])[1]"));
                        websiteUrl = websiteElement.getAttribute("href");
                    } catch (Exception exx) {
                        // No website found
                    }
                }
            }

            try {
                WebElement phoneElement = driver.findElement(By.xpath("//dt[contains(@class, 'mb1 text-heading-medium') and text()='Phone']/following-sibling::dd//span"));
                phoneNumber = phoneElement.getText();
            } catch (Exception e) {
                googleSearch(driver, companyName + " " + industry + " " + location);
                String pageSource = driver.getPageSource();
                phoneNumber = extractPhoneNumber(pageSource);
            }

            // Step 3: Extract address information
            try {
                WebElement locationsHeader = driver.findElement(By.xpath("//h3[contains(@class, 'text-heading-xlarge') and contains(text(), 'Locations')]"));
                if (locationsHeader.getText().contains("(1)")) {
                    WebElement addressElement = locationsHeader.findElement(By.xpath(".//following-sibling::p"));
                    addresses = addressElement.getText();
                } else {
                    List<WebElement> locationElements = driver.findElements(By.xpath("//g[@aria-label[contains(., 'CompanyLocations')]]"));
                    for (WebElement locationElement : locationElements) {
                        locationElement.click();
                        Thread.sleep(2000);
                        List<WebElement> addressElements = driver.findElements(By.xpath("//div[contains(@class, 'org-location-card')]//p"));
                        for (WebElement addressElement : addressElements) {
                            addresses += addressElement.getText() + "; ";
                        }
                        WebElement closeButton = driver.findElement(By.xpath("//button[@aria-label='Close the selected group of locations']"));
                        closeButton.click();
                    }
                }
            } catch (Exception e) {
                // Handle any exceptions related to extracting addresses
            }

            // Step 4: Extract emails and phone numbers from website
            String emailsFromWebsite = "";
            String phonesFromWebsite = "";
            if (!websiteUrl.isEmpty()) {
                googleSearch(driver, "site:" + websiteUrl);
                emailsFromWebsite = extractEmails(driver.getPageSource());
                phonesFromWebsite = extractPhoneNumber(driver.getPageSource());

                driver.get(websiteUrl);
                Thread.sleep(3000);
                List<WebElement> websiteLinks = driver.findElements(By.xpath("//a[contains(@href, websiteUrl)]"));
                for (WebElement link : websiteLinks) {
                    String linkUrl = link.getAttribute("href");
                    if (linkUrl.contains(websiteUrl)) {
                        driver.get(linkUrl);
                        Thread.sleep(2000);
                        String pageSource = driver.getPageSource();
                        emailsFromWebsite += extractEmails(pageSource) + "; ";
                        phonesFromWebsite += extractPhoneNumber(pageSource) + "; ";
                    }
                }
            }

            if (websiteUrl.isEmpty()) {
                googleSearch(driver, "\"" + companyName + " " + industry + " " + location + "\"");
                emailsFromWebsite = extractEmails(driver.getPageSource());
                phonesFromWebsite = extractPhoneNumber(driver.getPageSource());
            }

            System.out.println("Website URL: " + websiteUrl);
            System.out.println("Phone Number: " + phoneNumber);
            System.out.println("Addresses: " + addresses);
            System.out.println("Emails from Website: " + emailsFromWebsite);
            System.out.println("Phones from Website: " + phonesFromWebsite);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static void googleSearch(WebDriver driver, String query) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("window.open('');");
        Set<String> handles = driver.getWindowHandles();
        String newTabHandle = handles.toArray()[handles.size() - 1].toString();
        driver.switchTo().window(newTabHandle);
        driver.get("https://www.google.com");
        WebElement searchBox = driver.findElement(By.name("q"));
        searchBox.sendKeys(query);
        searchBox.sendKeys(Keys.RETURN);
        Thread.sleep(3000);
    }

    private static String extractEmails(String text) {
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = emailPattern.matcher(text);
        StringBuilder emails = new StringBuilder();
        while (matcher.find()) {
            emails.append(matcher.group()).append("; ");
        }
        return emails.toString();
    }

    private static String extractPhoneNumber(String text) {
        Pattern phonePattern = Pattern.compile("\\+?[0-9.\\-\\(\\) ]{7,}");
        Matcher matcher = phonePattern.matcher(text);
        StringBuilder phones = new StringBuilder();
        while (matcher.find()) {
            phones.append(matcher.group()).append("; ");
        }
        return phones.toString();
    }
}
