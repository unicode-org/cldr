function changeStyle(hideRegex) {
    for (m in document.styleSheets) {
        var theRules;
        if (document.styleSheets[m].cssRules) {
            theRules = document.styleSheets[m].cssRules;
        } else if (document.styleSheets[m].rules) {
            theRules = document.styleSheets[m].rules;
        }
        for (n in theRules) {
            var rule = theRules[n];
            var sel = rule.selectorText;
            if (sel != undefined && sel.match(/vv/))   {
                var theStyle = rule.style;
                if (sel.match(hideRegex)) {
                    if (theStyle.display == 'table-row') {
                        theStyle.display = null;
                    }
                } else {
                    if (theStyle.display != 'table-row') {
                        theStyle.display = 'table-row';
                    }
                }
            }
        }
    }
}

function setStyles() {
    var hideRegexString = "X1234X";
    for (i=0; i < document.checkboxes.elements.length; i++){
        var item = document.checkboxes.elements[i];
        if (!item.checked) {
            hideRegexString += "|";
            hideRegexString += item.name;
        }
    }
    var hideRegex = new RegExp(hideRegexString);
    changeStyle(hideRegex);
}
