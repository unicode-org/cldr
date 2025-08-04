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

The UTS 35 LDML spec for datetime formatting will be updated in CLDR v48 to specify
that the default value for dateTimeFormatType will be "atTime".
dateTimeFormatType represents the value that indicates the type of
datetime "glue" pattern, ex: indicating "atTime" or "standard" pattern.
By datetime "glue" pattern, we mean the pattern that is used to combine the result
of date-only formatting and time-only formatting to arrive at the overall combined
formatting for the datetime object containing both a date and a time.
Therefore, for test cases in the dataset in which a date and a time are both present
in the datetime object, if a dateTimeFormatType is not specified explicitly, the
value should be assumed to be "atTime".