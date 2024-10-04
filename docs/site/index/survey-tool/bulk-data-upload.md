---
title: Bulk Data Upload
---

# Bulk Data Upload

Here are the instructions for a bulk upload (of an XML file in LDML format) to the Survey Tool. You must be reasonably conversant with XML and the [LDML format](http://unicode.org/reports/tr35/) to use this method.

1. Prepare your xml files \- one per locale. Each file must be valid XML and LDML for a single file. The file doesn't have to be "complete": it might contain only translations of territories, for example.
	- *(Note: There is a new option that allows you to download from the Dashboard as a spreadsheet, modify the spreadsheet, and then convert that spreadsheet into an XML.  To access this option:<br> In the menu, choose **Upload(Bulk Import)** and then **Convert XLSX to XML**. Follow the steps from there.)*
2. The locale must exist. 
	- If not, see [Adding New Locales](/index/bug-reports#New_Locales).
3. You must be logged in, and under an account with permission to write to it.
	- If you don't have an account, see [Survey Tool Accounts](/index/survey-tool/accounts).
	- Managers and TC Members can submit on behalf of another user.
4. Go to the menu, under the **Upload ( Bulk Import)**  section. Choose **Upload XML as your Vote**.
5. Put in your email address to submit as yourself. If you manage other users, you may put the email address associated with their survey tool account in to submit a vote as that user.
6. Click **Choose File** to pick the XML file for that locale on your locale disk
7. Click **Upload as my Submission/Vetting choices**
8. You will see a raw listing of lines in XML, and an error line if the file doesn't validate.
	1. If the file does not validate, fix the file, hit the back button, and go to Step 4.
	2. If the file does validate, you'll see a list of XML paths and values.
9. Click **Submit \<locale\>**.
10. You will see a detailed list of the test results for the items you're submitting.
	- You can click on an item's path link (left hand side) to view that item in the surveytool
	- Any items with an error icon  will not be submitted.
	- If the message is "Item is not writable in the Survey Tool. Please file a ticket." then you will need to [file a ticket](/index/bug-reports#TOC-Filing-a-Ticket) instead. These can be filed in a single ticket. Include all the paths and the respective values.
	1. Press "Really Submit As My Vote" to submit all passing items as your vote, or revise the file and start back at Step 4.

### Example XML:

```xml
<?xml version="1.0" encoding="UTF-8" ?>

<!DOCTYPE ldml SYSTEM "../../common/dtd/ldml.dtd">  <!-- Not important. Latest DTD will be used. -->

<ldml>

  <identity>

    <version number="$Revision: 6546 $"/> <!-- ignored -->

    <generation date="$Date: 2012-02-07 10:32:35 -0800 (Tue, 07 Feb 2012) $"/> <!-- ignored -->

    <!-- CRITICAL: you must supply a valid identity block specifying language,

          and if part of the identity, the script, region, variant, etc. -->

    <language type="aa"/>  <!-- Required -->

  </identity>

  <localeDisplayNames>

    <scripts>

      <!-- The draft attribute and alt=proposed value are ignored. Comments are ignored.  -->

      <script alt="proposed-ABCDEF" type="Latn" draft="unconfirmed">Latin</script>  <!-- OK. -->

      <!-- The rest indicate errors people might have. -->

      <script alt="proposed-ABCDEF" type="Mong" draft="unconfirmed">Latin</script> <!-- ERR: duplicate -->

      <script alt="proposed-ABCDEF" type="Brai" draft="unconfirmed">Latin</script> <!-- ERR: duplicate -->

      <script alt="proposed-ABCDEF" type="Hant" draft="unconfirmed">Latin</script> <!-- ERR: duplicate -->

      <script alt="proposed-ABCDEF" type="Deva" draft="unconfirmed">Latin</script> <!-- ERR: duplicate -->

      <script alt="proposed-ABCDEF" type="0" draft="unconfirmed">Latin</script>    <!-- ERR: bad 'type' -->

    </scripts>

  </localeDisplayNames>

  <numbers>

    <defaultNumberingSystem>brai</defaultNumberingSystem>  <!-- ERR: can't change via survey tool -->

  </numbers>

</ldml>
```

Note: the filename of the XML file doesn't matter

### Example Submission View:

**Note to Organization Managers**: if you are submitting on behalf of another user, clicking these links will switch your user to that user.

![image](../../images/index/bulkDataUpload0.png)

