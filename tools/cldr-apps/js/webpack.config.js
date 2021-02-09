const path = require("path");
const { VueLoaderPlugin } = require("vue-loader");

module.exports = {
  entry: "./src/index.js",
  output: {
    filename: "bundle.js",
    path: path.resolve(__dirname, "..", "src", "main", "webapp", "dist"),
    library: "cldrBundle",
    libraryTarget: "var",
    libraryExport: "default",
  },
  mode: "development",
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
  plugins: [new VueLoaderPlugin()],
};
