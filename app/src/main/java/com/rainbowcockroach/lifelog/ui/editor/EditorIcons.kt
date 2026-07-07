package com.rainbowcockroach.lifelog.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom Material-style vector icons used by the editor toolbar. Defined locally as SVG path data
 * so we can stay on `material-icons-core` (the extended icon pack is huge and slows the build).
 */

private fun materialIcon(
    name: String,
    block: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply(block).build()

/** Material Design "photo_camera" glyph. */
val PhotoCameraIcon: ImageVector by lazy {
    materialIcon("PhotoCamera") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 15.2f)
            curveToRelative(1.77f, 0f, 3.2f, -1.43f, 3.2f, -3.2f)
            reflectiveCurveToRelative(-1.43f, -3.2f, -3.2f, -3.2f)
            reflectiveCurveToRelative(-3.2f, 1.43f, -3.2f, 3.2f)
            reflectiveCurveToRelative(1.43f, 3.2f, 3.2f, 3.2f)
            close()
            moveTo(9f, 2f)
            lineTo(7.17f, 4f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(6f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineToRelative(-3.17f)
            lineTo(15f, 2f)
            horizontalLineTo(9f)
            close()
            moveTo(12f, 17f)
            curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
            reflectiveCurveToRelative(2.24f, -5f, 5f, -5f)
            reflectiveCurveToRelative(5f, 2.24f, 5f, 5f)
            reflectiveCurveToRelative(-2.24f, 5f, -5f, 5f)
            close()
        }
    }
}

/** Material Design "image" glyph. */
val ImageIcon: ImageVector by lazy {
    materialIcon("Image") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(21f, 19f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineTo(5f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            close()
            moveTo(8.5f, 13.5f)
            lineToRelative(2.5f, 3.01f)
            lineTo(14.5f, 12f)
            lineToRelative(4.5f, 6f)
            horizontalLineTo(5f)
            lineToRelative(3.5f, -4.5f)
            close()
        }
    }
}

/** Material Design "link" glyph. */
val LinkIcon: ImageVector by lazy {
    materialIcon("Link") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3.9f, 12f)
            curveToRelative(0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
            horizontalLineToRelative(4f)
            verticalLineTo(7f)
            horizontalLineTo(7f)
            curveToRelative(-2.76f, 0f, -5f, 2.24f, -5f, 5f)
            reflectiveCurveToRelative(2.24f, 5f, 5f, 5f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-1.9f)
            horizontalLineTo(7f)
            curveToRelative(-1.71f, 0f, -3.1f, -1.39f, -3.1f, -3.1f)
            close()
            moveTo(8f, 13f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(-2f)
            horizontalLineTo(8f)
            verticalLineToRelative(2f)
            close()
            moveTo(17f, 7f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(1.9f)
            horizontalLineToRelative(4f)
            curveToRelative(1.71f, 0f, 3.1f, 1.39f, 3.1f, 3.1f)
            reflectiveCurveToRelative(-1.39f, 3.1f, -3.1f, 3.1f)
            horizontalLineToRelative(-4f)
            verticalLineTo(17f)
            horizontalLineToRelative(4f)
            curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
            reflectiveCurveToRelative(-2.24f, -5f, -5f, -5f)
            close()
        }
    }
}
