// This is based on an actual response returned for a dashboard request.
let dashJson =
{
  "hidden": {
    "asciiCharactersNotInMainOrAuxiliaryExemplars": [
      {
        "value": "M01",
        "xpstrid": "1d88b195fc94db8e"
      }
    ]
  },
  "notifications": [
    {
      "category": "Error",
      "groups": [
        {
          "entries": [
            {
              "code": "long-one-nominative",
              "comment": "&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={NUMBER_OF_UNITS}, e.g. “3”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.",
              "english": "{0} metric pint",
              "old": "{0} mpt",
              "subtype": "missingPlaceholders",
              "winning": "////",
              "xpstrid": "710b6e70773e5764"
            },
            {
              "code": "long-one-accusative",
              "comment": "&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={NUMBER_OF_UNITS}, e.g. “3”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.",
              "english": "{0} metric pint",
              "old": "{0} ሜትሪክ ፒንት",
              "subtype": "missingPlaceholders",
              "winning": "////",
              "xpstrid": "64a8a83fbacdf836"
            },
            {
              "code": "long-other-nominative",
              "comment": "&lt;display collision&gt; Can't have same translation as [<a href=\"/cldr-apps/v#/am/MassWeight/275e733607b4babe\">carat: long-one-nominative</a>]. Please change either this name or the other one. See <a target='doc' href='http://cldr.unicode.org/translation/short-names-and-keywords#TOC-Unique-Names'>Unique-Names</a>.<br>&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={NUMBER_OF_UNITS}, e.g. “3”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.",
              "english": "{0} metric pints",
              "old": "{0} mpt",
              "subtype": "displayCollision",
              "winning": "???",
              "xpstrid": "51ad46ead277a5b6"
            },
            {
              "code": "short-one-nominative",
              "comment": "&lt;display collision&gt; Can't have same translation as [<a href=\"/cldr-apps/v#/am/MassWeight/275e733607b4babe\">carat: long-one-nominative</a>]. Please change either this name or the other one. See <a target='doc' href='http://cldr.unicode.org/translation/short-names-and-keywords#TOC-Unique-Names'>Unique-Names</a>.<br>&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={NUMBER_OF_UNITS}, e.g. “3”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.",
              "english": "{0} mpt",
              "old": "{0} mpt",
              "subtype": "displayCollision",
              "winning": "...",
              "xpstrid": "79b2c17ebd9644f3"
            },
            {
              "code": "short-other-nominative",
              "comment": "&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={NUMBER_OF_UNITS}, e.g. “3”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.",
              "english": "{0} mpt",
              "old": "{0} mpt",
              "subtype": "missingPlaceholders",
              "winning": "sadfasd",
              "xpstrid": "5afc920b5f0bbc53"
            },
            {
              "code": "narrow-other-nominative",
              "comment": "&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={NUMBER_OF_UNITS}, e.g. “3”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.",
              "english": "{0}mpt",
              "old": "{0} mpt",
              "subtype": "missingPlaceholders",
              "winning": "sadfasd",
              "xpstrid": "6c4b9cb74797908c"
            }
          ],
          "header": "pint-metric",
          "page": "Volume",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "long-one-nominative",
              "comment": "&lt;display collision&gt; Can't have same translation as [<a href=\"/cldr-apps/v#/am/Volume/51ad46ead277a5b6\">pint-metric: long-other-nominative</a>, <a href=\"/cldr-apps/v#/am/Volume/79b2c17ebd9644f3\">pint-metric: short-one-nominative</a>]. Please change either this name or the other one. See <a target='doc' href='http://cldr.unicode.org/translation/short-names-and-keywords#TOC-Unique-Names'>Unique-Names</a>.<br>&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={NUMBER_OF_UNITS}, e.g. “3”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.",
              "english": "{0} carat",
              "old": "{0} ካራት",
              "subtype": "displayCollision",
              "winning": "???",
              "xpstrid": "275e733607b4babe"
            }
          ],
          "header": "carat",
          "page": "MassWeight",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "narrow",
              "comment": "&lt;missing placeholders&gt; Need at least 2 placeholder(s), but only have 0. Placeholders are: {{0}={DIVIDEND}, e.g. “UNIT meters”, {1}={DIVISOR}, e.g. “UNIT second”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.<br>&lt;invalid place holder&gt; Invalid unit pattern, must have min 2 and max 2 distinct placeholders of the form {n}",
              "english": "{0}/{1}",
              "old": "{0}/{1}",
              "subtype": "missingPlaceholders",
              "winning": "3333333",
              "xpstrid": "6c46c41bb59b604"
            }
          ],
          "header": "per",
          "page": "CompoundUnits",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "short-one-nominative-dgender",
              "comment": "&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={POWER}, e.g. “UNIT meters”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.<br>&lt;invalid place holder&gt; Invalid unit pattern, must have min 1 and max 1 distinct placeholders of the form {n}",
              "english": "{0}²",
              "old": "{0}²",
              "subtype": "missingPlaceholders",
              "winning": "....",
              "xpstrid": "6da42acaa30ab41b"
            },
            {
              "code": "narrow-one-nominative-dgender",
              "comment": "&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={POWER}, e.g. “UNIT meters”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.<br>&lt;invalid place holder&gt; Invalid unit pattern, must have min 1 and max 1 distinct placeholders of the form {n}",
              "english": "{0}²",
              "old": "{0}²",
              "subtype": "missingPlaceholders",
              "winning": "....",
              "xpstrid": "791d0154f7c8b0c2"
            }
          ],
          "header": "power2",
          "page": "CompoundUnits",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "narrow-other-nominative-dgender",
              "comment": "&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders are: {{0}={POWER}, e.g. “UNIT meters”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.<br>&lt;invalid place holder&gt; Invalid unit pattern, must have min 1 and max 1 distinct placeholders of the form {n}",
              "english": "{0}³",
              "old": "{0}³",
              "subtype": "missingPlaceholders",
              "winning": "?@@",
              "xpstrid": "15b36616163c9af6"
            }
          ],
          "header": "power3",
          "page": "CompoundUnits",
          "section": "Units"
        }
      ]
    },
    {
      "category": "Disputed",
      "groups": [
        {
          "entries": [
            {
              "code": "long",
              "english": "{0} east",
              "old": "{0}ምስ",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "{0} east",
              "xpstrid": "24bffacda2d45847"
            }
          ],
          "header": "east",
          "page": "Coordinates",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "long",
              "english": "{0} per {1}",
              "old": "{0} በ{1}",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "{0}/{1}",
              "xpstrid": "7c92c0407c07261a"
            },
            {
              "code": "narrow",
              "comment": "&lt;missing placeholders&gt; Need at least 2 placeholder(s), but only have 0. Placeholders are: {{0}={DIVIDEND}, e.g. “UNIT meters”, {1}={DIVISOR}, e.g. “UNIT second”}; see <a href='http://cldr.unicode.org/translation/error-codes#missingPlaceholders'  target='cldr_error_codes'>missing placeholders</a>.<br>&lt;invalid place holder&gt; Invalid unit pattern, must have min 2 and max 2 distinct placeholders of the form {n}",
              "english": "{0}/{1}",
              "old": "{0}/{1}",
              "subtype": "missingPlaceholders",
              "winning": "3333333",
              "xpstrid": "6c46c41bb59b604"
            }
          ],
          "header": "per",
          "page": "CompoundUnits",
          "section": "Units"
        }
      ]
    },
    {
      "category": "English_Changed",
      "groups": [
        {
          "entries": [
            {
              "code": "long-displayName",
              "english": "cardinal direction",
              "old": "ዓቢይ አቅጣጫ",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "ዓቢይ አቅጣጫ",
              "xpstrid": "f8a86481ab6f7c4"
            }
          ],
          "header": "all",
          "page": "Coordinates",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "long",
              "english": "{0} east",
              "old": "{0}ምስ",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "{0} east",
              "xpstrid": "24bffacda2d45847"
            }
          ],
          "header": "east",
          "page": "Coordinates",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "long",
              "english": "{0} north",
              "old": "{0}ሰ",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "{0}ሰ",
              "xpstrid": "70cd56e66f2e1571"
            }
          ],
          "header": "north",
          "page": "Coordinates",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "long",
              "english": "{0} south",
              "old": "{0}ደ",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "{0}ደ",
              "xpstrid": "5145b6aa12555070"
            }
          ],
          "header": "south",
          "page": "Coordinates",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "long",
              "english": "{0} west",
              "old": "{0}ምዕ",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "{0}ምዕ",
              "xpstrid": "14b9827c199aa34a"
            }
          ],
          "header": "west",
          "page": "Coordinates",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "long",
              "english": "{0} per {1}",
              "old": "{0} በ{1}",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "{0}/{1}",
              "xpstrid": "7c92c0407c07261a"
            }
          ],
          "header": "per",
          "page": "CompoundUnits",
          "section": "Units"
        },
        {
          "entries": [
            {
              "code": "long",
              "english": "{0}-{1}",
              "old": "{0}⋅{1}",
              "previousEnglish": "�",
              "subtype": "none",
              "winning": "{0}⋅{1}",
              "xpstrid": "6761036bf70f7124"
            }
          ],
          "header": "times",
          "page": "CompoundUnits",
          "section": "Units"
        }
      ]
    }
  ],
  "voterProgress": {
    "votablePathCount": 0,
    "votedPathCount": 0
  }
}

// This is based on an actual response returned for a WHAT_GETROW request with dashboard true.
// Some irrelevant parts of the data were removed.
// The data is specific to a single path, with three notification categories.
let dashUpdateJson1 =
{
  "section": {
    "rows": {
      "_xctnmb": {
        "xpstrid": "710b6e70773e5764",
      },
    }
  },
  "notifications": [
    {
      "groups": [
        {
          "entries": [
            {
              "code": "long-one-nominative",
              "previousEnglish": "�",
              "winning": "{0} ሜፒ",
              "subtype": "largerDifferences",
              "old": "{0} mpt",
              "english": "{0} metric pint",
              "comment": "&lt;larger differences&gt; 16 different characters within {???, {0} ሜትሪክ ፒንቶች, {0} ሜፒ⨱2}; COUNT_CASE",
              "xpstrid": "710b6e70773e5764"
            }
          ],
          "header": "pint-metric",
          "section": "Units",
          "page": "Volume"
        }
      ],
      "category": "Disputed"
    },
    {
      "groups": [
        {
          "entries": [
            {
              "code": "long-one-nominative",
              "previousEnglish": "�",
              "winning": "{0} ሜፒ",
              "subtype": "largerDifferences",
              "old": "{0} mpt",
              "english": "{0} metric pint",
              "comment": "&lt;larger differences&gt; 16 different characters within {???, {0} ሜትሪክ ፒንቶች, {0} ሜፒ⨱2}; COUNT_CASE",
              "xpstrid": "710b6e70773e5764"
            }
          ],
          "header": "pint-metric",
          "section": "Units",
          "page": "Volume"
        }
      ],
      "category": "Warning"
    },
    {
      "groups": [
        {
          "entries": [
            {
              "code": "long-one-nominative",
              "previousEnglish": "�",
              "winning": "{0} ሜፒ",
              "subtype": "largerDifferences",
              "old": "{0} mpt",
              "english": "{0} metric pint",
              "comment": "&lt;larger differences&gt; 16 different characters within {???, {0} ሜትሪክ ፒንቶች, {0} ሜፒ⨱2}; COUNT_CASE",
              "xpstrid": "710b6e70773e5764"
            }
          ],
          "header": "pint-metric",
          "section": "Units",
          "page": "Volume"
        }
      ],
      "category": "English_Changed"
    }
  ]
}

// This getrow data is specific to a single path, with zero notification categories.
// Since notifications is empty, we need to get xpstrid from section.rows...xpstrid
let dashUpdateJson2 =
{
  "section": {
    "rows": {
      "_xctnmb": {
        "xpstrid": "710b6e70773e5764",
      },
    }
  },
  "notifications": []
}
