<template>
  <div v-if="!showUnreadOnly || !announcement.checked">
    <div class="announcementBox">
      <section class="fullJustified">
        <span class="announcementToFrom">
          From: {{ announcement.posterName }}
        </span>
        <span class="announcementDate">
          {{ announcement.date }}
        </span>
      </section>
      <section class="announcementToFrom">
        To: {{ announcement.audience }} •
        {{ announcement.orgsAll ? "All organizations" : "Your organization" }} •
        {{
          announcement.locs ? "Locale(s): " + announcement.locs : "All locales"
        }}
      </section>
      <section class="announcementSubject">
        {{ announcement.subject }}
      </section>
      <div v-html="bodyHtml" class="announcementBody"></div>
      <div class="rightControl">
        <input
          type="checkbox"
          v-model="announcement.checked"
          id="alreadyReadChecked"
          @change="
            (event) => {
              checkmarkChanged(event, announcement);
            }
          "
        /><label for="alreadyReadChecked">&nbsp;I have read this</label>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: ["announcement", "checkmarkChanged", "showUnreadOnly"],

  computed: {
    /**
     * Get an html version of the body text, replacing newlines with br tags,
     * and making http(s) URLs into clickable links.
     */
    bodyHtml() {
      return this.announcement.body
        .replaceAll("\n", "\n<br>\n")
        .replaceAll(/(http\S+)/g, "<a href='$1'>$1</a>");
    },
  },
};
</script>

<style scoped>
.announcementBox {
  /* imitate forum style slightly */
  background-color: #f5f5f5;
  border: 1px solid #e3e3e3;
  border-radius: 3px;
  padding: 1em;
  margin: 1em;
}

.announcementToFrom {
  font-weight: bold;
  margin-left: 0;
  margin-right: auto;
  color: #40a9ff;
}

.announcementSubject {
  font-weight: bold;
  font-size: larger;
}

.announcementDate {
  display: block;
  margin-left: auto;
  margin-right: 0;
}

.announcementBody {
  border: 1px solid #1890ff;
  margin: 1ex;
  padding: 1ex;
}

.fullJustified {
  display: flex;
  justify-content: space-between;
  flex-direction: row;
  align-items: baseline;
}

.rightControl {
  display: flex;
  justify-content: flex-end;
  text-align: baseline;
}

label {
  font-weight: normal;
  margin: 0; /* override bootcamp */
}
</style>
