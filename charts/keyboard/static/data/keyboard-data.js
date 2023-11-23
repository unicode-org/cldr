const _KeyboardData = 
{
 "keyboards": {
  "pt-t-k0-abnt2.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keyboard3": {
    "locales": {
     "locale": {
      "@_id": "pt"
     }
    },
    "version": {
     "@_number": "0.0.0"
    },
    "info": {
     "@_name": "Portuguese (Brazil) (ABNT2)"
    },
    "displays": {
     "display": [
      {
       "@_output": "\\m{acute}",
       "@_display": "´"
      },
      {
       "@_output": "\\m{grave}",
       "@_display": "`"
      },
      {
       "@_output": "\\m{umlaut}",
       "@_display": "¨"
      },
      {
       "@_output": "\\m{caret}",
       "@_display": "^"
      },
      {
       "@_output": "\\m{tilde}",
       "@_display": "~"
      }
     ]
    },
    "keys": {
     "import": [
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-punctuation.xml"
      },
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-currency.xml"
      }
     ],
     "key": [
      {
       "@_id": "d-acute",
       "@_output": "\\m{acute}"
      },
      {
       "@_id": "d-grave",
       "@_output": "\\m{grave}"
      },
      {
       "@_id": "d-umlaut",
       "@_output": "\\m{umlaut}"
      },
      {
       "@_id": "d-caret",
       "@_output": "\\m{caret}"
      },
      {
       "@_id": "d-tilde",
       "@_output": "\\m{tilde}"
      },
      {
       "@_id": "c-cedilla",
       "@_output": "ç"
      },
      {
       "@_id": "C-cedilla",
       "@_output": "Ç"
      },
      {
       "@_id": "super-1",
       "@_output": "¹"
      },
      {
       "@_id": "super-2",
       "@_output": "²"
      },
      {
       "@_id": "super-3",
       "@_output": "³"
      },
      {
       "@_id": "ordinal-feminine",
       "@_output": "ª"
      },
      {
       "@_id": "ordinal-masculine",
       "@_output": "º"
      }
     ]
    },
    "layers": {
     "layer": [
      {
       "row": [
        {
         "@_keys": "apos 1 2 3 4 5 6 7 8 9 0 hyphen equal"
        },
        {
         "@_keys": "q w e r t y u i o p d-acute open-square"
        },
        {
         "@_keys": "a s d f g h j k l c-cedilla d-tilde close-square"
        },
        {
         "@_keys": "backslash z x c v b n m comma period semi-colon slash"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "none"
      },
      {
       "row": [
        {
         "@_keys": "double-quote bang at hash dollar percent d-umlaut amp asterisk open-paren close-paren underscore plus"
        },
        {
         "@_keys": "Q W E R T Y U I O P d-grave open-curly"
        },
        {
         "@_keys": "A S D F G H J K L C-cedilla d-caret close-curly"
        },
        {
         "@_keys": "pipe Z X C V B N M open-angle close-angle colon question"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "shift"
      },
      {
       "row": [
        {
         "@_keys": "gap super-1 super-2 super-3 pound cent not gap gap gap gap gap section"
        },
        {
         "@_keys": "slash question degree gap gap gap gap gap gap gap gap ordinal-feminine"
        },
        {
         "@_keys": "gap gap gap gap gap gap gap gap gap gap gap ordinal-masculine"
        },
        {
         "@_keys": "gap gap gap cruzeiro gap gap gap gap gap gap gap degree"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "altR"
      }
     ],
     "@_formId": "abnt2"
    },
    "@_locale": "pt-t-k0-abnt2",
    "@_conformsTo": "techpreview"
   }
  },
  "fr-t-k0-azerty.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keyboard3": {
    "locales": {
     "locale": {
      "@_id": "br"
     }
    },
    "version": {
     "@_number": "1.0.0"
    },
    "info": {
     "@_name": "French Test AZERTY",
     "@_author": "Team Keyboard",
     "@_layout": "AZERTY",
     "@_indicator": "FR"
    },
    "displays": {
     "display": [
      {
       "@_output": "\\u{0300}",
       "@_display": "${grave}"
      },
      {
       "@_keyId": "symbol",
       "@_display": "@"
      },
      {
       "@_keyId": "numeric",
       "@_display": "123"
      }
     ],
     "displayOptions": {
      "@_baseCharacter": "x"
     }
    },
    "keys": {
     "import": [
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-punctuation.xml"
      },
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-currency.xml"
      }
     ],
     "key": [
      {
       "@_id": "shift",
       "@_layerId": "shift"
      },
      {
       "@_id": "numeric",
       "@_layerId": "numeric"
      },
      {
       "@_id": "symbol",
       "@_layerId": "symbol"
      },
      {
       "@_id": "base",
       "@_layerId": "base"
      },
      {
       "@_id": "extra",
       "@_gap": "true"
      },
      {
       "@_id": "enter",
       "@_gap": "true"
      },
      {
       "@_id": "u-grave",
       "@_output": "ü"
      },
      {
       "@_id": "e-grave",
       "@_output": "é"
      },
      {
       "@_id": "e-acute",
       "@_output": "è"
      },
      {
       "@_id": "c-cedilla",
       "@_output": "ç"
      },
      {
       "@_id": "a-grave",
       "@_output": "à"
      },
      {
       "@_id": "a-acute",
       "@_output": "á"
      },
      {
       "@_id": "a-caret",
       "@_output": "â"
      },
      {
       "@_id": "a-umlaut",
       "@_output": "ä"
      },
      {
       "@_id": "a-tilde",
       "@_output": "ã"
      },
      {
       "@_id": "a-ring",
       "@_output": "å"
      },
      {
       "@_id": "a-caron",
       "@_output": "ā"
      },
      {
       "@_id": "A-grave",
       "@_output": "À"
      },
      {
       "@_id": "A-acute",
       "@_output": "Á"
      },
      {
       "@_id": "A-caret",
       "@_output": "Â"
      },
      {
       "@_id": "A-umlaut",
       "@_output": "Ä"
      },
      {
       "@_id": "A-tilde",
       "@_output": "Ã"
      },
      {
       "@_id": "A-ring",
       "@_output": "Å"
      },
      {
       "@_id": "A-caron",
       "@_output": "Ā"
      },
      {
       "@_id": "bullet",
       "@_output": "•"
      },
      {
       "@_id": "umlaut",
       "@_output": "¨"
      },
      {
       "@_id": "super-2",
       "@_output": "²",
       "@_multiTapKeyIds": "sub-2 2"
      },
      {
       "@_id": "sub-2",
       "@_output": "₂"
      },
      {
       "@_id": "a",
       "@_flickId": "a",
       "@_output": "a",
       "@_longPressKeyIds": "a-grave a-caret a-acute a-umlaut a-tilde a-ring a-caron",
       "@_longPressDefaultKeyId": "a-caret"
      },
      {
       "@_id": "A",
       "@_flickId": "b",
       "@_output": "A",
       "@_longPressKeyIds": "A-grave A-caret A-acute A-umlaut a-tilde A-ring A-caron",
       "@_longPressDefaultKeyId": "A-caret"
      }
     ]
    },
    "flicks": {
     "flick": [
      {
       "flickSegment": [
        {
         "@_directions": "nw",
         "@_keyId": "A-grave"
        },
        {
         "@_directions": "nw se",
         "@_keyId": "A-acute"
        },
        {
         "@_directions": "e",
         "@_keyId": "A-caron"
        },
        {
         "@_directions": "s",
         "@_keyId": "numeric"
        }
       ],
       "@_id": "b"
      },
      {
       "flickSegment": [
        {
         "@_directions": "nw",
         "@_keyId": "a-grave"
        },
        {
         "@_directions": "nw se",
         "@_keyId": "a-acute"
        },
        {
         "@_directions": "e",
         "@_keyId": "a-caron"
        }
       ],
       "@_id": "a"
      }
     ]
    },
    "layers": [
     {
      "layer": [
       {
        "row": [
         {
          "@_keys": "super-2 amp e-grave double-quote apos open-paren hyphen e-acute underscore c-cedilla a-acute close-paren equal"
         },
         {
          "@_keys": "a z e r t y u i o p caret dollar"
         },
         {
          "@_keys": "q s d f g h j k l m u-grave asterisk"
         },
         {
          "@_keys": "open-angle w x c v b n comma semi-colon colon bang"
         },
         {
          "@_keys": "space"
         }
        ],
        "@_modifiers": "none"
       },
       {
        "row": [
         {
          "@_keys": "1 2 3 4 5 6 7 8 9 0 degree plus"
         },
         {
          "@_keys": "A Z E R T Y U I O P umlaut pound"
         },
         {
          "@_keys": "Q S D F G H J K L M percent micro"
         },
         {
          "@_keys": "close-angle W X C V B N question period slash section"
         },
         {
          "@_keys": "space"
         }
        ],
        "@_modifiers": "shift"
       }
      ],
      "@_formId": "iso"
     },
     {
      "layer": [
       {
        "row": [
         {
          "@_keys": "a z e r t y u i o p"
         },
         {
          "@_keys": "q s d f g h j k l m"
         },
         {
          "@_keys": "shift gap w x c v b n gap"
         },
         {
          "@_keys": "numeric extra space enter"
         }
        ],
        "@_id": "base"
       },
       {
        "row": [
         {
          "@_keys": "A Z E R T Y U I O P"
         },
         {
          "@_keys": "Q S D F G H J K L M"
         },
         {
          "@_keys": "base W X C V B N"
         },
         {
          "@_keys": "numeric extra space enter"
         }
        ],
        "@_id": "shift"
       },
       {
        "row": [
         {
          "@_keys": "1 2 3 4 5 6 7 8 9 0"
         },
         {
          "@_keys": "hyphen slash colon semi-colon open-paren close-paren dollar amp at double-quote"
         },
         {
          "@_keys": "symbol period comma question bang double-quote"
         },
         {
          "@_keys": "base extra space enter"
         }
        ],
        "@_id": "numeric"
       },
       {
        "row": [
         {
          "@_keys": "open-square close-square open-curly close-curly hash percent caret asterisk plus equal"
         },
         {
          "@_keys": "underscore backslash pipe tilde open-angle close-angle euro pound yen bullet"
         },
         {
          "@_keys": "numeric period comma question bang double-quote"
         },
         {
          "@_keys": "base extra space enter"
         }
        ],
        "@_id": "symbol"
       }
      ],
      "@_formId": "touch",
      "@_minDeviceWidth": "150"
     }
    ],
    "variables": {
     "string": [
      {
       "@_id": "grave",
       "@_value": "`"
      },
      {
       "@_id": "caret",
       "@_value": "^"
      },
      {
       "@_id": "umlaut",
       "@_value": "¨"
      },
      {
       "@_id": "tilde",
       "@_value": "~"
      }
     ],
     "set": [
      {
       "@_id": "vowel",
       "@_value": "a e i o u  A E I O U"
      },
      {
       "@_id": "graveVowel",
       "@_value": "à è ì ò ù  À È Ì Ò Ù"
      },
      {
       "@_id": "caretVowel",
       "@_value": "â ê î ô û  Â Ê Î Ô Û"
      },
      {
       "@_id": "umlautVowel",
       "@_value": "ä ë ï ö ü  Ä Ë Ï Ö Ü"
      },
      {
       "@_id": "spacing_accent",
       "@_value": "${grave} ${caret} ${umlaut} ${tilde}"
      }
     ]
    },
    "transforms": [
     {
      "transformGroup": [
       {
        "transform": [
         {
          "@_from": "${grave}($[vowel])",
          "@_to": "$[1:graveVowel]"
         },
         {
          "@_from": "${caret}($[vowel])",
          "@_to": "$[1:caretVowel]"
         },
         {
          "@_from": "${umlaut}($[vowel])",
          "@_to": "$[1:umlautVowel]"
         },
         {
          "@_from": "${umlaut}y",
          "@_to": "ÿ"
         },
         {
          "@_from": "${tilde}a",
          "@_to": "ã"
         },
         {
          "@_from": "${tilde}A",
          "@_to": "Ã"
         },
         {
          "@_from": "${tilde}n",
          "@_to": "ñ"
         },
         {
          "@_from": "${tilde}N",
          "@_to": "Ñ"
         },
         {
          "@_from": "${tilde}o",
          "@_to": "õ"
         },
         {
          "@_from": "${tilde}O",
          "@_to": "Õ"
         },
         {
          "@_from": "($[spacing_accent])",
          "@_to": "$1"
         }
        ]
       },
       {
        "reorder": [
         {
          "@_from": "\\u{1A60}",
          "@_order": "127"
         },
         {
          "@_from": "\\u{1A6B}",
          "@_order": "42"
         },
         {
          "@_from": "[\\u{1A75}-\\u{1A79}]",
          "@_order": "55"
         }
        ]
       }
      ],
      "@_type": "simple"
     }
    ],
    "@_locale": "fr-t-k0-azerty",
    "@_conformsTo": "techpreview"
   }
  },
  "ja-Latn.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keyboard3": {
    "locales": {
     "locale": {
      "@_id": "en"
     }
    },
    "version": {
     "@_number": "0.0.0"
    },
    "info": {
     "@_name": "Romaji (JIS)"
    },
    "keys": {
     "import": [
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-punctuation.xml"
      },
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-currency.xml"
      }
     ]
    },
    "layers": {
     "layer": [
      {
       "row": [
        {
         "@_keys": "1 2 3 4 5 6 7 8 9 0 hyphen caret yen"
        },
        {
         "@_keys": "q w e r t y u i o p at open-square"
        },
        {
         "@_keys": "a s d f g h j k l semi-colon colon close-square"
        },
        {
         "@_keys": "z x c v b n m comma period slash underscore"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "none"
      },
      {
       "row": [
        {
         "@_keys": "bang double-quote hash dollar percent amp apos open-paren close-paren 0 equal tilde pipe"
        },
        {
         "@_keys": "Q W E R T Y U I O P grave open-curly"
        },
        {
         "@_keys": "A S D F G H J K L plus asterisk close-curly"
        },
        {
         "@_keys": "Z X C V B N M open-angle close-angle question underscore"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "shift"
      }
     ],
     "@_formId": "jis"
    },
    "@_locale": "ja-Latn",
    "@_conformsTo": "techpreview"
   }
  },
  "mt.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keyboard3": {
    "locales": {
     "locale": {
      "@_id": "en"
     }
    },
    "info": {
     "@_name": "Maltese",
     "@_author": "Steven R. Loomis",
     "@_layout": "QWERTY",
     "@_indicator": "MT"
    },
    "keys": {
     "import": [
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-punctuation.xml"
      },
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-currency.xml"
      }
     ],
     "key": [
      {
       "@_id": "a-grave",
       "@_output": "à"
      },
      {
       "@_id": "A-grave",
       "@_output": "À"
      },
      {
       "@_id": "e-grave",
       "@_output": "è"
      },
      {
       "@_id": "E-grave",
       "@_output": "È"
      },
      {
       "@_id": "i-grave",
       "@_output": "ì"
      },
      {
       "@_id": "I-grave",
       "@_output": "Ì"
      },
      {
       "@_id": "o-grave",
       "@_output": "ò"
      },
      {
       "@_id": "O-grave",
       "@_output": "Ò"
      },
      {
       "@_id": "u-grave",
       "@_output": "ù"
      },
      {
       "@_id": "U-grave",
       "@_output": "Ù"
      },
      {
       "@_id": "c-tikka",
       "@_output": "ċ"
      },
      {
       "@_id": "C-tikka",
       "@_output": "Ċ"
      },
      {
       "@_id": "g-tikka",
       "@_output": "ġ"
      },
      {
       "@_id": "G-tikka",
       "@_output": "Ġ"
      },
      {
       "@_id": "h-maqtugha",
       "@_output": "ħ"
      },
      {
       "@_id": "H-maqtugha",
       "@_output": "Ħ"
      },
      {
       "@_id": "z-tikka",
       "@_output": "ż"
      },
      {
       "@_id": "Z-tikka",
       "@_output": "Ż"
      },
      {
       "@_id": "c-cedilla",
       "@_output": "ç"
      }
     ]
    },
    "layers": {
     "layer": [
      {
       "row": [
        {
         "@_keys": "c-tikka 1 2 3 4 5 6 7 8 9 0 hyphen equal"
        },
        {
         "@_keys": "q w e r t y u i o p g-tikka h-maqtugha"
        },
        {
         "@_keys": "a s d f g h j k l semi-colon hash"
        },
        {
         "@_keys": "z-tikka z x c v b n m comma period slash"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "none"
      },
      {
       "row": [
        {
         "@_keys": "C-tikka bang double-quote euro dollar percent caret amp open-paren close-paren underscore plus"
        },
        {
         "@_keys": "Q W E R T Y U I O P G-tikka H-maqtugha"
        },
        {
         "@_keys": "A S D F G H J K L colon at tilde"
        },
        {
         "@_keys": "Z-tikka Z X C V B N M open-angle close-angle question"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "shift"
      },
      {
       "row": [
        {
         "@_keys": "grave gap gap pound gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "gap gap e-grave gap gap gap u-grave i-grave o-grave gap open-square close-square"
        },
        {
         "@_keys": "a-grave gap gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "backslash gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "altR"
      },
      {
       "row": [
        {
         "@_keys": "not gap gap gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "gap gap E-grave gap gap gap U-grave I-grave O-grave gap open-curly close-curly"
        },
        {
         "@_keys": "A-grave gap gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "pipe gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "altR shift"
      }
     ],
     "@_formId": "iso"
    },
    "@_locale": "mt",
    "@_conformsTo": "techpreview"
   }
  },
  "mt-t-k0-47key.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keyboard3": {
    "locales": {
     "locale": {
      "@_id": "en"
     }
    },
    "info": {
     "@_name": "Maltese 47-key",
     "@_author": "Steven R. Loomis",
     "@_layout": "QWERTY",
     "@_indicator": "MT"
    },
    "keys": {
     "import": [
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-punctuation.xml"
      },
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-currency.xml"
      }
     ],
     "key": [
      {
       "@_id": "a-grave",
       "@_output": "à"
      },
      {
       "@_id": "A-grave",
       "@_output": "À"
      },
      {
       "@_id": "e-grave",
       "@_output": "è"
      },
      {
       "@_id": "E-grave",
       "@_output": "È"
      },
      {
       "@_id": "i-grave",
       "@_output": "ì"
      },
      {
       "@_id": "I-grave",
       "@_output": "Ì"
      },
      {
       "@_id": "o-grave",
       "@_output": "ò"
      },
      {
       "@_id": "O-grave",
       "@_output": "Ò"
      },
      {
       "@_id": "u-grave",
       "@_output": "ù"
      },
      {
       "@_id": "U-grave",
       "@_output": "Ù"
      },
      {
       "@_id": "c-tikka",
       "@_output": "ċ"
      },
      {
       "@_id": "C-tikka",
       "@_output": "Ċ"
      },
      {
       "@_id": "g-tikka",
       "@_output": "ġ"
      },
      {
       "@_id": "G-tikka",
       "@_output": "Ġ"
      },
      {
       "@_id": "h-maqtugha",
       "@_output": "ħ"
      },
      {
       "@_id": "H-maqtugha",
       "@_output": "Ħ"
      },
      {
       "@_id": "z-tikka",
       "@_output": "ż"
      },
      {
       "@_id": "Z-tikka",
       "@_output": "Ż"
      },
      {
       "@_id": "c-cedilla",
       "@_output": "ç"
      },
      {
       "@_id": "gap",
       "@_gap": "true",
       "@_width": "1"
      }
     ]
    },
    "layers": {
     "layer": [
      {
       "row": [
        {
         "@_keys": "c-tikka 1 2 3 4 5 6 7 8 9 0 hyphen equal"
        },
        {
         "@_keys": "q w e r t y u i o p g-tikka h-maqtugha z-tikka"
        },
        {
         "@_keys": "a s d f g h j k l semi-colon apos"
        },
        {
         "@_keys": "z x c v b n m comma period slash"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "none"
      },
      {
       "row": [
        {
         "@_keys": "C-tikka bang at euro dollar percent caret amp asterisk open-paren close-paren underscore plus"
        },
        {
         "@_keys": "Q W E R T Y U I O P G-tikka H-maqtugha Z-tikka"
        },
        {
         "@_keys": "A S D F G H J K L colon double-quote"
        },
        {
         "@_keys": "Z X C V B N M open-angle close-angle question"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "shift"
      },
      {
       "row": [
        {
         "@_keys": "grave gap gap pound gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "gap gap e-grave gap gap gap u-grave i-grave o-grave gap open-square close-square backslash"
        },
        {
         "@_keys": "a-grave gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "altR"
      },
      {
       "row": [
        {
         "@_keys": "tilde gap gap gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "gap gap E-grave gap gap gap U-grave I-grave O-grave gap open-curly close-curly pipe"
        },
        {
         "@_keys": "A-grave gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "gap gap gap gap gap gap gap gap gap gap"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "altR shift"
      }
     ],
     "@_formId": "us"
    },
    "@_locale": "mt-t-k0-47key",
    "@_conformsTo": "techpreview"
   }
  },
  "pcm.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keyboard3": {
    "version": {
     "@_number": "1.0.0"
    },
    "info": {
     "@_name": "Naijíriá Píjin"
    },
    "keys": {
     "import": [
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-punctuation.xml"
      },
      {
       "@_base": "cldr",
       "@_path": "techpreview/keys-Zyyy-currency.xml"
      }
     ],
     "key": [
      {
       "@_id": "grave",
       "@_output": "\\u{300}"
      },
      {
       "@_id": "backquote",
       "@_output": "`"
      },
      {
       "@_id": "acute",
       "@_output": "\\u{301}"
      },
      {
       "@_id": "odot",
       "@_output": "ọ"
      },
      {
       "@_id": "Odot",
       "@_output": "Ọ"
      },
      {
       "@_id": "edot",
       "@_output": "ẹ"
      },
      {
       "@_id": "Edot",
       "@_output": "Ẹ"
      },
      {
       "@_id": "naira",
       "@_output": "₦"
      }
     ]
    },
    "layers": {
     "layer": [
      {
       "row": [
        {
         "@_keys": "grave 1 2 3 4 5 6 7 8 9 0 hyphen equal"
        },
        {
         "@_keys": "acute w e r t y u i o p open-square close-square"
        },
        {
         "@_keys": "a s d f g h j k l odot edot slash"
        },
        {
         "@_keys": "slash z c v b n m comma period semi-colon apos"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "none"
      },
      {
       "row": [
        {
         "@_keys": "grave bang at hash dollar naira percent amp asterisk open-paren close-paren underscore plus"
        },
        {
         "@_keys": "A S D F G H J K L Odot Edot question"
        },
        {
         "@_keys": "A S D F G H J K L Odot Edot"
        },
        {
         "@_keys": "question Z C V B N M open-angle close-angle colon double-quote"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "shift"
      },
      {
       "row": [
        {
         "@_keys": "backquote 1 2 3 4 5 6 7 8 9 0 hyphen equal"
        },
        {
         "@_keys": "Q W E R T Y U I O P open-square close-square"
        },
        {
         "@_keys": "A S D F G H J K L Odot Edot slash"
        },
        {
         "@_keys": "slash Z C V B N M comma period semi-colon apos"
        },
        {
         "@_keys": "space"
        }
       ],
       "@_modifiers": "caps"
      }
     ],
     "@_formId": "iso"
    },
    "transforms": [
     {
      "transformGroup": [
       {
        "transform": [
         {
          "@_from": "''",
          "@_to": "\\u{323}"
         }
        ]
       }
      ],
      "@_type": "simple"
     }
    ],
    "@_locale": "pcm",
    "@_conformsTo": "techpreview"
   }
  }
 },
 "imports": {
  "keys-Latn-implied.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keys": {
    "key": [
     {
      "@_id": "gap",
      "@_gap": "true",
      "@_width": "1"
     },
     {
      "@_id": "space",
      "@_output": "\\u{0020}",
      "@_stretch": "true",
      "@_width": "1"
     },
     {
      "@_id": "0",
      "@_output": "0"
     },
     {
      "@_id": "1",
      "@_output": "1"
     },
     {
      "@_id": "2",
      "@_output": "2"
     },
     {
      "@_id": "3",
      "@_output": "3"
     },
     {
      "@_id": "4",
      "@_output": "4"
     },
     {
      "@_id": "5",
      "@_output": "5"
     },
     {
      "@_id": "6",
      "@_output": "6"
     },
     {
      "@_id": "7",
      "@_output": "7"
     },
     {
      "@_id": "8",
      "@_output": "8"
     },
     {
      "@_id": "9",
      "@_output": "9"
     },
     {
      "@_id": "A",
      "@_output": "A"
     },
     {
      "@_id": "B",
      "@_output": "B"
     },
     {
      "@_id": "C",
      "@_output": "C"
     },
     {
      "@_id": "D",
      "@_output": "D"
     },
     {
      "@_id": "E",
      "@_output": "E"
     },
     {
      "@_id": "F",
      "@_output": "F"
     },
     {
      "@_id": "G",
      "@_output": "G"
     },
     {
      "@_id": "H",
      "@_output": "H"
     },
     {
      "@_id": "I",
      "@_output": "I"
     },
     {
      "@_id": "J",
      "@_output": "J"
     },
     {
      "@_id": "K",
      "@_output": "K"
     },
     {
      "@_id": "L",
      "@_output": "L"
     },
     {
      "@_id": "M",
      "@_output": "M"
     },
     {
      "@_id": "N",
      "@_output": "N"
     },
     {
      "@_id": "O",
      "@_output": "O"
     },
     {
      "@_id": "P",
      "@_output": "P"
     },
     {
      "@_id": "Q",
      "@_output": "Q"
     },
     {
      "@_id": "R",
      "@_output": "R"
     },
     {
      "@_id": "S",
      "@_output": "S"
     },
     {
      "@_id": "T",
      "@_output": "T"
     },
     {
      "@_id": "U",
      "@_output": "U"
     },
     {
      "@_id": "V",
      "@_output": "V"
     },
     {
      "@_id": "W",
      "@_output": "W"
     },
     {
      "@_id": "X",
      "@_output": "X"
     },
     {
      "@_id": "Y",
      "@_output": "Y"
     },
     {
      "@_id": "Z",
      "@_output": "Z"
     },
     {
      "@_id": "a",
      "@_output": "a"
     },
     {
      "@_id": "b",
      "@_output": "b"
     },
     {
      "@_id": "c",
      "@_output": "c"
     },
     {
      "@_id": "d",
      "@_output": "d"
     },
     {
      "@_id": "e",
      "@_output": "e"
     },
     {
      "@_id": "f",
      "@_output": "f"
     },
     {
      "@_id": "g",
      "@_output": "g"
     },
     {
      "@_id": "h",
      "@_output": "h"
     },
     {
      "@_id": "i",
      "@_output": "i"
     },
     {
      "@_id": "j",
      "@_output": "j"
     },
     {
      "@_id": "k",
      "@_output": "k"
     },
     {
      "@_id": "l",
      "@_output": "l"
     },
     {
      "@_id": "m",
      "@_output": "m"
     },
     {
      "@_id": "n",
      "@_output": "n"
     },
     {
      "@_id": "o",
      "@_output": "o"
     },
     {
      "@_id": "p",
      "@_output": "p"
     },
     {
      "@_id": "q",
      "@_output": "q"
     },
     {
      "@_id": "r",
      "@_output": "r"
     },
     {
      "@_id": "s",
      "@_output": "s"
     },
     {
      "@_id": "t",
      "@_output": "t"
     },
     {
      "@_id": "u",
      "@_output": "u"
     },
     {
      "@_id": "v",
      "@_output": "v"
     },
     {
      "@_id": "w",
      "@_output": "w"
     },
     {
      "@_id": "x",
      "@_output": "x"
     },
     {
      "@_id": "y",
      "@_output": "y"
     },
     {
      "@_id": "z",
      "@_output": "z"
     }
    ]
   }
  },
  "keys-Zyyy-punctuation.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keys": {
    "key": [
     {
      "@_id": "amp",
      "@_output": "\\u{0026}"
     },
     {
      "@_id": "apos",
      "@_output": "'"
     },
     {
      "@_id": "asterisk",
      "@_output": "*"
     },
     {
      "@_id": "at",
      "@_output": "@"
     },
     {
      "@_id": "backslash",
      "@_output": "\\u{005C}"
     },
     {
      "@_id": "bang",
      "@_output": "!"
     },
     {
      "@_id": "caret",
      "@_output": "^"
     },
     {
      "@_id": "close-angle",
      "@_output": ">"
     },
     {
      "@_id": "close-curly",
      "@_output": "}"
     },
     {
      "@_id": "close-paren",
      "@_output": ")"
     },
     {
      "@_id": "close-square",
      "@_output": "]"
     },
     {
      "@_id": "colon",
      "@_output": ":"
     },
     {
      "@_id": "comma",
      "@_output": ","
     },
     {
      "@_id": "degree",
      "@_output": "°"
     },
     {
      "@_id": "double-quote",
      "@_output": "\\u{0022}"
     },
     {
      "@_id": "equal",
      "@_output": "="
     },
     {
      "@_id": "grave",
      "@_output": "`"
     },
     {
      "@_id": "hash",
      "@_output": "#"
     },
     {
      "@_id": "hyphen",
      "@_output": "-"
     },
     {
      "@_id": "micro",
      "@_output": "µ"
     },
     {
      "@_id": "not",
      "@_output": "¬"
     },
     {
      "@_id": "open-angle",
      "@_output": "\\u{003C}"
     },
     {
      "@_id": "open-curly",
      "@_output": "{"
     },
     {
      "@_id": "open-paren",
      "@_output": "("
     },
     {
      "@_id": "open-square",
      "@_output": "["
     },
     {
      "@_id": "percent",
      "@_output": "%"
     },
     {
      "@_id": "period",
      "@_output": "."
     },
     {
      "@_id": "pipe",
      "@_output": "|"
     },
     {
      "@_id": "plus",
      "@_output": "+"
     },
     {
      "@_id": "question",
      "@_output": "?"
     },
     {
      "@_id": "section",
      "@_output": "§"
     },
     {
      "@_id": "semi-colon",
      "@_output": ";"
     },
     {
      "@_id": "slash",
      "@_output": "/"
     },
     {
      "@_id": "tilde",
      "@_output": "~"
     },
     {
      "@_id": "underscore",
      "@_output": "_"
     }
    ]
   }
  },
  "scanCodes-implied.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "forms": {
    "form": [
     {
      "scanCodes": [
       {
        "@_codes": "29 02 03 04 05 06 07 08 09 0A 0B 0C 0D"
       },
       {
        "@_codes": "10 11 12 13 14 15 16 17 18 19 1A 1B 2B"
       },
       {
        "@_codes": "1E 1F 20 21 22 23 24 25 26 27 28"
       },
       {
        "@_codes": "2C 2D 2E 2F 30 31 32 33 34 35"
       },
       {
        "@_codes": "39"
       }
      ],
      "@_id": "us"
     },
     {
      "scanCodes": [
       {
        "@_codes": "29 02 03 04 05 06 07 08 09 0A 0B 0C 0D"
       },
       {
        "@_codes": "10 11 12 13 14 15 16 17 18 19 1A 1B"
       },
       {
        "@_codes": "1E 1F 20 21 22 23 24 25 26 27 28 2B"
       },
       {
        "@_codes": "56 2C 2D 2E 2F 30 31 32 33 34 35"
       },
       {
        "@_codes": "39"
       }
      ],
      "@_id": "iso"
     },
     {
      "scanCodes": [
       {
        "@_codes": "29 02 03 04 05 06 07 08 09 0A 0B 0C 0D"
       },
       {
        "@_codes": "10 11 12 13 14 15 16 17 18 19 1A 1B"
       },
       {
        "@_codes": "1E 1F 20 21 22 23 24 25 26 27 28 2B"
       },
       {
        "@_codes": "56 2C 2D 2E 2F 30 31 32 33 34 35 73"
       },
       {
        "@_codes": "39"
       }
      ],
      "@_id": "abnt2"
     },
     {
      "scanCodes": [
       {
        "@_codes": "29 02 03 04 05 06 07 08 09 0A 0B 0C 0D 7D"
       },
       {
        "@_codes": "10 11 12 13 14 15 16 17 18 19 1A 1B"
       },
       {
        "@_codes": "1E 1F 20 21 22 23 24 25 26 27 28 2B"
       },
       {
        "@_codes": "2C 2D 2E 2F 30 31 32 33 34 35 73"
       },
       {
        "@_codes": "39"
       }
      ],
      "@_id": "jis"
     },
     {
      "scanCodes": [
       {
        "@_codes": "29 02 03 04 05 06 07 08 09 0A 0B 0C 0D 2B"
       },
       {
        "@_codes": "10 11 12 13 14 15 16 17 18 19 1A 1B"
       },
       {
        "@_codes": "1E 1F 20 21 22 23 24 25 26 27 28"
       },
       {
        "@_codes": "2C 2D 2E 2F 30 31 32 33 34 35"
       },
       {
        "@_codes": "39"
       }
      ],
      "@_id": "ks"
     }
    ]
   }
  },
  "keys-Zyyy-currency.xml": {
   "?xml": {
    "@_version": "1.0",
    "@_encoding": "UTF-8"
   },
   "keys": {
    "key": [
     {
      "@_id": "dollar",
      "@_output": "$"
     },
     {
      "@_id": "euro",
      "@_output": "€"
     },
     {
      "@_id": "pound",
      "@_output": "£"
     },
     {
      "@_id": "yen",
      "@_output": "¥"
     },
     {
      "@_id": "cruzeiro",
      "@_output": "₢"
     },
     {
      "@_id": "cent",
      "@_output": "¢"
     }
    ]
   }
  }
 }
}