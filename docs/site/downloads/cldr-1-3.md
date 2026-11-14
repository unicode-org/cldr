---
title: CLDR 1.3 Release Note
---

# CLDR 1.3 Release Note

The following are the files for this release. For a description of their purpose and format, see [CLDR Releases (Downloads)].

| No. | Date | Rel. Note | Data | Spec | Delta Tickets | GitHub Tag |
|:---:|:----------:|:---------:|:--------:|:------------:|:---:|:----------:|
| 1.3 | 2005-06-02 | [Version1.3] | [CLDR1.3] | [LDML1.3] | [~~Δ1.3~~] | [release-1-3] |

CLDR Version 1.3 contains contains data for 296 locales: 96 languages and 130 territories. In this release, the major additions to CLDR are:

- A complete set of POSIX-format data generated from the XML format, plus a tool to generate versions for different platforms.
- The addition of new data to support localization of timezones
- The addition of data for UN M.49 regions, including continents and region.
- The canonicalization of the data files, including the consolidation of inherited data
- The restriction of currency codes to ISO 4217 codes (past and present)
- The addition of number and data tests, for verifying that implementations correctly implement the LDML specification using CLDR data.
- The addition of metadata for LDML
- The addition of mappings from language to script and territory
- Various other fixes and additions of data, and extensions to the specification.

## Corrigendum 1: Time zone and Date Format Pattern Correction

In CLDR version 1.3, the policy was that for a given (resolved) locale, uniqueness is required for time zone display names.
That is, two different time zone IDs could not have the same display name. This policy turns out to be overly strict,
and did not allow for customary names in cases where it does not cause a problem.
The committee has relaxed this policy so that where the parsing results would give the same GMT offset,
the standard and daylight display names can be the same across different time zone IDs.

The short and long time zone names for Europe/London and Europe/Dublin in the `en.xml` locale data file had been changed because of the old policy.
In accordance with this new policy, they are corrected by this corrigendum as follows:

<table border="1" bordercolor="#111111" cellpadding="0" cellspacing="1" style="border-collapse:collapse">
  <tbody>
    <tr>
      <th width="50%">CLDR 1.3</th>
      <th width="50%">Corrected</th>
    </tr>
    <tr>
      <td width="50%">
        <code>
          <font size="2">&lt;zone type="Europe/London"&gt;
            <br />  &lt;long&gt;
            <br />    &lt;generic&gt;British Time&lt;/generic&gt; 
            <br />    &lt;standard&gt;British Standard Time&lt;/standard&gt; 
            <br />    &lt;daylight&gt;British Daylight Time&lt;/daylight&gt; 
            <br />  &lt;/long&gt;<br />  &lt;short&gt;
            <br />    &lt;generic&gt;BT&lt;/generic&gt; 
            <br />    &lt;standard&gt;BST&lt;/standard&gt; 
            <br />    &lt;daylight&gt;BDT&lt;/daylight&gt; 
            <br />  &lt;/short&gt;
            <br />&lt;/zone&gt;
          </font>
        </code>
      </td>
      <td width="50%">
        <code>&lt;zone type="Europe/London"&gt;
          <br />  &lt;long&gt;
          <br />    &lt;standard&gt;<span style="background-color:rgb(255,255,0)">Greenwich Mean</span> Time&lt;/standard&gt; 
          <br />    &lt;daylight&gt;British <span style="background-color:rgb(255,255,0)">Summer</span> Time&lt;/daylight&gt; 
          <br />  &lt;/long&gt;
          <br />  &lt;short&gt;
          <br />    &lt;standard&gt;<span style="background-color:rgb(255,255,0)">GM</span>T&lt;/standard&gt; 
          <br />    &lt;daylight&gt;B<span style="background-color:rgb(255,255,0)">S</span>T&lt;/daylight&gt; 
          <br />  &lt;/short&gt;<br />&lt;/zone&gt;
        </code>
      </td>
    </tr>
    <tr>
      <td width="50%">
        <p style="text-align:center">
          <i>
            <font size="2">{omitted}</font>
          </i>
        </p>
      </td>
      <td width="50%">
        <code>
          <span style="background-color:rgb(255,255,0)">&lt;zone type="Europe/Dublin"&gt;</span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">  &lt;long&gt;</span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">    &lt;standard&gt;Greenwich Mean Time&lt;/standard&gt; </span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">    &lt;daylight&gt;Irish Summer Time&lt;/daylight&gt; </span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">  &lt;/long&gt;</span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">  &lt;short&gt;</span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">    &lt;standard&gt;GMT&lt;/standard&gt; </span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">    &lt;daylight&gt;IST&lt;/daylight&gt; </span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">  &lt;/short&gt;</span>
          <br style="background-color:rgb(255,255,0)" />
          <span style="background-color:rgb(255,255,0)">&lt;/zone&gt; </span>
          <br />
        </code>
      </td>
    </tr>
  </tbody>
</table>

The data reflecting the above correction is checked into CVS under the tag release-1-3-C1. For information on the usage of this, see [CLDR Releases (Downloads)].

The following are corrections to the date format pattern in [UTS #35: Locale Data Markup Language (LDML)]. The Stand-Alone months and days, and the long era names, although approved by the technical committee, had been omitted from the specification. The use of specific sequences of 'z', 'Z', and 'E' is changed to preserve backwards compatibility with Java.

<table border="1" bordercolor="#111111" cellpadding="0" cellspacing="1" style="border-collapse:collapse">
  <tbody>
    <tr>
      <th style="vertical-align:top" width="50%">CLDR 1.3</th>
      <th style="vertical-align:top" width="50%">Corrected</th>
    </tr>
    <tr>
      <th width="50%">
        <table border="1" cellpadding="2" cellspacing="0">
          <tbody>
            <tr>
              <th>
                <font size="2">era</font></th>
              <td style="text-align:center">
                <font size="2">G</font>
              </td>
              <td style="text-align:center">
                <font size="2">1</font>
                <font size="2">..3</font>
              </td>
              <td>
                <font size="2">AD</font>
              </td>
              <td>
                <font size="2">Era - Replaced with the Era string for the current date.</font>
              </td>
            </tr>
          </tbody>
        </table>
      </th>
      <th width="50%">
        <table border="1" cellpadding="2" cellspacing="0">
          <tbody>
            <tr>
              <th rowspan="2">
                <font size="2">era</font>
              </th>
              <td rowspan="2" style="text-align:center">
                <font size="2">G</font>
              </td>
              <td style="text-align:center">
                <font size="2">1</font>
                <font size="2">..3</font>
              </td>
              <td>
                <font size="2">AD</font>
              </td>
              <td rowspan="2" style="background-color:rgb(255,255,0)">
                <font size="2">Era - Replaced with the Era string for the current date. </font>
                <font size="2">One to three letters for the abbreviated form, four letters for the long form.</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">4</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">Anno Domini</font>
              </td>
            </tr>
          </tbody>
        </table>
      </th>
    </tr>
    <tr>
      <th width="50%">
        <table border="1" cellpadding="2" cellspacing="0">
          <tbody>
            <tr>
              <th rowspan="4">
                <font size="2">month</font>
              </th>
              <td rowspan="4" style="text-align:center">
                <font size="2">M</font>
              </td>
              <td style="text-align:center">
                <font size="2">1..2</font>
              </td>
              <td>
                <font size="2">09</font>
              </td>
              <td rowspan="4">
                <font size="2">Month - Use one or two for the numerical month, three for the abbreviation, or four for the full name, or 5 for the narrow name.</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">3</font>
              </td>
              <td>
                <font size="2">Sept</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">4</font>
              </td>
              <td>
                <font size="2">September</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">5</font>
              </td>
              <td>
                <font size="2">S</font>
              </td>
            </tr>
          </tbody>
        </table>
      </th>
      <th width="50%">
        <table border="1" cellpadding="2" cellspacing="0">
          <tbody>
            <tr>
              <th rowspan="8">
                <font size="2">month</font>
              </th>
              <td rowspan="4" style="text-align:center">
                <font size="2">M</font>
              </td>
              <td style="text-align:center">
                <font size="2">1..2</font>
              </td>
              <td>
                <font size="2">09</font>
              </td>
              <td rowspan="4">
                <font size="2">Month - Use one or two for the numerical month, three for the abbreviation, or four for the full name, or five for the narrow name.</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">3</font>
              </td>
              <td>
                <font size="2">Sept</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">4</font>
              </td>
              <td>
                <font size="2">September</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">5</font>
              </td>
              <td>
                <font size="2">S</font>
              </td>
            </tr>
            <tr>
              <td rowspan="4" style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">L</font>
              </td>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">1..2</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">09</font>
              </td>
              <td rowspan="4" style="background-color:rgb(255,255,0)">
                <b>
                  <font size="2">Stand-Alone</font>
                </b>
                <font size="2"> Month - Use one or two for the numerical month, three for the abbreviation, or four for the full name, or 5 for the narrow name.</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">3</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                  <font size="2">Sept</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">4</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">September</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">5</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">S</font>
              </td>
            </tr>
          </tbody>
        </table>
      </th>
    </tr>
    <tr>
      <th width="50%"><table border="1" cellpadding="2" cellspacing="0">
        <tbody>
          <tr>
            <th rowspan="8">
              <font size="2">week<br />day</font>
            </th>
            <td rowspan="4" style="text-align:center">
              <font size="2">E</font>
            </td>
            <td style="text-align:center">
              <font size="2">1..2</font>
            </td>
            <td>
              <font size="2">3</font>
            </td>
            <td rowspan="4">
              <font size="2">Day of week - Use three for the short day, or four for the full name, or 5 for the narrow name. Sunday is always day 1</font>
            </td>
          </tr>
          <tr>
            <td style="text-align:center">
              <font size="2">3</font>
            </td>
            <td>
              <font size="2">Tues</font>
            </td>
          </tr>
          <tr>
            <td style="text-align:center">
              <font size="2">4</font>
            </td>
            <td>
              <font size="2">Tuesday</font>
            </td>
          </tr>
          <tr>
            <td style="text-align:center">
              <font size="2">5</font>
            </td>
            <td>
              <font size="2">T</font>
            </td>
          </tr>
          <tr>
            <td rowspan="4" style="text-align:center">
              <font size="2">e</font>
            </td>
            <td style="text-align:center">
              <font size="2">1..2</font>
            </td>
            <td>
              <font size="2">2</font>
            </td>
            <td rowspan="4">
              <font size="2">Local day of week. Same as E except numeric value will depend on the local starting day of the week. For this example, Monday is the first day of the week.</font>
            </td>
          </tr>
          <tr>
            <td style="text-align:center">
              <font size="2">3</font>
            </td>
            <td>
              <font size="2">Tues</font>
            </td>
          </tr>
          <tr>
            <td style="text-align:center">
              <font size="2">4</font>
            </td>
            <td>
              <font size="2">Tuesday</font>
            </td>
          </tr>
          <tr>
            <td style="text-align:center">
              <font size="2">5</font>
            </td>
            <td>
              <font size="2">T</font>
            </td>
          </tr>
        </tbody>
      </table>
      </th>
      <th width="50%">
        <table border="1" cellpadding="2" cellspacing="0">
          <tbody>
            <tr>
              <th rowspan="11">
                <font size="2">week<br />day</font>
              </th>
              <td rowspan="3" style="text-align:center">
                <font size="2">E</font>
              </td>
              <td style="text-align:center">
                <font size="2"><span style="background-color:rgb(255,255,0)">1..</span>3</font>
              </td>
              <td>
                <font size="2">Tues</font>
              </td>
              <td rowspan="3">
                <font size="2">Day of week - Use one through three letters for the short day, or four for the full name, or five for the narrow name.</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">4</font>
              </td>
              <td>
                <font size="2">Tuesday</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">5</font>
              </td>
              <td>
                <font size="2">T</font>
              </td>
            </tr>
            <tr>
              <td rowspan="4" style="text-align:center">
                <font size="2">e</font>
              </td>
              <td style="text-align:center">
                <font size="2">1..2</font>
              </td>
              <td>
                <font size="2">2</font>
              </td>
              <td rowspan="4">
                <font size="2">Local day of week. Same as E except<span style="background-color:rgb(255,255,0)">adds a</span> numeric value that will depend on the local starting day of the week, using one or two letters. For this example, Monday is the first day of the week.</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">3</font>
              </td>
              <td>
                <font size="2">Tues</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">4</font>
              </td>
              <td>
                <font size="2">Tuesday</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">5</font>
              </td>
              <td>
                <font size="2">T</font>
              </td>
            </tr>
            <tr>
              <td rowspan="4" style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">c</font>
              </td>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">1</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">2</font>
              </td>
              <td rowspan="4" style="background-color:rgb(255,255,0)">
                <b>
                  <font size="2">Stand-Alone</font>
                </b>
                <font size="2"> local day of week - Use one letter for the local numeric value (same as 'e'), three for the short day, or four for the full name, or five for the narrow name.</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">3</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">Tues</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">4</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">Tuesday</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center;background-color:rgb(255,255,0)">
                <font size="2">5</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">T</font>
              </td>
            </tr>
          </tbody>
        </table>
      </th>
    </tr>
    <tr>
      <td>
        <table border="1" cellpadding="2" cellspacing="0">
          <tbody>
            <tr>
              <th rowspan="6">
                <font size="2">zone</font>
              </th>
              <td rowspan="4" style="text-align:center">
                <font size="2">z</font>
              </td>
              <td style="text-align:center">
                <font size="2">1</font>
              </td>
              <td>
                <font size="2">PT</font>
              </td>
              <td rowspan="4">
                <font size="2">Time zone. Use 1 for short wall (generic) time, 2 for long wall time, 3 for the short time zone (i.e. PST) or 4 for the full name (Pacific Standard Time). If there's no name for the zone, fallbacks may be used, depending on available data.</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">2</font>
              </td>
              <td>
                <font size="2">Pacific Time</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">3</font>
              </td>
              <td>
                <font size="2">PDT</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">4</font>
              </td>
              <td>
                <font size="2">Pacific Daylight Time</font>
              </td>
            </tr>
            <tr>
              <td rowspan="2" style="text-align:center">
                <font size="2">Z</font>
              </td>
              <td style="text-align:center">
                <font size="2">1</font>
              </td>
              <td>
                <font size="2">GMT-08:00</font>
              </td>
              <td rowspan="2">
                <font size="2">Use 1 for GMT format, 2 for RFC 822</font>
              </td>
            </tr>
            <tr>
              <td style="text-align:center">
                <font size="2">2</font>
              </td>
              <td>
                <font size="2">-0800</font>
              </td>
            </tr>
          </tbody>
        </table>
      </td>
      <td width="50%">
        <table border="1" cellpadding="2" cellspacing="0">
          <tbody>
            <tr>
              <th rowspan="6">
                <font size="2">zone</font>
              </th>
              <td rowspan="2">
                <font size="2">z</font>
              </td>
              <td>
                <span style="background-color:rgb(255,255,0)"><font size="2">1..</font></span>
                <font size="2">3</font>
              </td>
              <td>
                <font size="2">PDT</font>
              </td>
              <td rowspan="2" style="background-color:rgb(255,255,0)">
                <font size="2">Time zone - Use one to three letters for the short time zone or four for the full name. For more information, see </font>
                <font size="2">Appendix J: </font>
                <a href="http://unicode.org/cldr/corrigenda.html#Time_Zone_Fallback" rel="nofollow">
                  <font size="2">Time Zone Fallback</font>
                </a>
              </td>
            </tr>
            <tr>
              <td>
                <font size="2">4</font>
              </td>
              <td>
                <font size="2">Pacific Daylight Time</font>
              </td>
            </tr>
            <tr>
              <td rowspan="2">
                <font size="2">Z</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">1..3</font>
              </td>
              <td>
                <font size="2">-0800</font>
              </td>
              <td rowspan="2">
                <font size="2">Use <span style="background-color:rgb(255,255,0)">one to three letters</span> for RFC 822, <span style="background-color:rgb(255,255,0)">four letters</span> for GMT format.</font>
              </td>
            </tr>
            <tr>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">4</font></td><td><font size="2">GMT-08:00</font>
                </td>
            </tr>
            <tr>
              <td rowspan="2" style="background-color:rgb(255,255,0)">
                <font size="2">v</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">1</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">PT</font>
              </td>
              <td rowspan="2" style="background-color:rgb(255,255,0)">
                <font size="2">Use one letter for short wall (generic) time, four for long wall time. For more information, see </font>
                <font size="2">Appendix J: </font>
                <a href="http://unicode.org/cldr/corrigenda.html#Time_Zone_Fallback" rel="nofollow">
                  <font size="2">Time Zone Fallback</font>
                </a>
              </td>
            </tr>
            <tr>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">4</font>
              </td>
              <td style="background-color:rgb(255,255,0)">
                <font size="2">Pacific Time</font>
              </td>
            </tr>
          </tbody>
        </table>
      </td>
    </tr>
  </tbody>
</table>
 
## List of Languages and Territories in this Release

**Languages (96):** Afar [Qafar]; Afrikaans; Albanian [shqipe]; Amharic [አማርኛ]; Arabic [‎العربية‎]; Armenian [Հայերէն]; Assamese [অসমীয়া]; Azerbaijani - Cyrillic [Азәрбајҹан - Cyrl]; Azerbaijani - Latin [azərbaycanca - Latn]; Basque [euskara]; Belarusian [Беларускі]; Bengali [বাংলা]; Blin [ብሊን]; Bosnian [Bosanski]; Bulgarian [Български]; Catalan [català]; Chinese - Simplified Han [中文 - 简体汉语]; Chinese - Traditional Han [中文 - 繁體漢語]; Cornish [kernewek]; Croatian [hrvatski]; Czech [Čeština]; Danish [Dansk]; Divehi [‎ދިވެހިބަސް‎]; Dutch [Nederlands]; Dzongkha [རྫོང་ཁ]; English; Esperanto; Estonian [Eesti]; Faroese [føroyskt]; Finnish [suomi]; French [français]; Gallegan [galego]; Geez [ግዕዝኛ]; Georgian [ქართული]; German [Deutsch]; Greek [Ελληνικά]; Gujarati [ગુજરાતી]; Hawaiian [ʻōlelo Hawaiʻi]; Hebrew [‎עברית‎]; Hindi [हिंदी]; Hungarian [magyar]; Icelandic [Íslenska]; Indonesian [Bahasa Indonesia]; Inuktitut [ᐃᓄᒃᑎᑐᑦ ᑎᑎᕋᐅᓯᖅ]; Irish [Gaeilge]; Italian [italiano]; Japanese [日本語]; Kalaallisut; Kannada [ಕನ್ನಡ]; Kazakh [Қазақ]; Khmer [ភាសាខ្មែរ]; Kirghiz [Кыргыз]; Konkani [कोंकणी]; Korean [한국어]; Lao [ລາວ]; Latvian [latviešu]; Lithuanian [Lietuvių]; Macedonian [македонски]; Malay [Bahasa Melayu]; Malayalam [മലയാളം]; Maltese [Malti]; Manx [Gaelg]; Marathi [मराठी]; Mongolian [Монгол хэл]; Norwegian Bokmål [norsk bokmål]; Norwegian Nynorsk [norsk nynorsk]; Oriya [ଓଡ଼ିଆ]; Oromo [Oromoo]; Pashto (Pushto) [‎پښتو‎]; Persian [‎فارسی‎]; Polish [polski]; Portuguese [português]; Punjabi [ਪੰਜਾਬੀ]; Romanian [Română]; Russian [русский]; Sanskrit [संस्कृत]; Serbian - Cyrillic [Српски - Ћирилица]; Serbian - Latin [Srpski - Latinica]; Sidamo [Sidaamu Afo]; Slovak [slovenský]; Slovenian [Slovenščina]; Somali [Soomaali]; Spanish [español]; Swahili [Kiswahili]; Swedish [svenska]; Syriac [‎ܣܘܪܝܝܐ‎]; Tamil [தமிழ்]; Tatar [Татар]; Telugu [తెలుగు]; Thai [ไทย]; Tigre [ትግረ]; Tigrinya [ትግርኛ]; Turkish [Türkçe]; Ukrainian [Українська]; Urdu [‎اردو‎]; Uzbek - Arabic [‎اۉزبېک - Араб‎]; Uzbek - Cyrillic [Ўзбек - Кирил]; Uzbek - Latin [oʿzbek - Lotin]; Vietnamese [Tiếng Việt]; Walamo [ወላይታቱ]; Welsh [Cymraeg]

**Territories (130):** Afghanistan [‎افغانستان‎]; Albania [Shqipëria]; Algeria [‎الجزائر‎]; American Samoa; Argentina; Armenia [Հայաստանի Հանրապետութիւն]; Australia; Austria [Österreich]; Azerbaijan [Azərbaycan, Азәрбајҹан]; Bahrain [‎البحرين‎]; Belarus [Беларусь]; Belgium [België, Belgien, Belgique]; Belize; Bhutan [འབྲུག]; Bolivia; Bosnia and Herzegovina [Bosna i Hercegovina, Босна и Херцеговина]; Botswana; Brazil [Brasil]; Brunei; Bulgaria [България]; Cambodia [កម្ពុជា]; Canada; Chile; China [中国]; Colombia; Costa Rica; Croatia [Hrvatska]; Cyprus [Κύπρος]; Czech Republic [Česká republika]; Denmark [Danmark]; Djibouti [Jabuuti, Yabuuti]; Dominican Republic [República Dominicana]; Ecuador; Egypt [‎مصر‎]; El Salvador; Eritrea [Eretria, ኤርትራ]; Estonia [Eesti]; Ethiopia [Itiyoophiya, Itoobiya, Itoophiyaa, Otobbia, ኢትዮጵያ]; Faroe Islands [Føroyar]; Finland [Suomi]; France; Georgia [საქართველო]; Germany [Deutschland]; Greece [Ελλάδα]; Greenland [Kalaallit Nunaat]; Guam; Guatemala; Honduras; Hong Kong S.A.R. China [中華人民共和國香港特別行政區]; Hungary [Magyarország]; Iceland [Ísland]; India [‎الهند‎, भारत, भारतम्, ভারত, ভাৰত, ਭਾਰਤ, ભારત, ଭାରତ, இந்தியா, భారత దేళ౦, ಭಾರತ, ഇന്ത്യ]; Indonesia; Iran [‎ایران‎]; Iraq [‎العراق‎]; Ireland [Éire]; Israel [‎ישראל‎]; Italy [Italia]; Jamaica; Japan [日本]; Jordan [‎الاردن‎]; Kazakhstan [Қазақстан]; Kenya [Keeniyaa, Kiiniya]; Kuwait [‎الكويت‎]; Kyrgyzstan [Кыргызстан]; Laos [ລາວ]; Latvia [Latvija]; Lebanon [‎لبنان‎]; Libya [‎ليبيا‎]; Liechtenstein; Lithuania [Lietuva]; Luxembourg [Luxemburg]; Macao S.A.R. China [澳門特別行政區]; Macedonia [Македонија]; Malaysia; Maldives [‎ދިވެހި ރާއްޖެ‎]; Malta; Marshall Islands; Mexico [México]; Monaco; Mongolia [Монгол улс]; Morocco [‎المغرب‎]; Netherlands [Nederland]; New Zealand; Nicaragua; Northern Mariana Islands; Norway [Noreg, Norge]; Oman [‎عمان‎]; Pakistan [‎پاکستان‎]; Panama [Panamá]; Paraguay; Peru [Perú]; Philippines; Poland [Polska]; Portugal; Puerto Rico; Qatar [‎قطر‎]; Romania [România]; Russia [Россия]; Saudi Arabia [‎العربية السعودية‎]; Serbia And Montenegro [Srbija i Crna Gora, Србија и Црна Гора]; Singapore [新加坡]; Slovakia [Slovenská republika]; Slovenia [Slovenija]; Somalia [Soomaaliya]; South Africa [Suid-Afrika]; South Korea [대한민국]; Spain [Espainia, España, Espanya]; Sudan [‎السودان‎]; Sweden [Sverige]; Switzerland [Schweiz, Suisse, Svizzera]; Syria [‎سورية‎, ‎ܣܘܪܝܝܐ‎]; Taiwan [臺灣]; Tanzania; Thailand [ประเทศไทย]; Trinidad and Tobago; Tunisia [‎تونس‎]; Turkey [Türkiye]; U.S. Virgin Islands; Ukraine [Украина, Україна]; United Arab Emirates [‎الامارات العربية المتحدة‎]; United Kingdom [Prydain Fawr, Rywvaneth Unys]; United States [Estados Unidos, ʻAmelika Hui Pū ʻIa]; United States Minor Outlying Islands; Uruguay; Uzbekistan [Oʿzbekiston, Ўзбекистон]; Venezuela; Vietnam [Việt Nam]; Yemen [‎اليمن‎]; Zimbabwe

Notes:

- The amount of data per locale and the status (draft or vetted) varies.
- Tooltips will show the ISO code and Latin transliteration if available.
  (To set your tooltip font in IE, right click on the desktop, pick Properties>Appearance>Advanced>Item: ToolTip, then set the font to Arial Unicode MS or other large font.)
- If the above doesn't display in your browser, see [Display Problems?]

[CLDR Releases (Downloads)]: /index/downloads
[Display Problems?]: https://www.unicode.org/help/display_problems.html
[UTS #35: Locale Data Markup Language (LDML)]: https://www.unicode.org/reports/tr35/
<!-- 1.3 release: 2005-06-02 -->
[Version1.3]: /downloads/cldr-1-3
[CLDR1.3]: https://unicode.org/Public/cldr/1.3.0/
[LDML1.3]: https://www.unicode.org/reports/tr35/tr35-4.html
[~~Δ1.3~~]: https://unicode.org/cldr/trac/query?status=closed&col=id&col=summary&milestone=1.3
[release-1-3]: https://github.com/unicode-org/cldr/tree/release-1-3
