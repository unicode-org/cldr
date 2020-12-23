/**
 * Statistics page
 * @module statistics
 */
define("js/special/statistics.js", [
  "js/special/SpecialPage.js",
  "dojo/number",
  "dojox/charting/Chart",
  "dojox/charting/axis2d/Default",
  "dojox/charting/plot2d/StackedBars",
  "dojox/charting/widget/SelectableLegend",
  "dojox/charting/themes/Distinctive", //http://archive.dojotoolkit.org/nightly/dojotoolkit/dojox/charting/tests/test_themes.html
  "dojox/charting/action2d/Tooltip",
  "dojo/domReady!",
], function (
  SpecialPage,
  dojoNumber,
  Chart,
  axis2dDefault,
  StackedBars,
  SelectableLegend,
  Wetland,
  Tooltip
) {
  var _super;

  function Page() {
    this.sections = {};
    for (var k = 0; k < this.sectionArray.length; k++) {
      var aSection = this.sectionArray[k];
      if (!aSection.url && aSection.url !== false) {
        aSection.url =
          cldrStatus.getContextPath() +
          "/SurveyAjax?&what=stats_" +
          aSection.name;
      }
      this.sections[aSection.name] = aSection;
      if (aSection.isDefault) {
        this.curSection = aSection.name;
      }
    }
  }

  _super = Page.prototype = new SpecialPage();

  Page.prototype.sectionArray = [
    {
      isDefault: true,
      name: "overview",
      url: cldrStatus.getContextPath() + "/SurveyAjax?&what=stats_byloc",
      show: function (json, theDiv, params) {
        theDiv.appendChild(
          createChunk(
            "For CLDR version " + cldrStatus.getNewVersion(),
            "h2",
            "helpContent"
          )
        );
        theDiv.appendChild(
          createChunk(
            "Total submitters: " +
              dojoNumber.format(json.total_submitters) +
              ", Total items: " +
              dojoNumber.format(json.total_items) +
              " (" +
              dojoNumber.format(json.total_new_items) +
              " new)",
            "p",
            "helpContent"
          )
        );
      },
    },
    {
      name: "byday",
      show: function (json, theDiv, params) {
        var statDiv = theDiv;
        // munge data
        var header = json.byday.header;
        var data = json.byday.data;
        var header_new = json.byday_new.header;
        var data_new = json.byday_new.data;
        var count_old = [];
        var labels = [];
        var count_new = [];
        statDiv.style.height = data_new.length * 1 + "em";

        count_old.push(0);
        count_new.push(0);
        labels.push({ value: 0, text: "" });

        for (var i in data_new) {
          const theDate = new Date(data_new[i][header_new.DATE]);
          // get first and last days

          var newLabel = theDate.toLocaleDateString();
          var newCount = Number(data_new[i][header_new.COUNT]);
          labels.push({ value: Number(i) + 1, text: newLabel }); // labels come from new data
          count_new.push(newCount);
          var oldLabel = new Date(data[i][header.DATE]).toLocaleDateString();
          if (newLabel == oldLabel) {
            // have old data
            var oldCount = Number(data[i][header.COUNT]);
            if (oldCount < newCount) {
              console.log(
                "Old data must be ≥ new data: however, " +
                  newLabel +
                  ": " +
                  oldCount +
                  " oldCount < " +
                  newCount +
                  "  newCount "
              );
              count_old.push(-1);
            } else {
              count_old.push(oldCount - newCount);
            }
          } else {
            console.log(
              "Something out of sync, Old and new labels don’t match: " +
                newLabel +
                " / " +
                oldLabel
            );
            count_old.push(-1);
          }
        }

        var c = new Chart(statDiv);
        c.addPlot("default", { type: StackedBars, hAxis: "y", vAxis: "x" })
          .setTheme(Wetland)
          .addAxis("x", {
            labels: labels,
            vertical: true,
            dropLabels: false,
            labelSizeChange: true,
            minorLabels: false,
            majorTickStep: 1,
          })
          .addAxis("y", { rotation: -90, vertical: false })
          .addSeries(
            "&nbsp;Just New or changed votes in CLDR " +
              cldrStatus.getNewVersion(),
            count_new
          )
          .addSeries(
            "&nbsp;Imported winning votes from previous releases",
            count_old
          );

        var tip = new Tooltip(c, "default", {
          text: function (o) {
            return (
              '<span style="font-size: smaller;">' +
              labels[o.index].text +
              "</span><br>" +
              dojoNumber.format(count_new[o.index]) +
              " new,<br>" +
              dojoNumber.format(count_old[o.index]) +
              " imported"
            );
          },
        });

        c.render();
        var l = new SelectableLegend({ chart: c });
        l.placeAt(statDiv);
        const firstDay = new Date(data_new[0][header_new.DATE]);
        const lastDay = new Date(
          data_new[data_new.length - 1][header_new.DATE]
        );
        theDiv.appendChild(
          createChunk(
            "Above data range: " +
              firstDay.toLocaleDateString() +
              "—" +
              lastDay.toLocaleDateString(),
            "p",
            "helpContent"
          )
        );
      },
    },
    {
      name: "byloc",
      show: function (json, theDiv, params) {
        var labels = [];
        var count_old = [];
        var count_new = [];

        var header = json.stats_byloc.header;
        var header_new = json.stats_byloc_new.header;
        if (json.stats_byloc.data.length < json.stats_byloc_new.data.length) {
          throw Error(
            "stats_byloc can't have more new than all:" +
              json.stats_byloc.data.length +
              " vs " +
              json.stats_byloc_new.data.length
          );
        }

        const data_all = json.stats_byloc.data; // All locales
        const data_new = json.stats_byloc_new.data; // New data (not imported)

        // All data, by locale
        const byLocale = {};

        // Collect data
        data_all.forEach((row) => {
          const locale = row[header.LOCALE];
          byLocale[locale] = byLocale[locale] || {};
          byLocale[locale].allCount = row[header.COUNT];
        });
        data_new.forEach((row) => {
          const locale = row[header_new.LOCALE];
          byLocale[locale] = byLocale[locale] || {};
          byLocale[locale].newCount = row[header_new.COUNT];
        });
        // Analyze old count

        count_old.push(0);
        count_new.push(0);
        labels.push({ value: 0, text: "" });

        Object.keys(byLocale)
          .sort()
          .reverse()
          .forEach((l) => {
            const { allCount, newCount } = byLocale[l];
            const oldCount = (byLocale[l].oldCount =
              allCount - (newCount || 0));
            labels.push({ value: count_old.length, text: l }); // value: 1 text: 'aa', etc.
            count_old.push(oldCount || 0);
            count_new.push(newCount || 0);
          });

        // Now, render

        var statDiv = theDiv;
        statDiv.style.height = 3 + json.stats_byloc.data.length * 1 + "em";
        var c = new Chart(statDiv);
        c.addPlot("default", { type: StackedBars, hAxis: "y", vAxis: "x" })
          .setTheme(Wetland)
          .addAxis("x", {
            labels: labels,
            vertical: true,
            dropLabels: false,
            labelSizeChange: false,
            minorLabels: false,
            majorTickStep: 1,
          })
          .addAxis("y", { vertical: false, rotation: -90 })
          .addSeries(
            "&nbsp;Just New or changed votes in CLDR " +
              cldrStatus.getNewVersion(),
            count_new
          )
          .addSeries(
            "&nbsp;Imported winning votes from previous releases",
            count_old
          );
        var tip = new Tooltip(c, "default", {
          text: function (o) {
            let ret = labels[o.index].text + "   " + count_old[o.index];
            if (locmap && labels[o.index].text) {
              // use the on-client locmap to lookup the locale id
              ret += " " + locmap.getLocaleName(labels[o.index].text);
            }
            return ret;
          },
        });
        c.render();
        var l = new SelectableLegend({ chart: c });
        l.placeAt(statDiv);
      },
    },
    {
      name: "recent",
      url: false,
      show: function (json, theDiv, params) {
        showRecent(theDiv);
      },
    },
  ];

  Page.prototype.show = function show(params) {
    showInPop2(
      cldrText.get("statisticsGuidance"),
      null,
      null,
      null,
      true
    ); /* show the box the first time */
    hideLoader(null);
    isLoading = false;
    var theDiv;
    var isNew = false;
    if (params.special.theDiv) {
      theDiv = params.special.theDiv;
    } else {
      theDiv = document.createElement("div");
      theDiv.className = params.name;
      isNew = true;
      params.special.theDiv = theDiv;
    }

    if (isNew) {
      // for now - just show all sections sequentially.
      for (var k = 0; k < this.sectionArray.length; k++) {
        var theSection = this.sectionArray[k];
        var subFragment = theDiv; //document.createDocumentFragment();

        var subDiv = document.createElement("div");
        var sectionId = (subDiv.id = "stats_" + theSection.name);
        var sectionTitle = cldrText.get(sectionId);
        subFragment.appendChild(createChunk(sectionTitle, "h2"));
        subDiv.className = "statArea";
        subFragment.appendChild(subDiv);

        if (theSection.url) {
          (function (theSection, subDiv, params) {
            var loading = createChunk(
              cldrText.get("loading"),
              "p",
              "helpContent"
            );

            subDiv.appendChild(loading);
            cldrAjax.queueXhr({
              url: theSection.url,
              handleAs: "json",
              load: function (json) {
                if (json.err) {
                  updateIf(loading, "Error: " + json.err);
                  console.log(
                    "Err loading " + theSection.name + " - " + json.err
                  );
                } else {
                  updateIf(loading, "");
                  theSection.show(json, subDiv, params);
                }
              },
              error: function (err) {
                updateIf(loading, "Error: " + err);
                console.log("Err loading " + theSection.name + " - " + err);
              },
            });
          })(theSection, subDiv, params);
        } else {
          theSection.show(null, subDiv, params);
        }
      }
    }

    params.flipper.flipTo(params.pages.other, theDiv);
    // Now it's shown, can commence with dynamic load
  };

  return Page;
});
