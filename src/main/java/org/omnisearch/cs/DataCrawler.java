package org.omnisearch.cs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.omnisearch.cs.dto.Company;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataCrawler {
    public static void main(String[] args) {
        String filePath = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent\\Iteration 2\\potential partners\\priority\\automotive_repair.csv";
        List<Company> companies = readCompaniesFromCsv(filePath);
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent" +
                "\\Iteration 2\\potential partners\\contact-scraper\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
        String cookiesJSON = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent\\Iteration 2\\potential partners\\contact-scraper\\src\\main\\resources\\exported-cookies.json";

        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);

        for (Company company : companies) {
            String companyName = company.getName();
            String socialMediaLink = company.getLink();
            String industry = company.getIndustry();
            String location = company.getLocation();

            String websiteUrl = "";
            String phoneNumbers = "";
            String addresses = "";
            String emailAddresses = "";

            String phonesFromWebsite = null;
            String emailsFromWebsite = null;
            try {
                // Step 1: Navigate to the company's social media /about/ page
                driver.get(socialMediaLink + "about/");
                importCookies(driver, cookiesJSON);
                driver.get(socialMediaLink + "about/");
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//            // Store the current URL
//            String initialUrl = driver.getCurrentUrl();
//            // Wait until the URL changes
//            wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(initialUrl)));
//            // Store the new URL
//            String finalUrl = driver.getCurrentUrl();
//            // Wait until the URL no longer changes
//            while (!finalUrl.equals(driver.getCurrentUrl())) {
//                wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(finalUrl)));
//                finalUrl = driver.getCurrentUrl();
//            }
                Thread.sleep(3000);
                scrollToBottom(driver);
                String originalTabHandle = driver.getWindowHandle();

                // Step 2: Extract website URL and phone number
                try {
                    WebElement websiteElement = driver.findElement(By.xpath("//dt[contains(@class, 'mb1 text-heading-medium') and text()='Website']/following-sibling::dd//a"));
                    websiteUrl = websiteElement.getAttribute("href");
                } catch (Exception e) {
                    googleSearch(driver, companyName + " " + industry + " " + location, originalTabHandle);
                    try {
                        WebElement websiteButton = driver.findElement(By.xpath("//a[contains(@class, 'ab_button')]/div[text()='Website']"));
                        websiteUrl = websiteButton.getAttribute("href");
                    } catch (Exception ex) {
                        try {
                            WebElement websiteElement = driver.findElement(By.xpath("(//a[@jsname][@href][br][h3])[1]"));
                            websiteUrl = websiteElement.getAttribute("href");
                        } catch (Exception exx) {
                        }
                    }
                }

                try {
                    WebElement phoneElement = driver.findElement(By.xpath("//dt[contains(@class, 'mb1 text-heading-medium') and text()='Phone']/following-sibling::dd//span"));
                    phoneNumbers = phoneElement.getText();
                } catch (Exception e) {
                    googleSearch(driver, companyName + " " + industry + " " + location, originalTabHandle);
                    String pageSource = driver.getPageSource();
                    phoneNumbers = extractPhoneNumber(pageSource);
                }

                // Step 3: Extract address information
                try {
                    WebElement locationsHeader = driver.findElement(By.xpath("//h3[contains(@class, 'text-heading-xlarge')]"));
                    if (locationsHeader.getText().contains("(1)")) {
                        WebElement addressElement = locationsHeader.findElement(By.xpath(".//following-sibling::p"));
                        addresses = addressElement.getText();
                    } else {
                        List<WebElement> locationElements = driver.findElements(By.xpath("//*[contains(@aria-label, 'CompanyLocations') and @role='region']"));
                        for (WebElement locationElement : locationElements) {
                            int index = 0;
                            List<WebElement> pathElements = locationElement.findElements(By.tagName("path"));
                            List<String> ariaLabels = pathElements.stream().map(element -> element.getAttribute("aria-label")).collect(Collectors.toList());
                            for (WebElement pathElement : pathElements) {
                                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                                if (index > 0) {
                                    wait.until(ExpectedConditions.stalenessOf(pathElement));
                                    String xpath = String.format("//*[contains(@aria-label, '%s')]", ariaLabels.get(index));
                                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
                                    pathElement = driver.findElement(By.xpath(xpath));
                                }
                                if (pathElement.getAttribute("aria-label").contains("CompanyLocations")) {
                                    pathElement.click();
                                    Thread.sleep(2000);
                                    List<WebElement> addressElements = driver.findElements(By.xpath("//div[contains(@class, 'org-location-card')]//p"));
                                    for (WebElement addressElement : addressElements) {
                                        addresses += addressElement.getText() + "; ";
                                    }
                                    WebElement closeButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@aria-label='Close the selected group of locations']")));
                                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", locationsHeader);
                                    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, -150)");
                                    Thread.sleep(500);
                                    closeButton.click();
                                }
                                index++;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Step 4: Extract emails and phone numbers from website
                emailsFromWebsite = "";
                phonesFromWebsite = "";
                if (!websiteUrl.isEmpty()) {
                    googleSearch(driver, "site:" + trimToDomain(websiteUrl), originalTabHandle);
                    emailsFromWebsite = extractEmails(driver.getPageSource());
                    phonesFromWebsite = extractPhoneNumber(driver.getPageSource());

                    driver.get(websiteUrl);
                    Thread.sleep(3000);
                    scrollToBottom(driver);
                    String pageSource = driver.getPageSource();
                    emailsFromWebsite += extractEmails(pageSource) + "; ";
                    phonesFromWebsite += extractPhoneNumber(pageSource) + "; ";
                    List<WebElement> websiteLinks = driver.findElements(By.xpath("//a[contains(@href, websiteUrl)]"));
                    for (WebElement link : websiteLinks) {
                        String linkUrl = link.getAttribute("href");
                        if (linkUrl.contains(websiteUrl)) {
                            driver.get(linkUrl);
                            Thread.sleep(2000);
                            scrollToBottom(driver);
                            pageSource = driver.getPageSource();
                            emailsFromWebsite += extractEmails(pageSource) + "; ";
                            phonesFromWebsite += extractPhoneNumber(pageSource) + "; ";
                        }
                    }
                }
                googleSearch(driver, companyName + " " + industry + " " + location, originalTabHandle);
                emailsFromWebsite += extractEmails(driver.getPageSource()) + "; ";
                phonesFromWebsite += extractPhoneNumber(driver.getPageSource()) + "; ";

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                phoneNumbers += phonesFromWebsite;
                emailAddresses += emailsFromWebsite;
                company.setEmailAddresses(removeDuplicates(emailAddresses));
                company.setPhoneNumbers(removeDuplicates(phoneNumbers));
                company.setWebsites(removeDuplicates(websiteUrl));
                company.setAddresses(removeDuplicates(addresses));
                System.out.println(String.format("\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\""
                        , company.getName(), company.getLink(), company.getIndustry()
                        , company.getLocation(), company.getFollowers()
                        , company.getPhoneNumbers(), company.getEmailAddresses()
                        , company.getWebsites(), company.getAddresses()));
            }
        }
        driver.quit();
    }

    private static void googleSearch(WebDriver driver, String query, String originalTabHandle) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("window.open('');");
        Set<String> handles = driver.getWindowHandles();
        String newTabHandle = handles.toArray()[handles.size() - 1].toString();
        driver.switchTo().window(newTabHandle);
        driver.get("https://www.google.com");
        WebElement searchBox = driver.findElement(By.name("q"));
        searchBox.sendKeys(query);
        searchBox.sendKeys(Keys.RETURN);
        Thread.sleep(3000);
        scrollToBottom(driver);
        driver.close();
        driver.switchTo().window(originalTabHandle);
    }

    private static String extractEmails(String text) {
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(?!jpg|jpeg|png|gif|pdf|doc|docx|xls|xlsx|zip|rar|mp3|mp4|avi|mkv)[A-Za-z]{2,}\\b");
        Matcher matcher = emailPattern.matcher(text);
        StringBuilder emails = new StringBuilder();
        while (matcher.find()) {
            emails.append(matcher.group()).append("; ");
        }
        return emails.toString();
    }

    private static String extractPhoneNumber(String text) {
        Pattern phonePattern = Pattern.compile("\\b(\\+61|0)?[\\s-]?(\\d{1,4})[\\s-]?(\\d{2,4})[\\s-]?(\\d{2,4})[\\s-]?(\\d{0,4})\\b");
        Matcher matcher = phonePattern.matcher(text);
        StringBuilder phones = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            if (isValidLength(match) && !isDate(match))
                phones.append(match).append("; ");
        }
        return phones.toString();
    }

    private static void importCookies(WebDriver driver, String filePath) {
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
            e.printStackTrace();
        }
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

    private static void scrollToBottom(WebDriver driver) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(2000); // wait for 2 seconds to ensure all content is loaded
    }

    private static boolean isValidLength(String phoneNumber) {
        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");

        // Check if the total number of digits is between 10 and 15
        int length = digitsOnly.length();
        if (digitsOnly.startsWith("0"))
            return length == 10;
        else if (digitsOnly.startsWith("61"))
            return length >= 10 && length <= 11;
        else if (digitsOnly.startsWith("13"))
            return length == 10 || length == 6;
        else
            return length == 8;
    }

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

    public static String removeDuplicates(String input) {
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

    private static String normalize(String str) {
        // Remove all spaces and punctuations, and convert to lowercase
        return str.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
    }

    public static List<Company> readCompaniesFromCsv(String filePath) {
        List<Company> companies = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            String[] values;
            // Skip the header
            csvReader.readNext();
            while ((values = csvReader.readNext()) != null) {
                Company company = new Company();
                if (!values[0].isEmpty()) company.setName(values[0]);
                if (!values[1].isEmpty()) company.setLink(values[1]);
                if (!values[2].isEmpty()) company.setIndustry(values[2]);
                if (!values[3].isEmpty()) company.setLocation(values[3]);
                if (!values[4].isEmpty()) company.setFollowers(Long.parseLong(values[4]));
                companies.add(company);
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return companies;
    }

}
