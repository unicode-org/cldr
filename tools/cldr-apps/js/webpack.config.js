const path = require("path");
const fs = require('fs');
const { VueLoaderPlugin } = require("vue-loader");
const { DefinePlugin, Compiler } = require("webpack");

class SurveyToolPlugin {
  constructor() {

  }
  /**
   *
   * @param {Compiler} compiler
   */
  apply(compiler) {
    compiler.hooks.afterEmit.tap('SurveyToolPlugin', (compilation) => {
      const { assets } = compilation;
      const jsfiles = Object.keys(assets).filter(s => /\.js$/.test(s));
      const manifestFile = path.resolve(compiler.outputPath, 'manifest.json');
      fs.writeFileSync(manifestFile, JSON.stringify({ jsfiles }), 'utf-8');
      console.log('# SurveyToolPlugin Wrote: ', manifestFile);
    });
  }
};

module.exports = (env, argv) => {
  const {mode} = argv;
  const DEV = (mode === 'development');
  return {
    entry: "./src/index.js",
    cache: {
      type: 'filesystem',
      cacheDirectory: path.resolve(__dirname, '../target/webpack_cache'),
    },
    output: {
      filename: "[name].[contenthash].js",
      path: path.resolve(__dirname, "..", "src", "main", "webapp", "dist"),
      library: "cldrBundle",
      libraryTarget: "var",
      libraryExport: "default",
      hashFunction: "xxhash64"
    },
    devtool: DEV ? "eval-cheap-module-source-map" : "source-map",
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
        __VUE_OPTIONS_API__: JSON.stringify(true),
      }),
      new SurveyToolPlugin(),
    ]
  };
};
