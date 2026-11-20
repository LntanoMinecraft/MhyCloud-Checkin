package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.GameType
import com.example.ui.theme.LocalStatusColors

/**
 * Plump rounded cloud path normalized from the adaptive launcher icon's 46x32dp cloud geometry.
 */
private fun buildPlumpCloudPath(w: Float, h: Float): Path {
  // Source cloud bounds: x in [31..77] (span 46), y in [38..70] (span 32), placed inside a square box
  val sx = w / 46f
  val sy = h / 46f
  val offsetY = (h - 32f * sy) * 0.5f
  fun x(v: Float) = (v - 31f) * sx
  fun y(v: Float) = (v - 38f) * sy + offsetY

  return Path().apply {
    moveTo(x(39.5f), y(70.0f))
    lineTo(x(68.5f), y(70.0f))
    cubicTo(x(73.2f), y(70.0f), x(77.0f), y(66.2f), x(77.0f), y(61.5f))
    cubicTo(x(77.0f), y(57.1f), x(73.7f), y(53.5f), x(69.4f), y(53.0f))
    cubicTo(x(68.1f), y(44.5f), x(61.5f), y(38.0f), x(53.5f), y(38.0f))
    cubicTo(x(46.2f), y(38.0f), x(40.2f), y(43.4f), x(39.2f), y(50.5f))
    cubicTo(x(34.5f), y(51.2f), x(31.0f), y(55.2f), x(31.0f), y(60.2f))
    cubicTo(x(31.0f), y(65.6f), x(34.8f), y(70.0f), x(39.5f), y(70.0f))
    close()
  }
}

private fun buildCheckmarkStrokePath(w: Float, h: Float): Path {
  val sx = w / 46f
  val sy = h / 46f
  val offsetY = (h - 32f * sy) * 0.5f
  fun x(v: Float) = (v - 31f) * sx
  fun y(v: Float) = (v - 38f) * sy + offsetY

  return Path().apply {
    moveTo(x(46.2f), y(56.8f))
    lineTo(x(51.4f), y(61.8f))
    lineTo(x(62.2f), y(50.8f))
  }
}

/**
 * Minimalist geometric cloud + checkmark emblem synced with the app's adaptive monochrome icon.
 */
@Composable
fun BrandCloudEmblem(
  modifier: Modifier = Modifier,
  tint: Color = MaterialTheme.colorScheme.primary,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val sx = w / 46f
    val sy = h / 46f
    val offsetY = (h - 32f * sy) * 0.5f
    fun x(v: Float) = (v - 31f) * sx
    fun y(v: Float) = (v - 38f) * sy + offsetY

    val path = Path().apply {
      fillType = PathFillType.EvenOdd
      moveTo(x(39.5f), y(70.0f))
      lineTo(x(68.5f), y(70.0f))
      cubicTo(x(73.2f), y(70.0f), x(77.0f), y(66.2f), x(77.0f), y(61.5f))
      cubicTo(x(77.0f), y(57.1f), x(73.7f), y(53.5f), x(69.4f), y(53.0f))
      cubicTo(x(68.1f), y(44.5f), x(61.5f), y(38.0f), x(53.5f), y(38.0f))
      cubicTo(x(46.2f), y(38.0f), x(40.2f), y(43.4f), x(39.2f), y(50.5f))
      cubicTo(x(34.5f), y(51.2f), x(31.0f), y(55.2f), x(31.0f), y(60.2f))
      cubicTo(x(31.0f), y(65.6f), x(34.8f), y(70.0f), x(39.5f), y(70.0f))
      close()

      // Negative-space checkmark cutout
      moveTo(x(47.6f), y(55.4f))
      lineTo(x(51.4f), y(59.0f))
      lineTo(x(60.8f), y(49.4f))
      lineTo(x(63.6f), y(52.2f))
      lineTo(x(51.4f), y(64.4f))
      lineTo(x(44.8f), y(58.2f))
      close()
    }
    drawPath(path = path, color = tint)
  }
}

/**
 * Full-color app icon preview matching the pure-white Google-style adaptive launcher icon.
 */
@Composable
fun AppBrandLauncherIcon(
  size: Dp = 92.dp,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color.White,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    modifier = modifier.size(size),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(size * 0.16f),
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = this.size.width
        val h = this.size.height
        val cloudPath = buildPlumpCloudPath(w, h)
        val gradient = Brush.linearGradient(
          colorStops = arrayOf(
            0.0f to Color(0xFF4DABF7),
            0.50f to Color(0xFF4285F4),
            1.0f to Color(0xFF7C5CFF),
          ),
          start = Offset.Zero,
          end = Offset(w, h),
        )
        drawPath(path = cloudPath, brush = gradient)

        val checkPath = buildCheckmarkStrokePath(w, h)
        drawPath(
          path = checkPath,
          color = Color.White,
          style = Stroke(
            width = w * (4.0f / 46f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      }
    }
  }
}

/**
 * Refined geometric badge icon for Cloud Genshin / Cloud ZZZ using dynamic theme containers.
 */
@Composable
fun GameEmblemBadge(
  gameType: GameType,
  size: Dp = 42.dp,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  val containerColor = if (gameType == GameType.GENSHIN) {
    statusColors.genshinContainer
  } else {
    statusColors.zzzContainer
  }
  val iconTint = if (gameType == GameType.GENSHIN) {
    statusColors.onGenshinContainer
  } else {
    statusColors.onZzzContainer
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = containerColor,
    modifier = modifier.size(size),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(9.dp),
    ) {
      if (gameType == GameType.GENSHIN) {
        // 4-point celestial star emblem
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = this.size.width
          val h = this.size.height
          val cx = w / 2f
          val cy = h / 2f
          val star = Path().apply {
            moveTo(cx, 0f)
            quadraticTo(cx, cy, w, cy)
            quadraticTo(cx, cy, cx, h)
            quadraticTo(cx, cy, 0f, cy)
            quadraticTo(cx, cy, cx, 0f)
            close()
          }
          drawPath(path = star, color = iconTint)
        }
      } else {
        // Sleek stylized Z-bolt emblem
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = this.size.width
          val h = this.size.height
          val bolt = Path().apply {
            moveTo(w * 0.18f, h * 0.22f)
            lineTo(w * 0.82f, h * 0.22f)
            lineTo(w * 0.22f, h * 0.78f)
            lineTo(w * 0.82f, h * 0.78f)
          }
          drawPath(
            path = bolt,
            color = iconTint,
            style = Stroke(
              width = w * 0.18f,
              cap = StrokeCap.Round,
              join = StrokeJoin.Round,
            ),
          )
        }
      }
    }
  }
}
