package site.lempert.kmp_user_agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import site.lempert.useragent.Component
import site.lempert.useragent.UserAgentGenerator
import site.lempert.useragent.UserAgentInfo
import site.lempert.useragent.UserAgentParser

/**
 * Thin harness proving `:library` works as a consumed dependency on Android --
 * not a real app experience. Parses a representative UA string and generates
 * a UA string from structured data, displaying both as text.
 */
private const val SAMPLE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/91.0.4472.120 Mobile Safari/537.36"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            val parsed = remember { UserAgentParser.parse(SAMPLE_USER_AGENT) }
            val generated = remember {
                UserAgentGenerator.generate(
                    UserAgentInfo(
                        browser = Component("Chrome", "91.0"),
                        engine = Component("Blink", "91.0"),
                        os = Component("Android", "12"),
                        device = null,
                    ),
                )
            }

            Text("UserAgentParser.parse():")
            Text("UA: $SAMPLE_USER_AGENT")
            Text("browser: ${parsed.browser}")
            Text("engine: ${parsed.engine}")
            Text("os: ${parsed.os}")
            Text("device: ${parsed.device}")
            Text("UserAgentGenerator.generate():")
            Text(generated)
        }
    }
}
