package com.example.asgm.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// START - <Supabase connection>
// TODO: replace with your project's own values (Supabase dashboard -> Project Settings -> API).
// SUPABASE_URL looks like "https://xxxxxxxxxxxx.supabase.co".
// SUPABASE_KEY is the "anon public" key, NOT the service_role key.
const val SUPABASE_URL = "SERVER URL"
const val SUPABASE_KEY = "API KEY"

// True once the two placeholders above have been replaced with real values. Every ViewModel's
// cloud-write call checks this FIRST and skips the Supabase call entirely when false, so the
// SDK's client object is never touched (and can't throw) before the project is actually set up.
val isSupabaseConfigured: Boolean =
    SUPABASE_URL != "SERVER URL" && SUPABASE_KEY != "API KEY"

// Supabase client instance, created once for the whole app the first time something actually
// reads it. `by lazy` (not a plain `val`) matters here: every top-level property in this file
// compiles into one shared class with one shared static initializer, so a plain `val` would build
// this client -- and risk throwing on the placeholder URL above -- the moment ANYTHING in this
// file is touched, including just reading `isSupabaseConfigured`. `by lazy` defers that until a
// caller actually reads `supabase`, which every call site below only does after checking
// isSupabaseConfigured is true.
val supabase by lazy {
    createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
    }
}
// END - <Supabase connection>
