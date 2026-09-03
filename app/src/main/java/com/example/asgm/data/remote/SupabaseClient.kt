// #member1
// Single Supabase client the whole app shares, same setup as Practical 9.
package com.example.asgm.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// Project Settings -> API on the Supabase dashboard.
const val SUPABASE_URL = "https://mmcmoewehhwvwwhxjabk.supabase.co"
const val SUPABASE_KEY = "sb_publishable_cUr6vKSgRVjoDOhU5B1BTg_mMzM0zQN"

// false while these are still the placeholder strings -- every ViewModel checks this before
// touching Supabase at all, so a not-yet-configured project never crashes anything.
val isSupabaseConfigured: Boolean =
    SUPABASE_URL != "SERVER URL" && SUPABASE_KEY != "API KEY"

// `by lazy`, not a plain val: top-level properties in one file share one static initializer, so a
// plain val would build the client (and risk throwing on a bad URL) the moment anything in this
// file loads. Lazy defers that until something actually reads `supabase`.
val supabase by lazy {
    createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
    }
}
