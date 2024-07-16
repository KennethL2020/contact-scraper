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
    public static final String OUTPUT_PATH = "company_log.csv";
    public static final String RECON_PATH = "recon.csv";
    public static final String INPUT_PATH = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent\\Iteration 2\\potential partners\\priority\\electronics_manufacturing\\file2.csv";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0";
    public static final String LINKEDIN_COOKIES = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent\\Iteration 2\\potential partners\\contact-scraper\\src\\main\\resources\\exported-cookies.json";
    public static final String GOOGLE_COOKIES = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent\\Iteration 2\\potential partners\\contact-scraper\\src\\main\\resources\\exported-cookies-google.json";
    public static final String USER_DATA = "C:\\Users\\kenne\\AppData\\Local\\Google\\Chrome\\User Data";
    public static final String ADDITIONAL_COOKIES = "bcookie=\"v=2&bd4f63c8-82af-4f6d-8847-154aa739b1a5\"; li_sugr=8c9ee0d1-69df-4a43-8bf3-52e2524bfd3f; bscookie=\"v=1&2023062901405403523bb9-0898-4d6d-8339-9b4657a2ef7eAQGFvgHxb9ph_zRaWAkNnkgym3FufAof\"; g_state={\"i_l\":0}; li_theme=light; li_theme_set=app; dfpfpt=78e020afcfb943bda017f8b2b9738568; li_rm=AQEwG6dMd4QguQAAAY3v_Mgx5flErpamsxD5nmN2BO6L7m8_f1Zd3s4-DfUFDfWE49MwRrY00BhoJTFTnVTh13TMGjNC_pss6PB89JN3CkhSYxZteO2cpWqF; __ssid=c0e7e9bf-7c96-49e8-96ef-d5fdae47113a; s_ips=742; gpv_pn=www.linkedin.com%2Fpremium%2Fwelcome-flow%2F; s_tp=755; s_tslv=1709515198916; l_page=https://www.linkedin.com/in/jimmyzamani/?fbclid=IwAR0qu_kHEPbc_um3iuhSh5a_GKGBxHYfG5GNLwJERDpVTWmgJqgTsontwpM; timezone=Australia/Sydney; aam_uuid=30658373170471406562014633407533178313; _guid=4a065b08-f66d-416a-828f-2fea25543914; visit=v=1&M; AnalyticsSyncHistory=AQL5HFx7tGNwRQAAAZCx8mN9xZUUBrR0-YNlh6FAIy3PzaimhMN-0dpf-ofJfowk5bEQBJRtga-l7DMqhGb_Ow; lms_ads=AQG8xRfKqSv6OgAAAZCx8mTc51loChtQZIQRmjeXgnZ4jd0H7GGj2D6kYojEXX-d0KdyXlu-XaUNZkqDeV2W1p5ikIWvZSk9; lms_analytics=AQG8xRfKqSv6OgAAAZCx8mTc51loChtQZIQRmjeXgnZ4jd0H7GGj2D6kYojEXX-d0KdyXlu-XaUNZkqDeV2W1p5ikIWvZSk9; AMCVS_14215E3D5995C57C0A495C55%40AdobeOrg=1; fptctx2=taBcrIH61PuCVH7eNCyH0J9Fjk1kZEyRnBbpUW3FKs%252fKlcrBAPPEU63nC6DlVaUuz192WD75mqF6VNRFsNic%252fmsRZKQ1smcB7q1hLHm3kO8r8XCppNk8ceX8EOQoZDWlekl7Y1f%252f7emrNj47WNKmstFeyOa6nno9QQBR2B3zfTNbTm%252fwVkWiIQq5FNpQIN%252fAew%252byopCDdd7Bm4%252f1%252fjwHrAat4bmD5CEl8tKtiqkrxVWvnVrjai2pVSj9AkExkfBqC5PXHm%252f69WvS877G4HDiXCC4QHkP7ICZ1XEjV8iQIxmyGzou4xMo2IteFUVnvX%252fMt8ySoEYNttTW5C2LAPpLrDKwQKJ%252bl8rvY%252bTI08vS1pQ%253d; AMCV_14215E3D5995C57C0A495C55%40AdobeOrg=-637568504%7CMCIDTS%7C19921%7CMCMID%7C30453195174519569871961956822207475202%7CMCAAMLH-1721715262%7C8%7CMCAAMB-1721715262%7C6G1ynYcLPuiQxYZrsz_pkqfLG9yMXBpb2zX5dvJdYQJzPXImdj0y%7CMCOPTOUT-1721117662s%7CNONE%7CvVersion%7C5.1.1%7CMCCIDH%7C924991526; li_g_recent_logout=v=1&true; UserMatchHistory=AQKO8-zTfl7-gAAAAZC6PccyQ3AxbXWWUGU1Dy3FJZUvgWx7H-JEQS22iH-b8GLPWSp1GBsW_9F_klCBzaQFTP7OfxuH1K48qPW_SKJQJMQSBMUC9Dseput2j8HktuXBS3tcDZBbhDhX44BC-vOPaY7YCYuguEQYunDprcue4G7Z6ScCnNxrGwj29eksF0-avo5eLUpPbMtOsxc2ttt3GVT9-63Q35zmQ-q6hFTa0pxLkqsiHta2wtUdjNBxbdV0-bQ9tFPUMLsqksW_t7RT7ps141bao688FgZOup5_3jag9nkYqWOcVgxX-uuBkecnaxu0PdAmkae1k_3AHk9HO5eO54qFhKIWbzKNWNkGdponLNb4Mg; JSESSIONID=ajax:1235531241225825825; lang=v=2&lang=en-us; lidc=\"b=OGST04:s=O:r=O:a=O:p=O:g=3246:u=1:x=1:i=1721112506:t=1721198906:v=2:sig=AQEicSn0XNRDekUEtnofwEMc7zmaq2kS\"";
    public static final String PROFILE = "Profile 4";
    public static final String WEBDRIVER_PATH = "C:\\Users\\kenne\\OneDrive\\Desktop\\shared\\Projects\\Job Agent" +
            "\\Iteration 2\\potential partners\\contact-scraper\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe";

    public static void main(String[] args){
        List<Company> companies = readCompaniesFromCsv(INPUT_PATH);
        int numberOfThreads = 1; // Number of threads you want to use
        int companiesPerThread = (int) Math.ceil((double) companies.size() / numberOfThreads);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * companiesPerThread;
            int end = Math.min(start + companiesPerThread, companies.size());

            List<Company> sublist = companies.subList(start, end);
            DataCrawler crawler = new DataCrawler(sublist);

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
