// #member1
// Sanitiser for phone-number text fields. Keeps digits and dashes only, and caps the digit
// count at 11 -- a Malaysian mobile like 011-2345-6789 is 11 digits; dashes are not counted.
package com.example.asgm.data

const val MAX_PHONE_DIGITS = 11

fun sanitizePhone(input: String): String {
    val sb = StringBuilder()
    var digits = 0
    for (c in input) {
        when {
            c.isDigit() && digits < MAX_PHONE_DIGITS -> {
                digits++
                sb.append(c)
            }
            c == '-' -> sb.append(c)
        }
    }
    return sb.toString()
}
