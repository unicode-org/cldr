package org.unicode.cldr.web;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.CldrUtility;

/** Google Sheets client for the Error Subtype */
public class ErrorSubtypeClient {

    static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(ErrorSubtypeClient.class.getSimpleName());

    /** range for the all-subtypes list. This will be written to */
    static final String ALL_SUBTYPES_RANGE = "AllSubtypes!A1:A";

    /** range for the subtype map. This will be read. Don't count the header. */
    static final String SUBTYPE_MAP_RANGE = "SubtypeToURLMap!A2:B";

    /** default name of credentials (service key) file */
    private static final String SUBTYPE_CREDENTIALS_JSON = "subtype-credentials.json";

    /** scopes needed */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    /** property indicating the full credential path */
    private static final String CLDR_SUBTYPE_CREDENTIAL_PATH = "CLDR_SUBTYPE_CREDENTIAL_PATH";

    /**
     * property indicating the Google Sheet ID of the spreadsheet. Spreadsheet must be shared with
     * the service credential for editing.
     */
    private static final String CLDR_SUBTYPE_SHEET = "CLDR_SUBTYPE_SHEET";

    /** read credentials from the file */
    private static Credential getCredentials() throws IOException {
        CLDRConfig config = CLDRConfig.getInstance();
        final String credentialsPath =
                config.getProperty(CLDR_SUBTYPE_CREDENTIAL_PATH, getDefaultCredentialPath(config));
        if (!new File(credentialsPath).canRead()) {
            logger.severe("Could not read credentials file " + credentialsPath);
            throw new IOException("Could not read credentials file " + credentialsPath);
        }
        InputStream in = new FileInputStream(credentialsPath);
        Credential credential = GoogleCredential.fromStream(in).createScoped(SCOPES);
        credential.refreshToken();
        return credential;
    }

    /** default path for credentials */
    private static String getDefaultCredentialPath(CLDRConfig config) {
        return new File(config.getProperty(CldrUtility.HOME_KEY), SUBTYPE_CREDENTIALS_JSON)
                .getAbsolutePath();
    }

    /**
     * You can run this main to exercise the client. You will need to pass it the path to your
     * cldr.home which is the directory containing cldr.properties
     */
    public static void main(String... args) throws IOException, GeneralSecurityException {
        logger.setLevel(Level.ALL);
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: " + ErrorSubtypeClient.class.getSimpleName() + " (CLDR_HOME)");
        }
        final String cldrHome = args[0];
        logger.info("Using cldrHome=" + cldrHome);
        System.setProperty(CLDRConfigImpl.class.getName() + ".cldrHome", cldrHome);
        CLDRConfigImpl.setCldrHome(cldrHome);

        // OK now call the client
        List<List<Object>> values = updateAndReadSubtypeMap();

        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            for (List<Object> row : values) {
                System.out.println(row.toString());
            }
        }
    }

    /** main internal API, updates the old values and reads the new ones */
    static List<List<Object>> updateAndReadSubtypeMap()
            throws GeneralSecurityException, IOException {
        final String spreadsheetId = getSpreadsheetId();
        logger.info("Attempting to access spreadsheet " + spreadsheetId);
        final JsonFactory gson = GsonFactory.getDefaultInstance();
        final NetHttpTransport http = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service =
                new Sheets.Builder(http, gson, getCredentials())
                        .setApplicationName("CLDR SurveyTool")
                        .build();

        // first update
        updateAllSubtypesSheet(spreadsheetId, service);

        // now read
        List<List<Object>> values = readSubtypeSheet(spreadsheetId, service);
        return values;
    }

    /** read values out of the subtype map */
    private static List<List<Object>> readSubtypeSheet(final String spreadsheetId, Sheets service)
            throws IOException {
        logger.info(
                "Reading subtype map from spreadsheet "
                        + spreadsheetId
                        + " : "
                        + SUBTYPE_MAP_RANGE);
        ValueRange response =
                service.spreadsheets().values().get(spreadsheetId, SUBTYPE_MAP_RANGE).execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            logger.warning("Read empty subtype map");
            return null;
        }
        logger.info("Read subtype with " + values.size() + " items");
        return values;
    }

    /** update the sheet at ALL_SUBTYPES_RANGE with the list of all subtypes */
    private static void updateAllSubtypesSheet(final String spreadsheetId, Sheets service)
            throws IOException {
        // update the full set of items
        logger.info("Updating all subtypes list " + ALL_SUBTYPES_RANGE);
        final List<List<Object>> allSubtypes = new ArrayList<List<Object>>();
        allSubtypes.add(Arrays.asList("subtype")); // heading
        for (final CheckCLDR.CheckStatus.Subtype s : CheckCLDR.CheckStatus.Subtype.values()) {
            allSubtypes.add(Arrays.asList(s.name()));
        }
        ValueRange allSets = new ValueRange().setValues(allSubtypes);
        UpdateValuesResponse uResponse =
                service.spreadsheets()
                        .values()
                        .update(spreadsheetId, ALL_SUBTYPES_RANGE, allSets)
                        .setValueInputOption("RAW")
                        .execute();
        // print out the existing values
        System.out.println("Updated: " + uResponse.toPrettyString());
    }

    /** compute the default ID of the spreadsheet */
    private static String getSpreadsheetId() {
        return CLDRConfig.getInstance()
                .getProperty(CLDR_SUBTYPE_SHEET, "1n7H_yt2Sxea1_AAp6Ggi5zo1HxtC5-xwieBlyM2kAcs");
    }
}
