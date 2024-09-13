const { ref } = Vue;

// site management

let myPath = window.location.pathname.slice(1) || "index.html";
if (!/\.html/.test(myPath)) {
  myPath = `${myPath}.html`; // cloudflare likes to drop the .html
}

/** replace a/b/c.md with a/b */
function path2dir(p) {
  const dir = p.split("/").slice(0, -1).join("/");
  return dir;
}

/** replace a/b/c.md with a/b/c.html */
function md2html(p) {
  return p.replace(/\.md$/, ".html");
}

/** replace a/b/c with to /a/b/c, also '' => '/' */
function path2url(p) {
  if (p === "index") {
    return "/";
  }
  return `/${p}`;
}

/** replace a/b/c.html with a/b/c.md */
function html2md(p) {
  return p.replace(/\.html$/, ".md");
}

/** replace a/b/c.md with a/b/c */
function dropmd(p) {
  return p.replace(/\.md$/, "");
}

/** replace a/b/c.html with a/b/c */
function drophtml(p) {
  return p.replace(/\.html$/, "").replace(/\/$/, "");
}

/** load and cook the site data */
async function siteData() {
  // load the json
  const d = await fetch("/assets/json/tree.json");
  const j = await d.json();
  const { usermap } = j;

  return j;
}

const AncestorPages = {
  props: ["ancestorPages"],
  setup() {},
  template: `
       <span class="ancestor" v-for="ancestor of ancestorPages" :key="ancestor.href">
         <a class="uplink" v-bind:href="ancestor.href">{{ ancestor.title }}</a><span class="crumb">❱</span>
       </span>
`,
};

const SubPagesPopup = {
  props: {
    children: {
      type: Object,
      required: true,
    },
  },
  emits: ["hide"], // when user clicks hide
  setup() {},
  methods: {
    hide() {
      this.$emit("hide");
    },
  },
  template: `
          <div class="subpages">
          <span class="hamburger" @click="hide()">✕</span>
          <ul class="subpages" >
              <li v-for="subpage of children" :key="subpage.path">
                  <a v-bind:href="subpage.href">
                      {{ subpage.title }}
                       <span class="hamburger" v-if="subpage.children">❱</span>
                  </a>
              </li>
          </ul>
        </div>
`,
};

const SubMap = {
  name: "SubMap",
  props: {
    usermap: {
      type: Object,
      required: true,
    },
    path: String,
  },
  setup() {},
  computed: {
    title() {
      return this.usermap[this.path].title;
    },
    children() {
      return this.usermap[this.path].children || [];
    },
    href() {
      return path2url(this.path);
    },
  },
  template: `
    <div class="submap">
      <a v-bind:href="href" v-bind:title="path">{{title}}</a>
      <SubMap v-for="child in children" :key="child" :usermap="usermap" :path="child" />
    </div>
  `,
};

const SiteMap = {
  props: {
    tree: {
      type: Object,
      required: true,
    },
  },
  components: {
    SubMap,
  },
  emits: ["hide"],
  setup() {},
  methods: {
    hide() {
      this.$emit("hide");
    },
  },
  template: `
    <div class="sitemap">
          <span class="hamburger" @click="hide()">✕</span>
          <b>Site Map</b><hr/>
          <SubMap :usermap="tree.value.usermap" path="index"/>
    </div>
  `,
};

const app = Vue.createApp(
  {
    components: {
      AncestorPages,
      SubPagesPopup,
      SiteMap,
    },
    setup(props) {
      // the tree.json data
      const tree = ref({});
      // loading status for tree.json
      const status = ref(null);
      // is the popup menu shown?
      const popup = ref(false);
      // is the site map shown?
      const showmap = ref(false);

      return {
        tree,
        status,
        popup,
        showmap,
      };
    },
    mounted() {
      const t = this;
      siteData().then(
        (d) => (t.tree.value = d),
        (e) => (t.status = e)
      );
    },
    props: {
      path: String,
    },
    computed: {
      /** base path:  'index' or 'downloads/cldr-33' */
      base() {
        if (this.path) {
          return drophtml(this.path);
        } else {
          return "index"; // '' => 'index'
        }
        return null;
      },
      ourTitle() {
        if (this.tree?.value) {
          if (this.path === "") return this.rootTitle;
          return this?.tree?.value?.usermap[this.base]?.title;
        }
      },
      // title of root
      rootTitle() {
        const usermap = this?.tree?.value?.usermap ?? {};
        return usermap?.index?.title ?? "CLDR";
      },
      children() {
        const usermap = this?.tree?.value?.usermap;
        if (!usermap) return []; // no children
        const entry = usermap[this.base];
        const children = entry?.children;
        if (!children || !children.length) return [];
        return children.map((path) => ({
          path,
          href: path2url(path),
          title: usermap[path]?.title || path,
          children: (usermap[path].children ?? []).length > 0,
        }));
      },
      ancestorPages() {
        const pages = [];
        // if we are not loaded, or if we're at the root, then exit
        const usermap = this?.tree?.value?.usermap;
        if (
          !usermap ||
          !this.path ||
          this.path == "index.html" ||
          this.map == "index"
        ) {
          return [];
        }
        // traverse
        let path = drophtml(this.path); // can't be null, empty, or index (see above). Map a/b/c.html to a/b/c
        do {
          // calculate the immediate ancestor
          const nextParentPath = usermap[path]?.parent;
          if (!nextParentPath) break;
          if (nextParentPath == path) {
            console.error("Loop detected!");
            break;
          }
          const nextParent = usermap[nextParentPath];
          if (!nextParent) break;
          const href = path2url(nextParentPath);
          const { title } = nextParent || nextParentPath;
          // prepend
          pages.push({
            href,
            title,
            path: nextParentPath,
          });
          path = nextParentPath;
        } while (path); // we iterate over 'path' until it returns null
        pages.reverse();
        return pages;
      },
    },
    template: `
    <div>
       <div class='status' v-if="status">{{ status }}</div>
       <div class='status' v-if="!tree">Loading…</div>
       <a class="icon" href="http://www.unicode.org/"> <img border="0"
					src="/assets/img/logo60s2.gif"  alt="[Unicode]" width="34"
					height="33"></a>&nbsp;&nbsp;

       <AncestorPages :ancestorPages="ancestorPages"/>

       <div v-if="!children || !children.length" class="title"> {{ ourTitle }} </div>

       <div v-else class="title"  @mouseover="popup = true"><span class="hamburger" @click="showmap=false,popup = !popup">≡</span>

            {{ ourTitle }}

        <SubPagesPopup v-if="popup && !showmap" @hide="popup = false" :children="children"/>

      </div>
        <div class="showmap" v-if="!showmap" @click="popup=false,showmap = true">Site Map</div>
        <SiteMap v-if="tree && showmap" :tree="tree" @hide="showmap = popup = false"  />

    </div>`,
  },
  {
    // path of / goes to /index.html
    path: myPath,
  }
);

app.mount("#nav");

// load anchor.js
anchors.add("h1, h2, h3, h4, h5, h6, caption, dfn");
