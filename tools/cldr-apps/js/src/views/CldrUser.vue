<template>
  <span>
    <a-avatar v-if="!plain" size="small">
      <template #icon>
        <UserOutlined />
      </template>
    </a-avatar>

    <span v-if="!info"> User #{{ uid }} </span>
    <span v-if="noHover && info">{{ info.name }}</span>
    <a-popover :title="info.name + ' #' + info.userid" v-if="!noHover && info">
      <template #content>
        Org: {{ info.org }}
        <span class="userLevelName label-info label">{{ info.level }}</span>
        <br />
        Email: {{ info.email }}
      </template>
      {{ info.name }}
    </a-popover>
    <slot />
  </span>
</template>
<script>
import * as cldrUsers from "../esm/cldrUsers.mjs";
import { UserOutlined } from "@ant-design/icons-vue";

export default {
  data() {
    return {
      info: null,
      org: null,
      name: null,
    };
  },
  components: {
    UserOutlined,
  },
  computed: {},
  props: {
    uid: {
      type: String,
      default: null,
    },
    noHover: {
      type: Boolean,
      default: false,
    },
    plain: {
      type: String,
      default: false,
    },
  },
  async created() {
    this.info = await cldrUsers.getUserInfo(this.uid);
    const { org, name } = this.info;
    this.org = org || null;
    this.name = name || null;
    this.$emit("org", this.org);
  },
};
</script>

<style scoped>
.userchit {
  border: 1px solid navy;
}
</style>
