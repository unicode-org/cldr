# Test Data for DateTime Formatting

This directory contains test data for DateTime formatting.
This data is the result of the test data generator `GenerateDateTimeTestData.java`.

The design of the test generator is to provide the "expected value" of datetime formatting over several dimensions,
such as locale,
formatting options (date length, calendar, etc.), 
and various input datetime values.

The test generator constructs the expected value using the various pieces
(date format pattern, time format pattern, datetime "glue" pattern)
and `SimpleDateFormat`s to combine them together.
Each test case reports the inputs for the test case and the expected value.