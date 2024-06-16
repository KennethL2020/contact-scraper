package org.omnisearch.cs;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ErrorLogger {

    private static final String ERROR_LOG_FILE = "error_log.csv";
    private static final String[] ERROR_HEADERS = { "Timestamp", "Error Message", "Stack Trace" };

    public static void logError(Exception exception, boolean debug) {
        boolean fileExists = new java.io.File(ERROR_LOG_FILE).exists();
        if (debug){
            exception.printStackTrace();
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter(ERROR_LOG_FILE, true))) {
            // Write header if file does not exist
            if (!fileExists) {
                writer.writeNext(ERROR_HEADERS);
            }

            // Get current timestamp
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // Get error message
            String errorMessage = exception.getMessage();

            // Get stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            String stackTrace = sw.toString();

            // Write error details
            String[] errorDetails = { timestamp, errorMessage, stackTrace };
            writer.writeNext(errorDetails);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
