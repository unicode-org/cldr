package org.unicode.cldr.unittest.web;

import java.sql.SQLException;

import org.unicode.cldr.web.UserSettings;
import org.unicode.cldr.web.UserSettingsData;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.ElapsedTimer;

public class TestUserSettingsData extends TestFmwk {

    private void setupDB() {
        long start = System.currentTimeMillis();
        TestAll.setupTestDb();
        logln("Set up test DB: " + ElapsedTimer.elapsedTime(start));
    }

    public static void main(String[] args) {
        args = TestAll.doResetDb(args);
        new TestUserSettingsData().run(args);
    }

    UserSettingsData getData() throws SQLException {
        return UserSettingsData.getInstance(TestAll.getProgressIndicator(this));
    }

    private String expect(String expectString, String key, String defaultValue, UserSettings data) {
        String currentWinner = data.get(key, defaultValue);

        if (!expectString.equals(currentWinner)) {
            errln("ERR:" + key + ": Expected '" + expectString + "':  got  '" + currentWinner + "'");
        } else {
            logln("ok :" + key + ":  got   expected '" + currentWinner + "'");
        }
        return currentWinner;
    }

    public void TestSeparate() throws SQLException {
        {
            setupDB();
            UserSettingsData d = getData();

            UserSettings a = d.getSettings(0);

            expect("(default)", "aKey", "(default)", a);
            a.set("aKey", "z");
            expect("z", "aKey", "(default)", a);

            UserSettings b = d.getSettings(0);
            expect("z", "aKey", "(default)", b);
            UserSettings c = d.getSettings(1);
            expect("(default)", "aKey", "(default)", c);
        }

        {
            UserSettingsData d = getData();

            UserSettings a = d.getSettings(0);

            expect("z", "aKey", "(default)", a);
        }
    }

}
