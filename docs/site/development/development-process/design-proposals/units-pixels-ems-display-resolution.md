---
title: 'Units: pixels, ems, display resolution'
---

# Units: pixels, ems, display resolution

|  |  |
|---|---|
| Author | Peter Edberg |
| Date | 2019-05-21 |
| Status | Proposal |
| Feedback to | pedberg (at) unicode (dot) org |
| Bugs | [CLDR-9996](https://unicode-org.atlassian.net/browse/CLDR-9996) Add units for  pixels , ems, and display resolution<br /> [CLDR-8076](https://unicode-org.atlassian.net/browse/CLDR-8076#icft=CLDR-8076) Proposal: add units for dot density and pixel density |

This is a proposal to add 7 units related to graphics, imaging, typography.

| unit | abbr (en) | proposed category | notes |
|---|---|---|---|
| pixel<br /> megapixel | px<br /> MP | new category? | A pixel is the smallest resolvable element of a bitmap image. In many usages it does not have a fixed size, and is just used for counting (e.g. an image has 360 pixels vertically and 480 horizontally). In some graphic usages it is specifically 1/96 inch. In CSS it is the smallest resolvable element on a display, but specifically means 1/96 inch on a printer. Thus sometimes it is a property associated with an image, and sometimes with a device. |
| dots-per-inch<br /> dots-per-centimeter | dpi<br /> dpcm | new category?<br /> or concentr | A dot is the smallest displayable element on a device, typically used for printers. Measurements using dots per inch or centimeter are used to indicate printer resolution, and are a kind of linear density. |
| pixels-per-inch<br /> pixels-per-centimeter | ppi<br /> ppcm | new category?<br /> or concentr | Measurements using pixels per inch or centimeter are sometimes used to indicate display resolution, and are a kind of linear density. |
| em | em | new category? or length | A typographic em is a unit of length that corresponds to the point size of a font (so it does not have a fixed size). |

We could consider adding a new category for these, say “graphics”; otherwise some of them do not fit reasonably into any existing category.

Per TC meeting 2019-05-22:

- Use singular for the internal key name, e.g. “dot-per-inch”
- Put all of these in a new category “graphics”

Some reference material:

- https://www.w3.org/Style/Examples/007/units.en.html
- https://en.wikipedia.org/wiki/Pixel
- https://en.wikipedia.org/wiki/Display_resolution
- https://en.wikipedia.org/wiki/Dots_per_inch
- https://en.wikipedia.org/wiki/Pixel_density
- https://en.wikipedia.org/wiki/Em_(typography)

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
