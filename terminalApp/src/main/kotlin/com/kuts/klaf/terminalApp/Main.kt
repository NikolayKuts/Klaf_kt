package com.kuts.klaf.terminalApp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    runMosaicBlocking {
        TerminalApp()
    }
}

@Composable
private fun TerminalApp() {
    var frame by remember {
        mutableStateOf(AsciiDonutRenderer.render(rotationX = 0.0, rotationZ = 0.0))
    }

    Column {
        Text("Klaf Terminal Donut")
        Text("--------------------")
        Text("")
        Text("Animated ASCII torus rendered through Mosaic.")
        Text("Press Ctrl+C to exit.")
        Text("")
        Text(frame)
    }

    LaunchedEffect(Unit) {
        var rotationX = 0.0
        var rotationZ = 0.0

        while (true) {
            frame = AsciiDonutRenderer.render(
                rotationX = rotationX,
                rotationZ = rotationZ,
            )
            rotationX += 0.07
            rotationZ += 0.03
            delay(33)
        }
    }
}

private object AsciiDonutRenderer {

    private const val ScreenWidth = 72
    private const val ScreenHeight = 28
    private const val ThetaStep = 0.07
    private const val PhiStep = 0.02
    private const val DistanceFromCamera = 5.0
    private const val TorusInnerOffset = 2.0
    private const val HorizontalScale = 30.0
    private const val VerticalScale = 15.0
    private const val MaxTheta = 6.28
    private const val MaxPhi = 6.28
    private const val ShadeRamp = ".,-~:;=!*#$@"

    fun render(
        rotationX: Double,
        rotationZ: Double,
    ): String {
        val depthBuffer = DoubleArray(ScreenWidth * ScreenHeight)
        val pixels = CharArray(ScreenWidth * ScreenHeight) { ' ' }

        var theta = 0.0
        while (theta < MaxTheta) {
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)

            var phi = 0.0
            while (phi < MaxPhi) {
                val sinPhi = sin(phi)
                val cosPhi = cos(phi)
                val sinX = sin(rotationX)
                val cosX = cos(rotationX)
                val sinZ = sin(rotationZ)
                val cosZ = cos(rotationZ)
                val circleX = cosTheta + TorusInnerOffset
                val inverseDepth = 1.0 / (sinPhi * circleX * sinX + sinTheta * cosX + DistanceFromCamera)
                val projectedCircle = sinPhi * circleX * cosX - sinTheta * sinX
                val x = (ScreenWidth / 2 + HorizontalScale * inverseDepth * (cosPhi * circleX * cosZ - projectedCircle * sinZ)).toInt()
                val y = (ScreenHeight / 2 + VerticalScale * inverseDepth * (cosPhi * circleX * sinZ + projectedCircle * cosZ)).toInt()

                if (x in 0 until ScreenWidth && y in 0 until ScreenHeight) {
                    val index = x + ScreenWidth * y
                    val luminance = (
                        (sinTheta * sinX - sinPhi * cosTheta * cosX) * cosZ -
                            sinPhi * cosTheta * sinX -
                            sinTheta * cosX -
                            cosPhi * cosTheta * sinZ
                        ) * 8.0

                    if (inverseDepth > depthBuffer[index]) {
                        depthBuffer[index] = inverseDepth
                        val shadeIndex = luminance.toInt().coerceIn(0, ShadeRamp.lastIndex)
                        pixels[index] = ShadeRamp[shadeIndex]
                    }
                }

                phi += PhiStep
            }

            theta += ThetaStep
        }

        return buildString {
            for (row in 0 until ScreenHeight) {
                append(pixels, row * ScreenWidth, ScreenWidth)
                if (row != ScreenHeight - 1) {
                    append('\n')
                }
            }
        }
    }
}
