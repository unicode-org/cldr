// this file also gets bundled by webpack.
// it includes all of the other assets.

// module stylesheets need to go here. See cldrVue.mjs
import "bootstrap/dist/css/bootstrap.min.css";
import "bootswatch/dist/spacelab/bootstrap.min.css";
import "../../../cldr-code/src/main/resources/org/unicode/cldr/tool/reports.css";
import "ant-design-vue/dist/antd.min.css";

// TODO: ideally these would be loaded by webpack, but not quite working yet.
// import "jquery/dist/jquery.min.js";
// import "jquery-ui/dist/jquery-ui.min.js";
// import "jquery-ui/ui/widgets/tooltip.js";
// import "bootstrap/dist/js/bootstrap.min.js";

import "autosize/dist/autosize.min.js";
import "@fontsource/noto-sans/400.css";
import "@fontsource/noto-sans/700.css";
import "@fontsource/noto-sans/400-italic.css";
import "@fontsource/noto-sans/700-italic.css";
import "@fontsource/noto-sans-symbols/400.css";
import "@fontsource/noto-sans-symbols/700.css";
import "@fontsource/noto-nastaliq-urdu/arabic.css";

import "./css/cldrForum.css";
import "./css/redesign.css";
import "./css/survey.css";
