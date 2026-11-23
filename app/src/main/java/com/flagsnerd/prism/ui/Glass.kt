package com.flagsnerd.prism.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flagsnerd.prism.data.Store

// ---------------------------------------------------------------------------
// Liquid glass: a REAL backdrop blur of whatever is scrolling behind the bar,
// plus a translucent tint, a soft top sheen and a specular rim that is bright
// at the top-left / bottom-right edges and fades in between.
//
// How the blur works: PrismRoot records the app content (aurora + screens)
// into a GraphicsLayer (see [glassSource]) and provides it through
// [LocalGlassSource]. Every [glassPanel] draws that same layer again behind
// itself, shifted to line up with the screen, with a RenderEffect blur on it.
// Needs Android 12+ (API 31); older phones get the frosted solid fallback.
// ---------------------------------------------------------------------------

val LocalGlassSource = staticCompositionLocalOf<GraphicsLayer?> { null }

/** Marks the content that glass panels blur. Put it on the full-screen box behind the bars. */
fun Modifier.glassSource(layer: GraphicsLayer): Modifier = this.drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}

@Composable
private fun Modifier.blurBehind(source: GraphicsLayer, shape: Shape, radius: Dp): Modifier {
    val layer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            val r = radius.toPx()
            layer.renderEffect = BlurEffect(r, r, TileMode.Clamp)
            layer.record {
                translate(-pos.x, -pos.y) { drawLayer(source) }
            }
            drawLayer(layer)
        }
}

private fun rimBrush(k: Float): Brush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = (0.55f * k).coerceAtMost(0.9f)),
        Color.White.copy(alpha = 0.06f),
        Color.White.copy(alpha = 0.02f),
        Color.White.copy(alpha = (0.30f * k).coerceAtMost(0.8f)),
    )
)

/** Light glass for small things (chips, buttons, cards over the aurora). [level] = glass intensity. */
fun Modifier.glass(shape: Shape, alpha: Float = 0.16f, level: Float = Store.glassLevel): Modifier {
    val a = (alpha * level).coerceIn(0.03f, 0.5f)
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = a + 0.07f), Color.White.copy(alpha = a * 0.55f))
            ),
            shape
        )
        .border(1.dp, rimBrush(level), shape)
}

/** Heavier glass for bars that sit over scrolling content (nav bar, mini player): live blur on Android 12+. */
@Composable
fun Modifier.glassPanel(shape: Shape): Modifier {
    val k = Store.glassLevel
    val cs = MaterialTheme.colorScheme
    val source = LocalGlassSource.current
    val live = source != null && Build.VERSION.SDK_INT >= 31
    val body = if (live) (0.46f - (k - 1f) * 0.22f).coerceIn(0.22f, 0.62f)
    else (0.88f - (k - 1f) * 0.30f).coerceIn(0.60f, 0.95f)
    val base = if (live && source != null) this.blurBehind(source, shape, 26.dp) else this.clip(shape)
    return base
        .background(cs.surfaceContainerHigh.copy(alpha = body), shape)
        .background(cs.primary.copy(alpha = if (live) 0.06f else 0f), shape)
        .background(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f * k), Color.Transparent)),
            shape
        )
        .border(1.dp, rimBrush(k), shape)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), 0.09f).padding(18.dp)
    ) { content() }
}

// ---------------------------------------------------------------------------
// Motion
// ---------------------------------------------------------------------------

/** Click with a springy squish. Respects the Animations setting. */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && Store.animations) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** Remembers which items already played their entrance so scrolling back doesn't replay it. */
object Stagger {
    private val seen = HashSet<Any>()
    fun reset() { seen.clear() }
    fun claim(key: Any): Boolean = seen.add(key)
}

/** Fade + rise entrance, staggered by [index]. Only the first dozen items animate. */
@Composable
fun Modifier.staggerIn(index: Int, key: Any): Modifier {
    val play = remember { Store.animations && index <= 11 && Stagger.claim(key) }
    if (!play) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        p.animateTo(1f, tween(durationMillis = 450, delayMillis = index * 50, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 48f
    }
}

// ---------------------------------------------------------------------------
// Aurora background: three soft colour blobs drifting slowly behind the app,
// tinted from the current theme. Gives the glass something to refract.
// ---------------------------------------------------------------------------

@Composable
fun AuroraBackground() {
    if (Store.animations) {
        val t = rememberInfiniteTransition(label = "aurora")
        val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(15000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
        val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(21000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
        val c by t.animateFloat(0f, 1f, infiniteRepeatable(tween(27000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c")
        AuroraCanvas({ a }, { b }, { c })
    } else {
        AuroraCanvas({ 0.4f }, { 0.5f }, { 0.6f })
    }
}

@Composable
private fun AuroraCanvas(a: () -> Float, b: () -> Float, c: () -> Float) {
    val cs = MaterialTheme.colorScheme
    val bg = cs.background
    val c1 = cs.primaryContainer.copy(alpha = 0.85f)
    val c2 = cs.primary.copy(alpha = 0.24f)
    val c3 = lerp(cs.primaryContainer, cs.primary, 0.5f).copy(alpha = 0.50f)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r = maxOf(w, h)
        val pa = a()
        val pb = b()
        val pc = c()
        drawRect(bg)
        blob(c1, Offset(w * (0.10f + 0.60f * pa), h * (0.05f + 0.25f * pb)), r * 0.80f)
        blob(c2, Offset(w * (0.95f - 0.65f * pb), h * (0.50f + 0.20f * pc)), r * 0.70f)
        blob(c3, Offset(w * (0.15f + 0.55f * pc), h * (0.98f - 0.30f * pa)), r * 0.75f)
    }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(color, Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center
    )
}
