/**
 * Operations around keyboard support
 */

import * as cldrClient from "./cldrClient.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

// See https://keyman.com/developer/keymanweb/ for the latest version and scripts to use
const KEYMAN_VERSION = "17.0.333";
const KEYMAN_SCRIPTS = ["keymanweb.js", "kmwuitoggle.js"];
const KEYMAN_BASE = "https://s.keyman.com/kmw/engine/";
const KEYMAN_ATTACH = `(
    function(kmw) {
        kmw.init({attachType:'auto'});
    }
)(keyman);`;


// are the scripts installed?
let installed = false;
let installing = false;
// is the option enabled?
let enabled = false;
// did we call init yet?
let initted = false;


function getRemoteScript(src) {
    const e = document.createElement('script');
    e.setAttribute('src', src);
    return e;
}

function getInlineScript(body) {
    const e = document.createElement('script');
    e.textContent = body;
    return e;
}

/** insert the script tags */
async function installKeyboards() {
    if (installing) return;
    installing = true;

    if (!installed) {
        const { body } = document;
        for (const script of KEYMAN_SCRIPTS) {
            const e = getRemoteScript(`${KEYMAN_BASE}${KEYMAN_VERSION}/${script}`);
            body.appendChild(e);
            await waitForLoad(e); // block here until loaded
        }
        const attach = getInlineScript(KEYMAN_ATTACH);
        body.appendChild(attach);
        installed = true;
        installing = false;
        updateKeyboardLocale(cldrStatus.getCurrentLocale());
    }

    function waitForLoad(e) {
        return new Promise((resolve, reject) => {
            try {
                e.addEventListener('load', resolve);
            } catch (err) {
                reject(err);
            }
        });
    }
}

/**
 * Update whether user has requested web keyboards
 * @param {boolean} enable  true if keyboards are enabled
 */
export function setUseKeyboards(enable) {
    if (enable != enabled) {
        if (enable) {
            installKeyboards().then(() => { enabled = true; });
        } else {
            enabled = false;
        }
    }
}

/**
 * Update the keyboard locale
 * @param {string} curLocale
 */
export function updateKeyboardLocale(curLocale) {
    if (installed && enabled) {
        // make sure keyman is loaded
        for (const { InternalName } of keyman.getKeyboards()) {
            keyman.removeKeyboards(InternalName);
            console.log(`Removed kbd: ${InternalName}`);
        }
        console.log(`Adding kbd: @${curLocale}`);
        keyman.addKeyboards(`@${curLocale}`);
        // end keyboards
    }
}

export function isEnabled() {
    return enabled;
}

/** Note: may need to call reload() in order to unload keyboard. */
export async function setEnabledPref(enable) {
    const client = await cldrClient.getClient();
    setUseKeyboards(enable);
    if (enable) {
        await client.apis.user.setSetting({setting: 'webkeyboard'});
    } else {
        await client.apis.user.removeSetting({setting: 'webkeyboard'});
    }
}
