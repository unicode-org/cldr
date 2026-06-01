/* tr35search.js */

if (document.location.hostname.endsWith('unicode.org')) {
    document.getElementById("searchbox").style.display = "inline";


    function dosearch() {
        const text = document.getElementById("searchfield").value;
        if(!text || !text.trim()) return;
        const u = new URL('https://www.google.com/search?q=site%3Aunicode.org%2Freports%2Ftr35%2F+');
        let q = u.searchParams.get('q');
        q = q + text; // append their search
        u.searchParams.set('q', q);
        document.location.assign(u); // Go!
    }
    document.getElementById("searchbutton").addEventListener("click", dosearch);
    document.getElementById("searchfield").addEventListener("keyup", (event) => {
        if(event.key === "Enter" || event?.keyCode === 13) {
            dosearch();
        }
    });
}
