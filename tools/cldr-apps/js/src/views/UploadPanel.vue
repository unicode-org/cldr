<template>
  <div id="home">
    <div v-if="!surveyUser">
      <h1>This panel is only available if you are logged in.</h1>
    </div>
    <a-collapse v-if="surveyUser" v-model:activeKey="activeKey" accordion>
      <a-collapse-panel key="convert-xlsx" header="Convert XLSX to XML">
        <h4>
          How to use this: (Beta, see
          <a href="https://unicode-org.atlassian.net/browse/CLDR-16846"
            >CLDR-16846 for feedback</a
          >)
        </h4>
        <a-timeline>
          <a-timeline-item
            >Navigate to the Locale and Coverage Level of
            interest</a-timeline-item
          >
          <a-timeline-item>Open the Dashboard</a-timeline-item>
          <a-timeline-item
            >Click the <b>Download…</b> button in the Dashboard</a-timeline-item
          >
          <a-timeline-item
            >In the spreadsheet you download, add a new column such as 'New
            Value'</a-timeline-item
          >
          <a-timeline-item
            >Edit the spreadsheet to fill in the 'New Value'. Save the
            spreadsheet off as .xlsx (other formats are not supported.)
          </a-timeline-item>
          <a-timeline-item>Come back to this page</a-timeline-item>
          <a-timeline-item
            >Enter the locale id:
            <a-input
              style="width: 25em"
              :max-length="40"
              v-model:value="xlsLocale"
              placeholder="en"
          /></a-timeline-item>
          <a-timeline-item v-if="xlsLocale && !xlsLocaleName" color="red"
            >Correct the locale ID, it's not valid.</a-timeline-item
          >
          <a-timeline-item v-if="xlsLocaleName"
            >Confirm the locale: <b>{{ xlsLocaleName }}</b></a-timeline-item
          >
          <a-timeline-item v-if="xlsLocaleName"
            >Drop the spreadsheet into this upload area:
            <a-upload-dragger
              v-model:fileList="xlsFileList"
              name="xlsFile"
              :action="xlsUpload"
              :multiple="false"
              accept=".xls,.xlsx"
            >
              <p class="ant-upload-drag-icon">
                <inbox-outlined></inbox-outlined>
              </p>
              <p class="ant-upload-text">Click or drag a spreadsheet here</p>
              <p class="ant-upload-hint">Supports one file, of type .xlsx</p>
            </a-upload-dragger>
          </a-timeline-item>
          <a-timeline-item v-if="xlsErr" color="red"
            >Error with spreadsheet: {{ xlsErr }}</a-timeline-item
          >
          <a-timeline-item v-if="!xlsErr && xlsFileDone"
            >The spreadsheet <b>{{ xlsFileDone }}</b> will be converted into a
            .xml file
          </a-timeline-item>
          <a-timeline-item v-if="!xlsErr && xlsHeaders && xlsFileDone">
            Columns:
            <span v-for="header of xlsHeaders"> {{ header }} |</span>
            <br />
            XPath:
            <a-select
              v-model:value="xlsXpathColumn"
              :options="xlsHeaderOptions"
              placeholder="Choose XPath Column"
            />
            <br />
            Value:
            <a-select
              v-model:value="xlsValueColumn"
              :options="xlsHeaderOptions"
              placeholder="Choose Value Column"
            />
            <br />
          </a-timeline-item>
          <a-timeline-item
            v-if="!xlsErr && xlsXpathColumn && xlsValueColumn && !xlsXml"
          >
            <button @click="xlsGenerateXml">
              Generate XML
            </button></a-timeline-item
          >
          <a-timeline-item v-if="!xlsErr && xlsXml">
            <h4>{{ xlsLocale }}.xml</h4>
            <textarea cols="50" rows="20">{{ xlsXml }}</textarea
            ><br />
            <a-button @click="xmlDownload">Download…</a-button>
          </a-timeline-item>
          <a-timeline-item v-if="!xlsErr && xlsXml"
            >Use the 'Upload XML' section of this page to upload that XML
            file</a-timeline-item
          >
          <a-timeline-item v-if="!xlsErr && xlsXml"
            >Follow the prompts to import your vote. Done!</a-timeline-item
          >
        </a-timeline>

        <hr />
      </a-collapse-panel>

      <a-collapse-panel
        key="upload-xml"
        header="Upload XML as your vote (Bulk Upload)"
      >
        <a id="xmlup" target="_blank" :href="xmlUploadUrl"> Upload XML… </a> |
        <a href="https://cldr.unicode.org/index/survey-tool/bulk-data-upload"
          >Help on Bulk XML Upload</a
        >
      </a-collapse-panel>
    </a-collapse>
  </div>
</template>

<script>
import * as cldrBulkConverter from "../esm/cldrBulkConverter.mjs";
import * as cldrLoad from "../esm/cldrLoad.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";
import { ref } from "vue";
import { InboxOutlined } from "@ant-design/icons-vue";
import { fileSave } from "browser-fs-access";

export default {
  components: {
    InboxOutlined,
  },
  data: function () {
    return {
      activeKey: "",
      sessionId: cldrStatus.refs.sessionId,
      surveyUser: cldrStatus.refs.surveyUser,
      xlsLocale: null,
      xlsFileList: ref([]),
      xlsFileDone: null,
      xlsWb: null,
      xlsErr: null,
      xlsXml: null,
      xlsHeaders: ref([]),
      xlsXpathColumn: ref(null),
      xlsValueColumn: ref(null),
    };
  },
  created: function () {},
  methods: {
    xlsGenerateXml() {
      try {
        this.xlsXml = cldrBulkConverter.xlsGenerateXml(
          this.xlsLocale,
          this.xlsWb,
          this.xlsXpathColumn,
          this.xlsValueColumn
        );
      } catch (e) {
        console.error(e);
        this.xlsErr = e.message;
      }
    },
    xlsUpload(file) {
      this.xlsErr = null;
      this.xlsWb = null;
      this.xlsFileDone = null;
      return cldrBulkConverter.xlsUpload(file, (err, wb) => {
        if (err) {
          this.xlsErr = err;
          this.xlsFileDone = null;
          this.xlsWb = null;
        } else {
          this.xlsWb = wb;
          this.xlsFileDone = file.name;
        }
      });
    },
    xmlDownload() {
      const blob = new Blob([this.xlsXml], { type: "text/xml" });
      return fileSave(blob, {
        fileName: `${this.xlsLocale}.xml`,
        extensions: [".xml"],
      });
    },
  },
  watch: {
    xlsWb() {
      if (!this.xlsWb) {
        this.xlsHeaders = null;
        return;
      }
      try {
        this.xlsHeaders = cldrBulkConverter.xlsHeaders(this.xlsWb);
      } catch (e) {
        console.error(e);
        this.xlsErr = e.message;
      }
    },
  },
  computed: {
    xlsHeaderOptions() {
      if (!this.xlsHeaders) return null;
      return this.xlsHeaders.map((value) => ({ value }));
    },
    xlsLocaleName() {
      if (!this.xlsLocale) {
        return null;
      }
      const locmap = cldrLoad.getTheLocaleMap();
      const info = locmap.getLocaleInfo(this.xlsLocale);
      if (!info) {
        return null;
      }
      return `${info.name} / ${info.bcp47}`;
    },
    xmlUploadUrl() {
      const p = new URLSearchParams();
      p.append("s", this.sessionId);
      p.append("email", this.surveyUser.email);
      const url = "upload.jsp?" + p.toString();
      return url;
    },
  },
};
</script>

<style scoped>
#xmlup {
  font-size: x-large;
}
</style>
