package com.example.ui.components

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
    val path = Path().apply {
      fillType = PathFillType.EvenOdd
      // Scaled cloud contour normalized from 108x108 viewport (27.5..81.5, 34.5..69.5)
      moveTo(w * 0.24f, h * 0.78f)
      lineTo(w * 0.78f, h * 0.78f)
      cubicTo(w * 0.89f, h * 0.78f, w * 0.96f, h * 0.70f, w * 0.96f, h * 0.59f)
      cubicTo(w * 0.96f, h * 0.49f, w * 0.89f, h * 0.41f, w * 0.80f, h * 0.40f)
      cubicTo(w * 0.77f, h * 0.26f, w * 0.65f, h * 0.17f, w * 0.52f, h * 0.17f)
      cubicTo(w * 0.38f, h * 0.17f, w * 0.27f, h * 0.27f, w * 0.25f, h * 0.41f)
      cubicTo(w * 0.14f, h * 0.43f, w * 0.06f, h * 0.52f, w * 0.06f, h * 0.63f)
      cubicTo(w * 0.06f, h * 0.72f, w * 0.14f, h * 0.78f, w * 0.24f, h * 0.78f)
      close()

      // Negative-space checkmark cutout
      moveTo(w * 0.45f, h * 0.66f)
      lineTo(w * 0.33f, h * 0.54f)
      lineTo(w * 0.38f, h * 0.49f)
      lineTo(w * 0.45f, h * 0.56f)
      lineTo(w * 0.65f, h * 0.36f)
      lineTo(w * 0.70f, h * 0.41f)
      close()
    }
    drawPath(path = path, color = tint)
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
