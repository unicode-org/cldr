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

/** replace a/b/c.html with a/b/c.md */
function html2md(p) {
  return p.replace(/\.html$/, ".md");
}

/** load and cook the site data */
async function siteData() {
  // load the json
  const d = await fetch("/assets/json/tree.json");
  const j = await d.json();
  const { all } = j;

  // 'all' is an array of { title, fullPath } entries.
  // Flat list of paths
  const allPaths = all.map(({ fullPath }) => fullPath);
  // Find all 'directories' (ending with /)
  const allDirs = new Set();
  allPaths.forEach((p) => {
    const segs = p.split("/").slice(0, -1); // ['', 'dir1']
    for (let n = 0; n <= segs.length; n++) {
      // add all parent paths, so: '', dir1, dir1/dir2 etc.
      const subpath = segs.slice(0, n).join("/");
      allDirs.add(subpath);
    }
  });
  j.allDirs = {};
  j.allIndexes = [];
  // allDirs:  '', index, downloads, etc…
  allDirs.forEach((dir) => {
    // presumed index page:  /downloads -> /downloads.md
    // also / -> /index.md
    const dirIndex = `${dir || "index"}.md`;
    // console.dir({dir, dirIndex});
    if (allPaths.indexOf(dirIndex) !== -1) {
      j.allDirs[dir] = { index: dirIndex };
      j.allIndexes.push(dirIndex);
    } else {
      console.error(`No index page: ${dirIndex}`);
      j.allDirs[dir] = {};
    }
    j.allDirs[dir].pages = [];
  });
  allPaths.forEach((p) => {
    const dir = path2dir(p);
    j.allDirs[dir].pages.push(p);
  });
  // map md -> title
  j.title = {};
  all.forEach(({ title, fullPath }) => (j.title[fullPath] = title));
  return j;
}

const app = Vue.createApp(
  {
    setup(props) {
      // the tree.json data
      const tree = ref({});
      // loading status for tree.json
      const status = ref(null);
      // is the popup menu shown?
      const popup = ref(false);

      return {
        tree,
        status,
        popup,
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
      mdPath() {
        if (this.path) {
          return html2md(this.path);
        }
        return null;
      },
      ourDir() {
        if (this.path) {
          return path2dir(this.path);
        }
        return "";
      },
      ourIndex() {
        if (this.tree?.value) {
          // first ARE we an index page?
          if (this.tree.value.allIndexes.indexOf(this.mdPath) != -1) {
            return this.mdPath; // we are an index
          }
          return this.tree.value.allDirs[this.ourDir].index;
        }
        return null;
      },
      ourIndexHtml() {
        if (this.ourIndex) {
          return md2html(this.ourIndex);
        } else {
          return null;
        }
      },
      ourIndexTitle() {
        if (this.ourIndex && this.tree?.value) {
          return this.tree.value.title[this.ourIndex] || this.ourIndex;
        } else {
          return null;
        }
      },
      ourTitle() {
        if (this.tree?.value) {
          if (this.path === "") return this.rootTitle;
          return this.tree.value.title[html2md(this.path)];
        }
      },
      // title of root
      rootTitle() {
        return this.tree?.value?.title["index.md"];
      },
      // list of pages for siblings of this dir
      siblingPages() {
        if (!this.tree?.value) return [];
        let dirForPage = this.ourDir;
        if (this.tree.value.allIndexes.indexOf(this.mdPath) != -1) {
          const dirPages = Object.entries(this.tree?.value?.allDirs).filter(
            ([k, { index }]) => index == this.mdPath
          )[0];
          if (dirPages) {
            // our page is an index -so, show the subpages instead of the siblings.
            dirForPage = dirPages[0]; // the adjusted index
          } else {
            return []; // no sibling pages;
          }
        } else {
          return []; // no sibling pages
        }
        let thePages = this.tree?.value?.allDirs[dirForPage].pages ?? [];
        if (dirForPage === "") {
          thePages = [...thePages, ...this.tree?.value?.allDirs["index"].pages];
        }
        const c = new Intl.Collator([]);
        const t = this;
        return thePages
          .map((path) => ({
            path,
            html: md2html(path),
            title: this.tree.value.title[path] ?? path,
          }))
          .sort((a, b) => c.compare(a.title, b.title))
          .filter(({ html }) => html != t.path); // skip showing the index page in the subpage list
      },
      ancestorPages() {
        const pages = [];
        // if we are not loaded, or if we're at the root, then exit
        if (!this.tree?.value || !this.path || this.path == "index.html")
          return pages;
        // traverse
        let path = this.path;
        do {
          // calculate the immediate ancestor
          const pathMd = html2md(path);
          const dir = path2dir(path);
          const nextIndex = this.tree.value.allDirs[dir].index || "index.md"; // falls back to top
          const nextIndexHtml = md2html(nextIndex);
          const nextIndexTitle = this.tree.value.title[nextIndex];
          // prepend
          pages.push({
            href: "/" + nextIndexHtml,
            title: nextIndexTitle,
          });
          if (nextIndexHtml == path) {
            console.error("Loop detected from " + this.path);
            path = "index.html"; // exit
          }
          path = nextIndexHtml;
        } while (path && path != "index.html"); // we iterate over 'path', so html
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

       <span class="ancestor" v-for="ancestor of ancestorPages" :key="ancestor.href">
         <a class="uplink" v-bind:href="ancestor.href">{{ ancestor.title }}</a><span class="crumb">❱</span>
       </span>
       <div v-if="!siblingPages || !siblingPages.length" class="title"> {{ ourTitle }} </div>
       <div v-else class="title"  @mouseover="popup = true"><span class="hamburger" @click="popup = !popup">≡</span>
            {{ ourTitle }}

        <div class="subpages" v-if="popup">
          <span class="hamburger" @click="popup=false">✕</span>
          <ul class="subpages" >
              <li v-for="subpage of siblingPages" :key="subpage.path">
                  <span v-if="path == subpage.html">
                      <b>{{ subpage.title }}</b>
                  </span>
                  <a v-else v-bind:href="'/'+subpage.html">
                      {{ subpage.title }}
                  </a>
              </li>
          </ul>
        </div>


      </div>

    </div>`,
  },
  {
    // path of / goes to /index.html
    path: myPath,
  }
);

// app.component("CldrPage", {
//   setup() {},
//   template: `<p>Hello</p>
//         `,
// });

// app.component("CldrList", {
//   setup() {},
//   template: `
//         <p>Hullo</p>
//         `,
// });

app.mount("#nav");
