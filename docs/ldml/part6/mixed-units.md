## <a name="Mixed_Units" id="Mixed_Units" href="#Mixed_Units">Mixed Units</a>

Mixed units, or unit sequences, are units with the same base unit which are listed in sequence.
Common examples are feet and inches; meters and centimeters; hours, minutes, and seconds; degrees, minutes, and seconds.
Mixed unit identifiers are expressed using the "-and-" infix, as in "foot-and-inch", "meter-and-centimeter", "hour-and-minute-and-second", "degree-and-arc-minute-and-arc-second."

Scalar values for mixed units are expressed in the largest unit, according to the sort order discussed above in "Normalization".
For example, numbers for "foot-and-inch" are expressed in feet.

Mixed unit identifiers should be from highest to lowest (eg foot-and-inch instead of inch-and-foot), and that is reflected in the display.
If it turns out that some locales present certain mixed units in a different order, additional structure will be needed in CLDR.

Only the lowest unit can have decimal fractions; the higher units will be integers, so no "3.5 feet 3 inches".
If a number is negative, then only the highest unit shows the minus sign: eg, "-3 hours 27 minutes".
If one of the units is zero, then it is normally omitted: eg, "3 feet" instead of "3 feet 0 inches".
However, when all of the units would be omitted, then the highest unit is shown with zero: eg "0 feet".

Implementations may offer mechanisms to control the precision of the formatted mixed unit. Examples include, but are not limited to:
* An implementation could apply the precision of a number formatter to the final unit.
  However, this approach has a couple of disadvantages, such as matching precision across user preferences. For example, suppose the input amount is 1.5254 and the precision is 2 decimals.
    * Locale A uses decimal degrees and gets 1.53°.
    * Locale B uses degrees, minutes, seconds, and gets 1° 31′ 31.44″
	* Locale B has an unnecessarily precise result: the equivalent of 1.52540 in precision.
* An implementation could match the decimal precision that would be used with just the first unit, such as the following:
    * Two decimal digits with degrees is 1.53°, representing a range of 1.525° to 1.535°
    * Only continue adding subunits (or fractions in the final unit) if the current amount is not within that range.
       * 1° 31′ => 1.516666667, so it is not within that range, and we add another subunit
       * 1° 31′ 31″ => 1.525277778, so it is within range, and we don't add any fractional units

The default behavior is to round the lowest unit to the nearest integer.
Thus 1.99959 degree-and-arc-minute-and-arc-second would be (before rounding) **1 degree 59 minutes 58.524 seconds**.
After rounding it would be **1 degree 59 minutes 59 seconds**.

If the lowest unit would round to zero, or round up to the size of the next higher unit, then the next higher unit is rounded instead, recursively.
Thus 1.999862 degree-and-arc-minute-and-arc-second would be (before rounding) **1 degree 59 minutes 59.5032 degrees**.
After rounding the last unit it would be **1 degree 59 minutes 60 seconds**, which rounds up to **1 degree 60 minutes**, which rounds up to  **2 degrees**.
This behavior can be determined before having to compute the lower units:
for example, where rounding to the second, if the remainder in degrees is below 1/120 degrees or above 119/120 degrees, then the degrees can be rounded without computing the minutes or seconds.

