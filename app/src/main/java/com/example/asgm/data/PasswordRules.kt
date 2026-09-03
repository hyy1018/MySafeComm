// #member1
// Password rule used everywhere a password gets set: sign up, admin add-admin, reset password.
package com.example.asgm.data

object PasswordRules {
    const val REQUIREMENT_MESSAGE = "Password must be at least 6 characters and include both letters and numbers"

    fun isValid(password: String): Boolean =
        password.length >= 6 && password.any { it.isLetter() } && password.any { it.isDigit() }
}
