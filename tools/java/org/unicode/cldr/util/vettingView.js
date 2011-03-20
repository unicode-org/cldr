function changeStyle(show) {
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
                if (sel.match(show)) {
                    rule.style.display = 'table-row';
                } else {
                    rule.style.display = 'none';
                }
            }
        }
    }
}

function setStyles() {
    var regexString = "";
    for (i=0; i < document.checkboxes.elements.length; i++){
        var item = document.checkboxes.elements[i];
        if (item.checked) {
            if (regexString.length != 0) {
                regexString += "|";
            }
            regexString += item.name;
        }
    }
    var myregexp = new RegExp(regexString);
    changeStyle(myregexp);
}
