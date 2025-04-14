const { ref } = Vue;

// load anchor.js - must be before the sidebar loads. might as well do this
// first thing.
anchors.add("h1, h2, h3, h4, h5, h6");

const coll = Intl.Collator([], {
  usage: "search",
  sensitivity: "base",
  ignorePunctuation: "true",
});

/**
 * @param {string} t string to search for
 * @param {string} full full text to search on
 * @returns
 */
function searchMatch(t, full) {
  // normalize input strings
  t = t.trim().toLowerCase();
  full = full.trim().toLowerCase();

  // exact exact match
  if (t == full) return true;
  // substring match
  if (full.includes(t)) return true;
  // next, try search collator
  if (coll.compare(t, full) == 0) return true;

  // sorry, no match
  return false;
}

/** flatten anchor.elements into a flat map */
function processAnchorElements(anchorElements) {
  if (!anchorElements) return [];
  let objects = anchorElements?.map(({ textContent, id, tagName }) => ({
    title: textContent,
    href: `#${id}`,
    children: null,
    style: `heading${tagName}`,
  }));
  if (objects[0]?.title === this.ourTitle) {
    objects = objects.slice(1);
  }
  if (!objects?.length) return null;
  return objects;
}

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

/**
 * Single Promise for site data.
 */
const siteDataPromise = siteData();

const AncestorPages = {
  props: ["ancestorPages"],
  setup() {},
  template: `
       <span class="ancestor" v-for="ancestor of ancestorPages" :key="ancestor.href">
         <a class="uplink" v-bind:href="ancestor.href">{{ ancestor.title }}</a><span class="crumb">❱</span>
       </span>
`,
};

const SubPageEntry = {
  props: {
    children: Boolean,
    title: String,
    href: String,
  },
  computed: {
    style() {
      if (this.children) {
        return "hasChildren";
      } else {
        return "";
      }
    },
  },
  template: `
      <a v-bind:href="href" v-bind:class="style">
        {{ title }}
      </a>
  `,
};

const SubPagesPopup = {
  props: {
    children: {
      type: Object,
      required: true,
    },
  },
  components: {
    SubPageEntry,
  },
  emits: ["hide"], // when user clicks hide
  setup() {},
  methods: {
    hide() {
      this.$emit("hide");
    },
  },
  template: `
          <div class="subpageList">
          <div class="navHeader">Subpages</div>
          <ul class="subpages" >
              <li v-for="subpage of children" :key="subpage.path">
                <SubPageEntry :children="subpage.children" :title="subpage.title" :href="subpage.href" />
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
      if (!this.usermap) return "";
      return this.usermap[this.path]?.title;
    },
    children() {
      if (!this.usermap) return [];
      return this.usermap[this.path]?.children || [];
    },
    href() {
      if (!this.usermap) return "";
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

const SubContents = {
  name: "SubContents",
  props: {
    title: String,
    href: String,
    children: Object,
    path: String,
    style: String,
  },
  setup() {},
  template: `
    <div class="submap" v-bind:class="style">
      <a v-bind:href="href" v-bind:title="path">{{title}}</a>
      <SubContents v-if="children && children.length" v-for="child in children" :key="child.href" :title="child.title" :href="child.href" :children="child.children" />
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
          <SubMap :usermap="tree?.value?.usermap" path="index"/>
    </div>
  `,
};

const PageContents = {
  components: {
    SubContents,
  },
  props: {
    children: {
      type: Object,
      required: true,
    },
  },
  setup() {},
  template: `
    <div class="pagecontents">
        <div class="navHeader">Contents</div>
        <SubContents v-if="children && children.length" v-for="child in children" :key="child.href" :style="child.style" :title="child.title" :href="child.href" :children="child.children" />
    </div>
  `,
};

const SearchBox = {
  components: {},
  props: {
    // sitemap
    tree: {
      type: Object,
      required: true,
    },
    max: {
      type: Number,
      default: 5,
      required: false,
    },
  },
  methods: {
    // handler: check for enter key
    keyup(event) {
      if (event.key === "Enter" || event?.keycode === 13) {
        this.webSearch();
      }
    },
    // update the search box
    updateSearch(event) {
      this.searchText = event.target.value;
      const t = this.searchText?.trim();
      this.clearSearchResults();
      if (t) {
        this.localSearch();
      }
    },
    // do the google search
    webSearch() {
      this.clearSearchResults(); // as we are leaving this page
      const text = this.searchText;
      if (!text || !text.trim()) return;
      const u = new URL(
        "https://www.google.com/search?q=site%3Acldr.unicode.org%2F+"
      );
      let q = u.searchParams.get("q");
      q = q + text; // append their search
      u.searchParams.set("q", q);
      document.location.assign(u); // Go!
    },
    // handle the X (clear) button
    clearSearch() {
      this.searchText = "";
      this.clearSearchResults();
    },
    clearSearchResults() {
      this.searchResults = [];
      this.headerResults = [];
    },
    // attempt a local search
    localSearch() {
      const t = this.searchText?.trim();
      this.clearSearchResults();
      if (t.length <= 1) return; // don't search on one letter
      const pathAndTitle = Object.entries(this.tree.value.usermap).map(
        ([href, { title }]) => ({ href, title })
      );
      this.headerResults = [
        ...this.pageContents
          .filter(({ title }) => searchMatch(t, title))
          // don't match the H1
          .filter(({ style }) => style != "headingH1"),
      ];
      this.searchResults = [
        ...pathAndTitle
          .filter(({ href, title }) => searchMatch(t, title))
          // need to add a preceding slash to the href
          .map(({ href, title }) => ({ href: `/${href}`, title })),
      ];
      if (!this.searchResults.length) {
        this.searchResults = [
          ...pathAndTitle
            .filter(({ href, title }) => searchMatch(t, href))
            // need to add a preceding slash to the href
            .map(({ href, title }) => ({ href: `/${href}`, title })),
        ];
      }
    },
  },
  setup(props) {
    const searchText = ref("");
    const headerResults = ref([]);
    const searchResults = ref([]);
    const pageContents = ref(processAnchorElements(anchors.elements));
    return {
      searchText,
      headerResults,
      searchResults,
      pageContents,
    };
  },
  template: `
    <input size="30" placeholder="Search CLDR…" @keyup="keyup" :value="searchText" @input="updateSearch"/><button id="searchbutton" title="search" @click="webSearch">🔎</button>
    <button v-show="searchText" id="clearsearch" title="clear search" @click="clearSearch">✕</button>
    <div class="searchResults" v-if="headerResults?.length">
      <i>Matching headings on this page:</i>
      <ul>
        <li v-for="r of headerResults.slice(0,max)" :key="href">
          <a :href="r.href">{{ r.title }}</a>
        </li>
        <li class="searchMax" v-show="headerResults?.length > max">…</li>
      </ul>
    </div>
    <div class="searchResults" v-if="searchResults?.length">
      <i>Matching page titles:</i>
      <ul>
        <li v-for="r of searchResults.slice(0,max)" :key="href">
          <a :href="r.href">{{ r.title }}</a>
        </li>
        <li class="searchMax" v-show="searchResults?.length > max">…</li>
      </ul>
      <i v-if="searchResults">or, press Enter to search the site.</i>
    </div>
  `,
};

const app = Vue.createApp(
  {
    components: {
      AncestorPages,
      SubPagesPopup,
      SiteMap,
      SearchBox,
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
      siteDataPromise.then(
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
       <a class="icon" href="https://www.unicode.org/"> <img border="0"
					src="/assets/img/logo60s2.gif"  alt="[Unicode]" width="34"
					height="33"></a>&nbsp;&nbsp;

       <div class="breadcrumb">
        <AncestorPages :ancestorPages="ancestorPages"/>

        </div>
        <div id="searchbox">
          <SearchBox :tree="tree" />
        </div>
       </div>
    </div>`,
  },
  {
    // path of / goes to /index.html
    path: myPath,
  }
);

app.mount("#nav");

if (myPath === "sitemap.html") {
  // for now: duplicate app including sitemap
  const sapp = Vue.createApp(
    {
      components: {
        SiteMap,
      },
      setup(props) {
        // the tree.json data
        const tree = ref({});
        // loading status for tree.json
        const status = ref(null);
        // is the site map shown?
        const showmap = ref(true);

        return {
          tree,
          status,
          showmap,
        };
      },
      mounted() {
        const t = this;
        siteDataPromise.then(
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
      },
      template: `
      <div>
         <div class='status' v-if="status">{{ status }}</div>
         <div class='status' v-if="!tree">Loading…</div>
          <SiteMap v-if="true" :tree="tree" @hide="showmap = popup = false"  />

      </div>`,
    },
    {
      // path of / goes to /index.html
      path: myPath,
    }
  );
  sapp.mount("#sitemap");
} else {
  // NOT in sitemap - mount the view app for the in-page sidebar
  const sapp = Vue.createApp(
    {
      components: {
        PageContents,
        SubPagesPopup,
      },
      setup(props) {
        // the tree.json data
        const tree = ref({});
        // loading status for tree.json
        const status = ref(null);
        // is the site map shown?
        const showmap = ref(true);

        return {
          tree,
          status,
          showmap,
        };
      },
      mounted() {
        const t = this;
        siteDataPromise.then(
          (d) => {
            t.tree.value = d;
            // set style on first header if redundant
            if (this.anchorElements[0]?.textContent === this.ourTitle) {
              this.anchorElements[0].className = "redundantTitle";
            }
          },
          (e) => (t.status = e)
        );
      },
      props: {
        path: String,
        anchorElements: Object,
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
        contents() {
          // For now we generate a flat map
          const objects = processAnchorElements(this.anchorElements);
          return objects;
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
      },
      template: `
      <div>
      <div class="navBar" v-if="contents || (children.length)">
        <PageContents v-if="contents" :children="contents" />
        <SubPagesPopup v-if="children.length" :children="children"/>
      </div>
      <h1 class="title">{{ ourTitle }}</h1>
      </div>`,
    },
    {
      // path of / goes to /index.html
      path: myPath,
      anchorElements: anchors.elements,
    }
  );
  sapp.mount("#sidebar");
}
