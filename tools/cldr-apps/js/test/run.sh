# This needs to be run from the cldr folder
# Optionally, to run this as cldrtestjs, you may have in .bash_profile:
# alias cldrtestjs='cd [path/to/cldr] && sh tools/cldr-apps/js-unittest/run.sh'
cd tools/cldr-apps/js
npm install
npm run build-test
cd ../../..
npx open-cli tools/cldr-apps/js/test/Test.html
