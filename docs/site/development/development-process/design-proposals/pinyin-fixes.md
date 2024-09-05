---
title: Pinyin Fixes
---

# Pinyin Fixes

As a part of the CLDR updates for Unicode 5.2, I've been looking at the pinyin support. This is in two areas:

- We transform from Han characters to Pinyin
- We sort according to Pinyin

According to the directions from Richard Cook, the best algorithm to get the most frequently used pinyin reading is to use all kHanyuPinlu readings first; then take all kXHC1983; then kHanyuPinyin. Using a program to get this, and compare against the pinyin sorting and transforms, we get discrepancies. For example, for sorting there are about 1500 cases (see attachment). The format is:

?? for items that look out of place (using a heuristic algorithm). Example:

?? 606 \* kē (607) 錒

The 606 is the "distance" from surrounding cases, the 607 is the rank order of the pinyin.

Where there are multiple readings in Unihan, they are given in the format with --:

?? 1 ào (20) 坳 垇

&emsp;-- 坳 {ào=[xh, pn, ma], āo=[pn, ma], yǒu=[pn]}

&emsp;-- 垇 {ào=[xh, ma], āo=[ma]}

- lu is kHanyuPinlu
- xh is kXHC1983
- pn is kHanyuPinyin
- ma is kMandarin

[pinyinSortComparison.txt](https://drive.google.com/file/d/1XFMmbjipcf6pTH2VOJ_KOnjSdpkvyLcq/view?usp=sharing)

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)