package com.example.asgm.data

/**
 * Temporary stand-in for a real login/session system. Screens use this fixed resident
 * account id until the Login flow exists; [com.example.asgm.data.local.AppDatabase] seeds a
 * matching Users row so report/comment/like foreign keys resolve. Replace with real session
 * state (whoever is logged in) once Login is built.
 */
object DemoSession {
    const val CURRENT_USER_ID = "resident1"
}
