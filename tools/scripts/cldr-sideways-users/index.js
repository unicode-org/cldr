// © unicode

const parseString = require('xml2js').parseString;
const fs = require('fs');
const csvWriter = require('csv-write-stream')
const writer = csvWriter()
const argv = require('minimist')(process.argv.slice(2));

if ( ! argv.users ) {
    throw new Error('Please call me with --users=/path/to/users.xml');
}

const xml = fs.readFileSync(argv.users);

// result.users.user[0] =
// { '$': 
//    { id: '_',
//      email: '_@_._',
//      level: 'tc',
//      name: '_ _',
//      org: '_',
//      locales: 'pt nr tlh' } }


parseString(xml, function (err, result) {
    if(err) return console.dir(err);

    var map = result.users.user.reduce(
        function(p, v, idx) {
            if(!p) throw new Error('Why is p null?)');
            const u = v['$'];
            if(u.level === 'locked') return p; // skip locked

            u.locales.split(' ').forEach(function(l) {
                // if the locale isn’t there, add a placeholder
                if(!p[l]) {
                    p[l] = {};
                }
                // key by email
                p[l][u.email] = u;
            });

            return p;
        }, {}
    );

    function userFmt(u) {
        return u.name + ' <'+u.email+'>%'+u.org+'#'+u.level;
    }

    //console.dir(map, {color: true, depth: Infinity});
    const writer = csvWriter({ headers: ["locale", "vettercount", "vetters"]});
    const csvFile = argv.csv || 'users.csv';
    writer.pipe(fs.createWriteStream(csvFile))
    Object.keys(map).sort().forEach(function(loc) {
        const row = map[loc];
        console.log(loc,Object.keys(row).length);
        const vetterstr = Object.keys(row).sort(function(a, b) {
            return (row[a].name+row[a].org).localeCompare(row[b].name+row[b].org);
        }).reduce(function(p, v){
            const u = row[v];
            const uInfo = userFmt(u);
            console.log('\t',u.name + ' <'+u.email+'>\t'+u.org+'\t'+u.level);
            p = p + uInfo +'|';
            return p;
        }, '');
        writer.write([loc, Object.keys(row).length, vetterstr]);
    });
    writer.end();
    console.warn('# Wrote to: ' + csvFile);
});
