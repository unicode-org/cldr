const path = require("path");
const { VueLoaderPlugin } = require("vue-loader");
const { DefinePlugin } = require("webpack");

module.exports = (env, argv) => {
  const {mode} = argv;
  const DEV = (mode === 'development');
  return {
    entry: "./src/index.js",
    output: {
      filename: "bundle.js",
      path: path.resolve(__dirname, "..", "src", "main", "webapp", "dist"),
      library: "cldrBundle",
      libraryTarget: "var",
      libraryExport: "default",
      hashFunction: "xxhash64"
    },
    devtool: DEV ? "eval" : "source-map",
    module: {
      rules: [
        {
          test: /\.css$/i,
          use: ["style-loader", "css-loader"],
        },
        {
          test: /\.vue$/,
          loader: "vue-loader",
        },
      ],
    },
    resolve: {
      alias: {
        vue: DEV ? "vue/dist/vue.runtime.esm-browser.js" : "vue/dist/vue.runtime.esm-browser.prod.js"
      }
    },
    plugins: [
      new VueLoaderPlugin(),
      new DefinePlugin({
        // esm bundler flags,
        // see <https://github.com/vuejs/vue-next/tree/master/packages/vue#bundler-build-feature-flags>
        __VUE_PROD_DEVTOOLS__: JSON.stringify(DEV), // TODO: support dev mode
        __VUE_OPTIONS_API__:   JSON.stringify(true),
      })],
    };
};
