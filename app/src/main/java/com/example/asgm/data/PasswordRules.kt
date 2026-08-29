package com.example.asgm.data

/** Shared password policy, used anywhere an account's password is set: Sign Up, Admin-created
 * accounts, and Admin password resets. */
object PasswordRules {
    const val REQUIREMENT_MESSAGE = "Password must be at least 6 characters and include both letters and numbers"

    fun isValid(password: String): Boolean =
        password.length >= 6 && password.any { it.isLetter() } && password.any { it.isDigit() }
}
