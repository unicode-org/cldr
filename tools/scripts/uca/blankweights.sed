# Blank out non-zero weights.
# Helper script for manual review of UCA DUCET and CLDR root collation data files.
# Most of the collation element weights change with every new version.
# "Blanking out" the weights makes files comparable,
# for finding changes in sort order and changes in lengths of weights.
#
# sed -r -f blankweights.sed FractionalUCA.txt > frac-7.0.txt

# protect allkeys 0000 weights
s/0000/@@4ZEROES@@/g

# fractional primary weights
s/\[[0-9A-F]{2},/[pp,/g
s/\[[0-9A-F]{2} [0-9A-F]{2},/[pp pp,/g
s/\[[0-9A-F]{2} [0-9A-F]{2} [0-9A-F]{2},/[pp pp pp,/g
# fractional secondary weights
s/, [0-9A-F]{2},/, ss,/g
s/, [0-9A-F]{2} [0-9A-F]{2},/, ss ss,/g
# fractional tertiary weights
s/, [0-9A-F]{2}\]/, tt]/g

# allkeys primary weights
s/\[[0-9A-F]{4}/[pppp/g
s/\[([.*])[0-9A-F]{4}/[\1pppp/g
# allkeys secondary weights
s/\.[0-9A-F]{4}\./.ssss./g
# leave fixed allkeys tertiary weights
# s/\.[0-9A-F]{4}\]/.tttt]/g

# restore zero weights
s/@@4ZEROES@@/0000/g
