package org.omnisearch.cs;

import java.util.regex.Pattern;

public class AustralianPhoneNumberValidator {

    // Define regex patterns for different Australian phone number formats without spaces
    private static final String LANDLINE_WITHOUT_AREA_CODE_REGEX = "\\d{8}"; // Landline without area code
    private static final String LANDLINE_REGEX = "02\\d{8}";
    private static final String MOBILE_REGEX = "04\\d{8}";
    private static final String LOCAL_RATE_REGEX = "1300\\d{6}";
    private static final String ALT_LOCAL_RATE_REGEX = "13\\d{4}";
    private static final String FREEPHONE_REGEX = "1800\\d{6}";
    private static final String INT_LANDLINE_REGEX = "612\\d{8}";
    private static final String INT_MOBILE_REGEX = "614\\d{8}";

    public static boolean isValidAustralianPhoneNumber(String phoneNumber) {
        // Combine all regex patterns into one pattern
        String combinedRegex = String.format(
                "(%s)|(%s)|(%s)|(%s)|(%s)|(%s)|(%s)|(%s)",
                LANDLINE_REGEX,
                MOBILE_REGEX,
                LOCAL_RATE_REGEX,
                ALT_LOCAL_RATE_REGEX,
                FREEPHONE_REGEX,
                INT_LANDLINE_REGEX,
                INT_MOBILE_REGEX,
                LANDLINE_WITHOUT_AREA_CODE_REGEX
        );

        // Compile the combined regex pattern
        Pattern pattern = Pattern.compile(combinedRegex);

        // Check if the phone number matches any of the patterns
        return pattern.matcher(phoneNumber).matches();
    }
}
