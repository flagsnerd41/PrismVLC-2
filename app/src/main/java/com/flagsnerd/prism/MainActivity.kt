package com.flagsnerd.prism

import android.app.PictureInPictureParams
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flagsnerd.prism.player.PlayerController
import com.flagsnerd.prism.ui.PrismRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        addOnPictureInPictureModeChangedListener { info ->
            UiState.inPip = info.isInPictureInPictureMode
        }
        handleIntent(intent)
        setContent { PrismRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        UiState.refresh++
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PlayerController.showPlayer && PlayerController.isPlaying) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            )
        }
    }

    private fun handleIntent(i: Intent?) {
        if (i == null) return
        if (i.getBooleanExtra("update", false)) UiState.openUpdate = true
        if (i.action == Intent.ACTION_VIEW && i.data != null) UiState.pendingExternal = i.data
    }
}
