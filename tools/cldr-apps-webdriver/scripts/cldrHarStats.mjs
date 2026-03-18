import { readFileSync } from "fs";

const URL_PREFIX = "http://localhost:9080/cldr-apps/";
const FILTER_REGEX = "api/voting/[a-zA-Z_]+/row";

/*
 * Run with three args: the HAR file name, the start time, and the end time. For example:
 * node scripts/cldrHarStats.mjs ../HAR/2024-01-12-a.har 2024-01-12T17:15:55Z 2024-01-12T17:18:55Z > ../HAR/2024-01-12-a.html
 *
 * Summary is written to stderr (console.warn); detailed HTML is written to stdout
 */
const harFileName = process.argv[2];
const startTimeStamp = process.argv[3];
const endTimeStamp = process.argv[4];
/*
 * Read the input HAR file into an array and filter it to include only entries
 * matching FILTER_REGEX and within the given start/end times
 */
const obj = JSON.parse(readFileSync(harFileName, "utf8"));
const allEntries = obj.log.entries;
const filteredEntries = allEntries.filter(function (entry) {
  return (
    entry.startedDateTime.localeCompare(startTimeStamp) >= 0 &&
    entry.startedDateTime.localeCompare(endTimeStamp) < 0 &&
    entry.request.url.match(FILTER_REGEX)
  );
});
console.warn("allEntries.length = " + allEntries.length);
console.warn("filteredEntries.length = " + filteredEntries.length);

writeHtmlThruTableStart();
writeHtmlTableHeader();
writeHtmlTableBody(filteredEntries);
writeHtmlFromTableEnd();

/**
 * Write the HTML up to and including the opening table tag
 */
function writeHtmlThruTableStart() {
  write("<html>");
  write("<head>");
  write(
    '<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />'
  );
  write("<style>");
  write("table {border-collapse: collapse;}");
  write("table, th, td {border: 1px solid black; padding: 4px;}");
  write("td:nth-child(3){text-align: right;}"); // ms
  write("td:nth-child(4){text-align: right;}"); // size
  write("</style>");
  write("</head>");
  write("<body>");
  write("<table>");
}

function writeHtmlFromTableEnd() {
  write("</table>");
  write("</body>");
  write("</html>");
}

function writeHtmlTableHeader() {
  const info = {
    isHeader: true,
    requestNumber: "request",
    startedDateTime: "start",
    time: "ms",
    size: "size",
    method: "method",
    url: "URL",
    postData: "POST data (if applicable)",
  };
  putEntryInfo(info);
}

function writeHtmlTableBody(entries) {
  let postTime = 0,
    getTime = 0,
    postCount = 0,
    getCount = 0;
  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];
    let url = entry.request.url;
    if (url.indexOf(URL_PREFIX) === 0) {
      url = url.slice(URL_PREFIX.length);
    }
    if (entry.request.method === "POST") {
      postCount++;
      postTime += entry.time;
    } else {
      getCount++;
      getTime += entry.time;
    }
    const postData =
      entry.request.method === "POST" && entry.request?.postData?.text
        ? entry.request.postData.text
        : "";
    const info = {
      isHeader: false,
      requestNumber: i + 1,
      startedDateTime: entry.startedDateTime,
      time: Math.round(parseFloat(entry.time)),
      size: Math.round(entry.response.content.size / 1000) + "k",
      method: entry.request.method,
      url: url,
      postData: postData,
    };
    putEntryInfo(info);
  }
  console.warn("Average POST time = " + postTime / postCount);
  console.warn("Average GET time = " + getTime / getCount);
}

function putEntryInfo(info) {
  const cellStart = info.isHeader ? "<th>" : "<td>";
  const cellEnd = info.isHeader ? "</th>" : "</td>";
  write("<tr>");
  write(cellStart + info.requestNumber + cellEnd);
  write(cellStart + info.startedDateTime + cellEnd);
  write(cellStart + info.time + cellEnd);
  write(cellStart + info.size + cellEnd);
  write(cellStart + info.method + cellEnd);
  write(cellStart + info.url + cellEnd);
  write(cellStart + info.postData + cellEnd);
  write("</tr>");
}

function write(string) {
  process.stdout.write(string + "\n");
}
