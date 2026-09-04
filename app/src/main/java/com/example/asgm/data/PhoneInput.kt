// #member1
// Sanitiser + validator for phone-number text fields. Keeps digits and dashes only, and caps
// the digit count at 11 -- a Malaysian mobile like 011-2345-6789 is 11 digits; dashes are not
// counted. Resident-entered numbers (personal contacts, profile) must be a full 10- or
// 11-digit line; the admin's SOS contacts are exempt so short codes like 999 still work.
package com.example.asgm.data

const val MAX_PHONE_DIGITS = 11
const val MIN_PHONE_DIGITS = 10

const val PHONE_LENGTH_MESSAGE = "Phone number must be 10 or 11 digits (dashes don't count)"

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

// Digits only, dashes and any other separators ignored.
fun phoneDigitCount(input: String): Int = input.count { it.isDigit() }

// A complete resident phone line: 10 or 11 digits, dashes not counted.
fun isValidPhone(input: String): Boolean = phoneDigitCount(input) in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
