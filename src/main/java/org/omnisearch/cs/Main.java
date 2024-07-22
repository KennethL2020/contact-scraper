package org.omnisearch.cs;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.omnisearch.cs.dto.Company;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final boolean DEBUG = true;
    public static final boolean KEEP_TIME = true;
    public static final String[] HEADERS = { "Name", "Link", "Industry", "Location", "Followers", "Phone", "Email", "Website", "Address" };
    public static final String CHROME_PATH = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent\\Iteration 2\\potential partners\\contact-scraper\\chrome-win64\\chrome-win64\\chrome.exe";
    public static final String OUTPUT_PATH = "company_log.csv";
    public static final String RECON_PATH = "recon.csv";
    public static final String INPUT_PATH = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent\\Iteration 2\\potential partners\\priority\\electronics_manufacturing\\file0.csv";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0";
    public static final String USER_DATA = "\\Chrome for Testing\\User Data\\";
    public static final String USER_DATA_PREFIX = "C:\\Users\\kenne\\AppData\\Local\\Google\\Threads\\";
    public static final String PROFILE = "Default";
    public static final String WEBDRIVER_PATH = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent" +
            "\\Iteration 2\\potential partners\\contact-scraper\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe";

    public static void main(String[] args){
        List<Company> companies = readCompaniesFromCsv(INPUT_PATH);
        int numberOfThreads = 3; // Number of threads you want to use
        int companiesPerThread = (int) Math.ceil((double) companies.size() / numberOfThreads);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * companiesPerThread;
            int end = Math.min(start + companiesPerThread, companies.size());

            List<Company> sublist = companies.subList(start, end);
            DataCrawler crawler = new DataCrawler(sublist, i+1);

            executor.submit(crawler);
        }

        // Shut down the executor and wait for all tasks to finish
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<Company> readCompaniesFromCsv(String filePath) {
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
            ErrorLogger.logError(e, Main.DEBUG);
        }
        return companies;
    }
}
