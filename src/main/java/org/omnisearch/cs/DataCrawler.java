package org.omnisearch.cs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import org.omnisearch.cs.dto.Company;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataCrawler implements Runnable{
    private static final Object lock = new Object();
    private WebDriver driver;
    private List<Company> companies;
    private int fails = 0;
    public DataCrawler(List<Company> companies){
        System.setProperty("webdriver.chrome.driver", Main.WEBDRIVER_PATH);
        this.companies = companies;
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");
        options.addArguments("--no-sandbox");
        options.addArguments("--user-data-dir="+Main.USER_DATA);
        options.addArguments("--user-agent="+Main.USER_AGENT);
        //options.addArguments("--profile-directory="+Main.PROFILE);
        if (!Main.DEBUG)
            options.addArguments("--headless");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS); // 10 seconds timeout
    }
    public void run() {
        driver.manage().window().setSize(new Dimension(1440, 900));
        System.out.println(driver.manage().window().getSize());
        for (Company company : companies) {
            if (fails > 10) {
                System.out.println("Please renew cookies");
                break;
            }
            long startTime = System.nanoTime();
            crawl(company);
            long endTime = System.nanoTime();
            long duration = endTime - startTime; // in nanoseconds
            if (Main.KEEP_TIME)
                System.out.println("Execution time: " + (duration / 1000000000) + " seconds");
        }
        driver.quit();
    }
    private void addCookiesThroughString(WebDriver driver, String allCookies){
        String[] cookiesArray = allCookies.split("; ");

        // Iterate over each cookie string
        for (String cookieString : cookiesArray) {
            // Split the cookie string into name and value
            String[] cookieNameValue = cookieString.split("=", 2);
            if (cookieNameValue.length == 2) {
                String name = cookieNameValue[0];
                String value = cookieNameValue[1].replace("\"", ""); // Remove any surrounding quotes

                // Create a new cookie and add it to the driver
                Cookie cookie = new Cookie(name, value);
                driver.manage().addCookie(cookie);
            }
        }
    }
    private void crawl(Company company){
        String companyName = company.getName();
        String socialMediaLink = company.getLink();
        String industry = company.getIndustry();
        String location = company.getLocation();

        String websiteUrl = "";
        String phoneNumbers = "";
        String addresses = "";
        String emailAddresses = "";

        String phonesFromWebsite = "";
        String emailsFromWebsite = "";

        boolean writeToFile = true;
        try {
            // Step 1: Navigate to the company's social media /about/ page
            try {
                driver.get(socialMediaLink + "about/");
            } catch (TimeoutException e){
                ErrorLogger.logError(e, Main.DEBUG);
            }
            try {
                //Util.importCookies(driver, Main.LINKEDIN_COOKIES);
                //Util.importCookies(driver, Main.GOOGLE_COOKIES);
                //addCookiesThroughString(driver, Main.ADDITIONAL_COOKIES);
            } catch (Exception e){
                ErrorLogger.logError(e, Main.DEBUG);
            }
            try {
                driver.get(socialMediaLink + "about/");
            } catch (TimeoutException e){
                ErrorLogger.logError(e, Main.DEBUG);
                System.out.println("Logged out");
                fails ++;
                throw new LoggedOutException();
            }
            if (!driver.getCurrentUrl().equals(socialMediaLink + "about/")){
                System.out.println("Logged out");
                fails ++;
                throw new LoggedOutException();
            }
            Thread.sleep(2000);
            try {
                scrollToBottom();
            } catch (TimeoutException e){
                ErrorLogger.logError(e, Main.DEBUG);
            }
            String originalTabHandle = driver.getWindowHandle();

            // Step 2: Extract website URL and phone number
            try {
                WebElement websiteElement = driver.findElement(By.xpath("//dt[contains(@class, 'mb1 text-heading-medium') and text()='Website']/following-sibling::dd//a"));
                websiteUrl = websiteElement.getAttribute("href");
            } catch (Exception e) {
                googleSearch(companyName + " " + industry + " " + location);
                try {
                    WebElement websiteButton = driver.findElement(By.xpath("//a[contains(@class, 'ab_button')]/div[text()='Website']"));
                    websiteUrl = websiteButton.getAttribute("href");
                } catch (Exception ex) {
                    try {
                        WebElement websiteElement = driver.findElement(By.xpath("(//a[@jsname][@href][br][h3])[1]"));
                        websiteUrl = websiteElement.getAttribute("href");
                    } catch (Exception exx) {
                        ErrorLogger.logError(exx, Main.DEBUG);
                    }
                }
                driver.close();
                driver.switchTo().window(originalTabHandle);
            }

            try {
                WebElement phoneElement = driver.findElement(By.xpath("//dt[contains(@class, 'mb1 text-heading-medium') and text()='Phone']/following-sibling::dd//span"));
                phoneNumbers += phoneElement.getText() + "; ";
            } catch (Exception e) {
                ErrorLogger.logError(e, Main.DEBUG);
            }

            // Step 3: Extract address information
            try {
                WebElement locationsHeader = driver.findElement(By.xpath("//h3[contains(@class, 'text-heading-xlarge')]"));
                if (locationsHeader.getText().contains("(1)")) {
                    WebElement addressElement = locationsHeader.findElement(By.xpath(".//following-sibling::p"));
                    addresses += addressElement.getText() + "; ";
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
                                //Thread.sleep(500);
                                List<WebElement> addressElements = driver.findElements(By.xpath("//div[contains(@class, 'org-location-card')]//p"));
                                for (WebElement addressElement : addressElements) {
                                    addresses += addressElement.getText() + "; ";
                                }
                                WebElement closeButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@aria-label='Close the selected group of locations']")));
                                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", locationsHeader);
                                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, -150)");
                                //Thread.sleep(500);
                                closeButton.click();
                            }
                            index++;
                        }
                    }
                }
            } catch (Exception e) {
                ErrorLogger.logError(e, Main.DEBUG);
            }
            try {
                // Step 4: Extract emails and phone numbers from website
                emailsFromWebsite = "";
                phonesFromWebsite = "";
                if (!websiteUrl.isEmpty()) {
                    try {
                        driver.get(websiteUrl);
                    } catch (TimeoutException e){
                        ErrorLogger.logError(e, Main.DEBUG);
                    }
                    //Thread.sleep(3000);
                    try {
                        scrollToBottom();
                    } catch (TimeoutException e){
                        ErrorLogger.logError(e, Main.DEBUG);
                    }
                    String pageSource = driver.getPageSource();
                    emailsFromWebsite += extractEmails(pageSource);
                    phonesFromWebsite += extractVisiblePhoneNumber(pageSource);
                    List<WebElement> websiteLinks = driver.findElements(By.xpath("//a[contains(@href, websiteUrl)]"));
                    //websiteLinks = websiteLinks.stream()
                    //        .filter(distinctByKey(l -> l.getAttribute("href")))
                    //        .collect(Collectors.toList());
                    String companyWebsiteTabHandle = driver.getWindowHandle();
                    int i = 0;
                    for (WebElement link : websiteLinks) {
                        String linkUrl = link.getAttribute("href");
                        if (linkUrl == null || (i > 3 && !linkUrl.toLowerCase().contains("contact")))
                            continue;
                        if ((Util.trimToDomain(linkUrl).contains(Util.trimToDomain(websiteUrl)) ||
                                Util.trimToDomain(websiteUrl).contains(Util.trimToDomain(linkUrl)) ||
                                linkUrl.contains(websiteUrl) || linkUrl.startsWith("/")) &&
                                !linkUrl.contains("/#")) {
                            if (linkUrl.startsWith("/")) {
                                URL mergedURL = new URL(new URL(websiteUrl), linkUrl);
                                linkUrl = mergedURL.toString();
                            }
                            ((JavascriptExecutor) driver).executeScript("window.open('');");
                            Set<String> handles = driver.getWindowHandles();
                            String newTabHandle = handles.toArray()[handles.size() - 1].toString();
                            driver.switchTo().window(newTabHandle);
                            try {
                                driver.get(linkUrl);
                            } catch (TimeoutException e){
                                ErrorLogger.logError(e, Main.DEBUG);
                            }
                            //Thread.sleep(2000);
                            try {
                                scrollToBottom();
                            } catch (TimeoutException e){
                                ErrorLogger.logError(e, Main.DEBUG);
                            }
                            pageSource = driver.getPageSource();
                            emailsFromWebsite += extractEmails(pageSource);
                            phonesFromWebsite += extractVisiblePhoneNumber(pageSource);
                            driver.close();
                            driver.switchTo().window(companyWebsiteTabHandle);
                            i ++;
                        }
                        if (i > 3 && linkUrl.toLowerCase().contains("contact"))
                            break;
                    }
                }
            } catch (Exception e) {
                ErrorLogger.logError(e, Main.DEBUG);
            }
            try {
                googleSearch(companyName + " " + industry + " " + location);
                addresses += findAddressFromGoogleSearch();
                emailsFromWebsite += extractEmails(driver.getPageSource());
                phonesFromWebsite += findPhNoFromGoogleSearch();
                phonesFromWebsite += findPhNosFromGoogleSearch();
                driver.close();
                driver.switchTo().window(originalTabHandle);
                bingSearch(companyName + " " + industry + " " + location);
                emailsFromWebsite += extractEmails(driver.getPageSource());
                phonesFromWebsite += findPhNoFromBingSearch();
                driver.close();
                driver.switchTo().window(originalTabHandle);
            } catch (Exception e) {
                ErrorLogger.logError(e, Main.DEBUG);
            }

        } catch (LoggedOutException e){
            writeToFile = false;
        } catch (Exception e) {
            ErrorLogger.logError(e, Main.DEBUG);
        } finally {
            if (writeToFile) {
                phoneNumbers += phonesFromWebsite;
                emailAddresses += emailsFromWebsite;
                company.setEmailAddresses(Util.removeDuplicates(emailAddresses));
                company.setPhoneNumbers(Util.removeDuplicates(Util.removeInvalidNumbers(phoneNumbers)));
                company.setWebsites(Util.removeDuplicates(websiteUrl));
                company.setAddresses(Util.removeDuplicates(addresses));
                System.out.println(String.format("\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\""
                        , company.getName(), company.getLink(), company.getIndustry()
                        , company.getLocation(), company.getFollowers()
                        , company.getPhoneNumbers(), company.getEmailAddresses()
                        , company.getWebsites(), company.getAddresses()));
                synchronized (lock) {
                    logCompany(company);
                }
            }
        }
    }

    private <T> java.util.function.Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private void googleSearch(String query) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("window.open('');");
        Set<String> handles = driver.getWindowHandles();
        String newTabHandle = handles.toArray()[handles.size() - 1].toString();
        driver.switchTo().window(newTabHandle);
        driver.get("https://www.google.com");
        WebElement searchBox = driver.findElement(By.name("q"));
        searchBox.sendKeys(query);
        searchBox.sendKeys(Keys.RETURN);
        //Thread.sleep(3000);
    }

    private void bingSearch(String query) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("window.open('');");
        Set<String> handles = driver.getWindowHandles();
        String newTabHandle = handles.toArray(new String[0])[handles.size() - 1];
        driver.switchTo().window(newTabHandle);
        driver.get("https://www.bing.com");

        // Wait for the search box to be present and visible
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("q")));

        // Ensure the search box is clickable and interactable
        wait.until(ExpectedConditions.elementToBeClickable(searchBox));

        // Use Actions to click on the search box before sending keys
        Actions actions = new Actions(driver);
        actions.moveToElement(searchBox).click().perform();

        // Input the query into the search box
        searchBox.sendKeys(query);
        searchBox.sendKeys(Keys.RETURN);

        // Optionally wait for the results to load
        Thread.sleep(3000);
    }

    private String findAddressFromGoogleSearch() {
        try {
            // Locate the span element containing an a element with the text content "Address"
            WebElement spanWithAddressLink = driver.findElement(By.xpath("//span[a[text()='Address']]"));

            // Locate the next span element under this span element
            WebElement addressSpan = spanWithAddressLink.findElement(By.xpath("./following-sibling::span"));

            // Extract and return the text content of the found span element
            return addressSpan.getText() + "; ";
        } catch (Exception e) {
            ErrorLogger.logError(e, Main.DEBUG);
            return "";
        }
    }

    private String findPhNoFromBingSearch() {
        try {
            WebElement spanPhoneNumber = driver.findElement(By.xpath("//*[contains(@aria-label, \"Phone\")]"));
            return spanPhoneNumber.getText() + "; ";
        } catch (Exception e) {
            ErrorLogger.logError(e, Main.DEBUG);
            return "";
        }
    }

    private String findPhNoFromGoogleSearch() {
        try {
            WebElement spanPhoneNumber = driver.findElement(By.xpath("//*[contains(@aria-label, \"phone\")]"));
            return spanPhoneNumber.getText() + "; ";
        } catch (Exception e) {
            ErrorLogger.logError(e, Main.DEBUG);
            return "";
        }
    }

    private String findPhNosFromGoogleSearch() {
        try {
            String result = "";
            List<WebElement> elements = driver.findElements(By.xpath("//*[contains(@aria-level, 3) and contains(@role, heading)]"));
            for (WebElement e : elements){
                WebElement divElement = e.findElement(By.xpath("following-sibling::div"));
                result += divElement.getText();
            }
            return result;
        } catch (Exception e) {
            ErrorLogger.logError(e, Main.DEBUG);
            return "";
        }
    }

    private void scrollToBottom() throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
        //Thread.sleep(2000); // wait for 2 seconds to ensure all content is loaded
    }


    private void logCompany(Company company) {
        boolean fileExists = new File(Main.OUTPUT_PATH).exists();
        try (CSVWriter writer = new CSVWriter(new FileWriter(Main.OUTPUT_PATH, true))) {
            // Write header if file does not exist
            if (!fileExists) {
                writer.writeNext(Main.HEADERS);
            }
            // Write company details
            String[] details = {
                    company.getName(),
                    company.getLink(),
                    company.getIndustry(),
                    company.getLocation(),
                    String.valueOf(company.getFollowers()),
                    company.getPhoneNumbers(),
                    company.getEmailAddresses(),
                    company.getWebsites(),
                    company.getAddresses()
            };
            writer.writeNext(details);
        } catch (IOException e) {
            ErrorLogger.logError(e, Main.DEBUG);
        }
    }
    private String extractVisiblePhoneNumber(String s) {
        // Define the regex pattern for phone numbers
        String regex = "\\b(\\+61|0)?[\\s-]?(\\d{1,4})[\\s-]?(\\d{2,4})[\\s-]?(\\d{2,4})[\\s-]?(\\d{0,4})\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(s);
        StringBuilder phones = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            if (Util.isValidLength(match) && !Util.isDate(match) && isElementVisible(match))
                phones.append(match).append("; ");
        }
        return phones.toString();
    }

    private String extractEmails(String text) {
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(?!jpg|jpeg|png|gif|pdf|doc|docx|xls|xlsx|zip|rar|mp3|mp4|avi|mkv)[A-Za-z]{2,}\\b");
        Matcher matcher = emailPattern.matcher(text);
        StringBuilder emails = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            if (!match.contains("@sentry-next.wixpress.com") && !match.contains("@sentry.io") && !match.contains("@sentry.wixpress.com"))
                emails.append(match).append("; ");
        }
        return emails.toString();
    }

    private boolean isElementVisible(String s){
        // Find all elements that contain the given string
        List<WebElement> elements = driver.findElements(By.xpath("//*[contains(text(), '" + s + "')]"));

        // Check if any of the elements are visible
        for (WebElement element : elements) {
            if (element.isDisplayed()) {
                return true;
            }
        }

        elements = driver.findElements(By.xpath("//*[text()[contains(.,'" + s + "')]]"));

        // Check if any of the elements are visible
        for (WebElement element : elements) {
            if (element.isDisplayed()) {
                return true;
            }
        }

        return false;
    }

}
