const { ref } = Vue;

// load anchor.js - must be before the sidebar loads. might as well do this
// first thing.
anchors.add("h1, h2, h3, h4, h5, h6");

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
          // this
          let objects = this.anchorElements?.map(
            ({ textContent, id, tagName }) => ({
              title: textContent,
              href: `#${id}`,
              children: null,
              style: `heading${tagName}`,
            })
          );
          if (objects[0]?.title === this.ourTitle) {
            objects = objects.slice(1);
          }
          if (!objects?.length) return null;
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
