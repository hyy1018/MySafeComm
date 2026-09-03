// App entry point: sets the theme and hands off to the nav graph.
package com.example.asgm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.asgm.nav.AppNavGraph
import com.example.asgm.ui.theme.AsgmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsgmTheme {
                AppNavGraph()
            }
        }
    }
}
