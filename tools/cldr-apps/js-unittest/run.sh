
# This needs to be run from the cldr folder
# "http-server" can be installed by "npm install --global http-server"
# Optionally, to run this as cldrtestjs, you may have in .bash_profile:
# alias cldrtestjs='cd [path/to/cldr] && sh tools/cldr-apps/js-unittest/run.sh'
http-server -p 8008 -o tools/cldr-apps/js-unittest/Test.html
