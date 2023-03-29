const path = require("path");
const { VueLoaderPlugin } = require("vue-loader");
const { DefinePlugin } = require("webpack");

module.exports = {
  entry: "./src/index.js",
  output: {
    filename: "bundle.js",
    path: path.resolve(__dirname, "..", "src", "main", "webapp", "dist"),
    library: "cldrBundle",
    libraryTarget: "var",
    libraryExport: "default",
    hashFunction: "xxhash64"
  },
  mode: "production", // TODO: support dev mode
  devtool: "source-map",
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
      vue: "vue/dist/vue.runtime.esm-browser.prod.js" // TODO: support dev mode
    }
  },
  plugins: [
    new VueLoaderPlugin(),
    new DefinePlugin({
      // esm bundler flags,
      // see <https://github.com/vuejs/vue-next/tree/master/packages/vue#bundler-build-feature-flags>
      __VUE_PROD_DEVTOOLS__: JSON.stringify(false), // TODO: support dev mode
      __VUE_OPTIONS_API__:   JSON.stringify(true),
    })],
};
