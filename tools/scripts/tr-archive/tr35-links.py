#!/usr/bin/python3
#
# SPDX-License-Identifier: Unicode-3.0
# Date: 26 Mar 2026
# Copyright © 2026 Unicode, Inc.
# Unicode and the Unicode Logo are registered trademarks of Unicode, Inc.
# in the United States and other countries.

from yaml import safe_load
import argparse
import re
import sys
import os

ver_match = re.compile(r'^v([0-9]+(?:\.[0-9]+){0,2})$')
rev_match = re.compile(r'^r([0-9]{2,3})$')

BASE_HOST = "https://www.unicode.org"
BASE_PATH = "/reports/tr35"

def write_gss(s):
    if not 'GITHUB_STEP_SUMMARY' in os.environ:
        return
    try:
        file = open(os.environ['GITHUB_STEP_SUMMARY'], 'a')
        print(s, file=file, flush=True)
    except IOError as e:
        print("Err writing GITHUB_STEP_SUMMARY", e, file=sys.stderr)

def revision_to_path(s):
    rev = get_revision(s)
    return '%s/tr35-%s' % ( BASE_PATH, rev)

def version_to_path(s):
    ver = get_version(s)
    return '%s/%s' % ( BASE_PATH, ver)

def revision_to_url(s):
    return '%s%s' % ( BASE_HOST, revision_to_path(s))

def version_to_url(s):
    return '%s%s' % ( BASE_HOST, version_to_path(s))

def get_version(s):
    """Return version number or None, v48.2 -> 48.2"""
    try:
        m = ver_match.match(s)
    except:
        return None
    if m is None:
        return None
    return m.group(1)

def get_revision(s):
    """Return Revision number or None, r79 -> 79"""
    try:
        m = rev_match.match(s)
    except:
        return None
    if m is None:
        return None
    return m.group(1)

def is_version(s):
    """Return True if s is a version such as v48.2"""
    return get_version(s) is not None

def is_revision(s):
    """Return True if s is a revision r79"""
    return get_revision(s) is not None

def fail(msg):
    write_gss('### :x: %s' % msg)
    raise Exception('%s: FAIL: %s' % (config_name, msg))

def msg(m):
    print('%s: %s' % (config_name, m), file=sys.stderr)
    write_gss('- %s' % m)

def write_gss_link(src, trg):
    """src: revision, trg: version OR dev | latest"""
    src_url = revision_to_url(src)
    if is_version(trg):
        trg_url = version_to_url(trg)
    elif trg == 'latest':
        trg_url = BASE_HOST + BASE_PATH
    else:
        # dev or something else
        trg_url = "%s%s/%s" % (BASE_HOST, BASE_PATH, trg)
    # reversed: the 'target' of the link is the 'from' url
    write_gss('| <%s> | <%s> |' % (trg_url, src_url))

def check(data):
    """Check that the config file is valid, or fail"""
    msg('Checking')
    if not 'spec' in data:
        fail('Missing key: spec:')
    spec = data['spec']
    for k in ['dev','latest']:
        if not k in spec:
            fail('Missing key spec.%s:' % k)
        if not is_revision(spec[k]):
            fail('Not a revision: spec.%s: %s' % (k, spec[k]))
    if not 'versions' in spec:
        fail('Missing key: spec.versions:')
    versions = spec['versions']
    for version in versions.keys():
        if not is_version(version):
            fail('Not a version: spec.versions.%s' % version)
        rev = versions[version]
        if not is_revision(rev):
            fail('Not a revision: spec.versions.%s: %s' % (version, rev))
    msg('All OK')
    write_gss('')
    write_gss('')
    # for Github, print a table of links
    write_gss('| from | to |')
    write_gss('| --- | --- |')
    for k in ['dev','latest']:
        write_gss_link(spec[k], k)
    for version in versions.keys():
        write_gss_link(versions[version], version)


## Parse Args

parser = argparse.ArgumentParser()
parser.add_argument("config", help="path to tr35-versions.yml", action="store");
parser.add_argument("action", help="'check', 'link', or 'redirects'", default="check", action="store")
parser.add_argument("--dry-run", help="Don't execute commands, just print", action="store_true")
parser.add_argument("--dir", help="Target dir for symlinks", action="store", default=".", type=str)
args = parser.parse_args()

config_name = args.config
action_name = args.action
dry_run = args.dry_run
target_dir = args.dir

# write the Github Step Summary
write_gss('## tr35-links')
write_gss('')
write_gss('- ConfigFile = `%s`' % config_name)
write_gss('- Action = `%s`' % action_name)
write_gss('')

if action_name not in ["check", "link", "redirects"]:
    raise Exception('Unknown action %s' % action_name)

## Load config

msg('Loading')

data = safe_load(open(config_name, "r"))

def link_cmd(src, trg):
    trg_path = os.path.join(target_dir, trg)
    if dry_run:
        msg("# LINK %s -> %s" % (src, trg_path))
    else:
        if os.path.isdir(trg_path) or os.path.isfile(trg_path):
            fail("Exists and isn't a symlink: %s" % trg_path)
        elif os.path.islink(trg_path):
            old_link = os.readlink(trg_path)
            if old_link != src:
                msg("# - unlink %s (was: %s)" % (trg_path, old_link))
                os.unlink(trg_path)
            else:
                msg("# OK: %s -> %s" % (src, trg_path))
                return  # already linked
        msg("# - symlink %s -> %s" % (src,trg_path))
        os.symlink(src, trg_path)

## perform the check
check(data)
write_gss('## :white_check_mark: %s OK' % config_name)

## we might be done
if action_name == 'check':
    # check and exit
    exit(0)
elif action_name == 'link':
    if not os.path.isdir(target_dir):
        fail("--dir=%s is not a directory" % target_dir)
    spec = data['spec']
    versions = data['spec']['versions']
    link_cmd("tr35-%s" % get_revision(spec['latest']), "latest")
    link_cmd("tr35-%s" % get_revision(spec['dev']), "dev"   )
    for version in versions.keys():
        link_cmd("tr35-%s" % get_revision(versions[version]), get_version(version))

elif action_name == 'redirects':
    # print in _redirects format
    print("# CLDR Spec in _redirects format")
    print("# Note: See <https://developers.cloudflare.com/pages/configuration/redirects/> for docs!")
    print("")
    print("%s/proposed.html %s/dev/tr35.html" % (BASE_PATH, BASE_PATH))
    print("%s/dev/* %s/:splat" % (BASE_PATH, revision_to_path(data['spec']['dev'])))
    for version in data['spec']['versions'].keys():
        revision = data['spec']['versions'][version]
        print("%s/* %s/:splat" % (version_to_path(version), revision_to_path(revision)))
    print("%s/tr35-*.html %s/tr35-:splat.html" % (BASE_PATH, revision_to_path(data['spec']['latest'])))
    print("%s/ %s/" % (BASE_PATH, revision_to_path(data['spec']['latest'])))
