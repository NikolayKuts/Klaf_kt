package com.kuts.klaf.terminalApp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.onPreviewKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.system.exitProcess

private const val TargetFrameDelayMillis = 33L
private const val HeaderLines = 3

private enum class DoomScreen {
    Title,
    Playing,
    GameOver,
}

@Composable
internal fun DoomTerminalApp() {
    val terminalState = LocalTerminalState.current
    val terminalWidth = terminalState.size.columns.coerceAtLeast(minimumValue = 48)
    val terminalHeight = terminalState.size.rows.coerceAtLeast(minimumValue = 20)
    val game = remember { DoomGameState() }
    var screen by remember { mutableStateOf(DoomScreen.Title) }
    var frame by remember { mutableStateOf("") }
    var fps by remember { mutableStateOf(0) }
    var uiTimeSeconds by remember { mutableStateOf(0.0) }

    LaunchedEffect(screen, terminalWidth, terminalHeight) {
        var lastFrameTimestamp = System.nanoTime()

        while (true) {
            val now = System.nanoTime()
            val deltaSeconds = ((now - lastFrameTimestamp) / 1_000_000_000.0)
                .coerceIn(minimumValue = 0.0, maximumValue = 0.12)
            lastFrameTimestamp = now
            uiTimeSeconds += deltaSeconds
            fps = if (deltaSeconds > 0.0) {
                (1.0 / deltaSeconds).roundToInt()
            } else {
                0
            }

            if (screen == DoomScreen.Playing) {
                game.update(deltaSeconds = deltaSeconds)
                if (game.isDead) {
                    screen = DoomScreen.GameOver
                }
            }

            frame = when (screen) {
                DoomScreen.Title -> renderTitleScreen(
                    width = terminalWidth,
                    height = terminalHeight,
                    blinkOn = ((uiTimeSeconds * 2.0).toInt() % 2) == 0,
                )

                DoomScreen.Playing -> AsciiDoomRenderer.render(
                    state = game,
                    width = terminalWidth,
                    height = terminalHeight,
                    fps = fps,
                )

                DoomScreen.GameOver -> renderGameOverScreen(
                    width = terminalWidth,
                    height = terminalHeight,
                    kills = game.kills,
                    totalEnemies = game.totalEnemies,
                    blinkOn = ((uiTimeSeconds * 2.0).toInt() % 2) == 0,
                )
            }

            delay(timeMillis = TargetFrameDelayMillis)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(6, 6, 6))
            .onPreviewKeyEvent { event ->
                when (screen) {
                    DoomScreen.Title -> {
                        when (event.key.lowercase()) {
                            "enter", "return", "\n", "\r" -> {
                                game.reset()
                                screen = DoomScreen.Playing
                                true
                            }

                            "escape", "esc", "q" -> {
                                exitProcess(status = 0)
                            }

                            else -> false
                        }
                    }

                    DoomScreen.Playing -> {
                        when (event.key.lowercase()) {
                            "w", "arrowup" -> {
                                game.moveForward()
                                true
                            }

                            "s", "arrowdown" -> {
                                game.moveBackward()
                                true
                            }

                            "a" -> {
                                game.strafeLeft()
                                true
                            }

                            "d" -> {
                                game.strafeRight()
                                true
                            }

                            "q", "arrowleft" -> {
                                game.turnLeft()
                                true
                            }

                            "e", "arrowright" -> {
                                game.turnRight()
                                true
                            }

                            "spacebar", "space", " " -> {
                                game.fire()
                                true
                            }

                            "escape", "esc", "m" -> {
                                screen = DoomScreen.Title
                                true
                            }

                            else -> false
                        }
                    }

                    DoomScreen.GameOver -> {
                        when (event.key.lowercase()) {
                            "enter", "return", "\n", "\r" -> {
                                game.reset()
                                screen = DoomScreen.Playing
                                true
                            }

                            "escape", "esc", "m" -> {
                                screen = DoomScreen.Title
                                true
                            }

                            "q" -> {
                                exitProcess(status = 0)
                            }

                            else -> false
                        }
                    }
                }
            },
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            value = frame,
            color = Color(132, 255, 148),
        )
    }
}

private class DoomGameState {

    private val layout = listOf(
        "################",
        "#..............#",
        "#..######..##..#",
        "#..#....#..##..#",
        "#..#....#......#",
        "#..#....####...#",
        "#..#...........#",
        "#..#######.##..#",
        "#........#.##..#",
        "#.######.#.....#",
        "#.#....#.#.###.#",
        "#.#....#.#...#.#",
        "#.######.###.#.#",
        "#.............##",
        "#..............#",
        "################",
    )

    val mapWidth = layout.first().length
    val mapHeight = layout.size
    val totalEnemies: Int
        get() = enemies.size
    val activeEnemies: List<Enemy>
        get() = enemies.filter { it.alive }
    val isDead: Boolean
        get() = health <= 0.0

    private val enemies = mutableListOf<Enemy>()

    var playerX = 2.5
        private set
    var playerY = 2.5
        private set
    var playerAngle = 0.0
        private set
    var health = 100.0
        private set
    var ammo = 30
        private set
    var kills = 0
        private set
    var elapsedSeconds = 0.0
        private set
    var motionBob = 0.0
        private set
    var muzzleFlashTime = 0.0
        private set
    var statusText = "Press Enter on the title screen to deploy."
        private set

    init {
        require(layout.map { row -> row.length }.distinct().size == 1) {
            "Doom map rows must be the same width."
        }
        reset()
    }

    fun reset() {
        playerX = 2.5
        playerY = 2.5
        playerAngle = 0.15
        health = 100.0
        ammo = 30
        kills = 0
        elapsedSeconds = 0.0
        motionBob = 0.0
        muzzleFlashTime = 0.0
        statusText = "Sector hot. Clear every imp in the maze."
        enemies.clear()
        enemies += Enemy(x = 13.5, y = 2.5)
        enemies += Enemy(x = 5.5, y = 6.5)
        enemies += Enemy(x = 12.5, y = 9.5)
        enemies += Enemy(x = 3.5, y = 11.5)
        enemies += Enemy(x = 10.5, y = 13.5)
    }

    fun update(deltaSeconds: Double) {
        elapsedSeconds += deltaSeconds
        muzzleFlashTime = (muzzleFlashTime - deltaSeconds).coerceAtLeast(minimumValue = 0.0)

        for (enemy in enemies) {
            if (!enemy.alive) {
                continue
            }

            enemy.animationTime += deltaSeconds
            val dx = playerX - enemy.x
            val dy = playerY - enemy.y
            val distanceToPlayer = hypot(dx, dy)
            val angleToPlayer = atan2(dy, dx)

            if (distanceToPlayer > 0.85 && hasLineOfSight(enemy.x, enemy.y, playerX, playerY)) {
                val speed = if (distanceToPlayer > 4.0) 1.3 else 0.9
                val nextX = enemy.x + cos(angleToPlayer) * speed * deltaSeconds
                val nextY = enemy.y + sin(angleToPlayer) * speed * deltaSeconds

                if (isWalkable(nextX, enemy.y, collisionRadius = 0.24) &&
                    isFarEnoughFromOtherEnemies(enemy = enemy, candidateX = nextX, candidateY = enemy.y)
                ) {
                    enemy.x = nextX
                }

                if (isWalkable(enemy.x, nextY, collisionRadius = 0.24) &&
                    isFarEnoughFromOtherEnemies(enemy = enemy, candidateX = enemy.x, candidateY = nextY)
                ) {
                    enemy.y = nextY
                }
            } else if (distanceToPlayer <= 0.85) {
                health = (health - 18.0 * deltaSeconds).coerceAtLeast(minimumValue = 0.0)
                statusText = if (health > 0.0) {
                    "Imp claws are tearing you apart."
                } else {
                    "Marine down."
                }
            }
        }

        if (activeEnemies.isEmpty() && !isDead) {
            statusText = "Sector clear. Press M or Esc to leave the level."
        } else if (health in 0.01..25.0 && !statusText.contains(other = "Marine down")) {
            statusText = "Critical damage. Find breathing room and keep firing."
        }
    }

    fun moveForward() {
        moveRelative(forwardStep = 0.42, strafeStep = 0.0)
    }

    fun moveBackward() {
        moveRelative(forwardStep = -0.34, strafeStep = 0.0)
    }

    fun strafeLeft() {
        moveRelative(forwardStep = 0.0, strafeStep = -0.32)
    }

    fun strafeRight() {
        moveRelative(forwardStep = 0.0, strafeStep = 0.32)
    }

    fun turnLeft() {
        playerAngle = normalizeAngle(angle = playerAngle - 0.18)
    }

    fun turnRight() {
        playerAngle = normalizeAngle(angle = playerAngle + 0.18)
    }

    fun fire() {
        if (ammo <= 0) {
            statusText = "Click. Out of shells."
            return
        }
        if (isDead) {
            return
        }

        ammo--
        muzzleFlashTime = 0.12

        val hitEnemy = activeEnemies
            .map { enemy -> enemy to hitScoreFor(enemy = enemy) }
            .filter { (_, score) -> score > 0.0 }
            .maxByOrNull { (_, score) -> score }
            ?.first

        if (hitEnemy != null) {
            hitEnemy.alive = false
            kills++
            statusText = if (activeEnemies.isEmpty()) {
                "Last imp dropped. Sector clear."
            } else {
                "Direct hit. ${activeEnemies.size} hostile(s) remaining."
            }
        } else {
            statusText = "Shot wide. Recenter and fire again."
        }
    }

    fun tileAt(x: Int, y: Int): Char {
        if (x !in 0 until mapWidth || y !in 0 until mapHeight) {
            return '#'
        }
        return layout[y][x]
    }

    private fun moveRelative(
        forwardStep: Double,
        strafeStep: Double,
    ) {
        if (isDead) {
            return
        }

        val nextX = playerX + cos(playerAngle) * forwardStep + cos(playerAngle + PI / 2.0) * strafeStep
        val nextY = playerY + sin(playerAngle) * forwardStep + sin(playerAngle + PI / 2.0) * strafeStep

        if (isWalkable(nextX, playerY, collisionRadius = 0.18)) {
            playerX = nextX
        }

        if (isWalkable(playerX, nextY, collisionRadius = 0.18)) {
            playerY = nextY
        }

        motionBob += abs(forwardStep) + abs(strafeStep)
    }

    private fun isWalkable(
        x: Double,
        y: Double,
        collisionRadius: Double,
    ): Boolean {
        val minX = floor(x - collisionRadius).toInt()
        val maxX = floor(x + collisionRadius).toInt()
        val minY = floor(y - collisionRadius).toInt()
        val maxY = floor(y + collisionRadius).toInt()

        for (checkY in minY..maxY) {
            for (checkX in minX..maxX) {
                if (tileAt(x = checkX, y = checkY) == '#') {
                    return false
                }
            }
        }

        return true
    }

    private fun isFarEnoughFromOtherEnemies(
        enemy: Enemy,
        candidateX: Double,
        candidateY: Double,
    ): Boolean {
        return enemies.none { other ->
            other !== enemy &&
                other.alive &&
                hypot(candidateX - other.x, candidateY - other.y) < 0.45
        }
    }

    private fun hitScoreFor(enemy: Enemy): Double {
        val dx = enemy.x - playerX
        val dy = enemy.y - playerY
        val distance = hypot(dx, dy)

        if (distance > 10.0) {
            return 0.0
        }

        val angleDifference = normalizeAngle(angle = atan2(dy, dx) - playerAngle)
        val lateralOffset = abs(sin(angleDifference) * distance)

        if (abs(angleDifference) > 0.18 || lateralOffset > 0.42) {
            return 0.0
        }

        if (!hasLineOfSight(playerX, playerY, enemy.x, enemy.y)) {
            return 0.0
        }

        return 1.0 / distance
    }

    private fun hasLineOfSight(
        fromX: Double,
        fromY: Double,
        toX: Double,
        toY: Double,
    ): Boolean {
        val dx = toX - fromX
        val dy = toY - fromY
        val distance = hypot(dx, dy)
        val stepCount = (distance / 0.08).roundToInt().coerceAtLeast(minimumValue = 1)

        for (step in 1 until stepCount) {
            val progress = step.toDouble() / stepCount
            val sampleX = fromX + dx * progress
            val sampleY = fromY + dy * progress
            if (tileAt(x = floor(sampleX).toInt(), y = floor(sampleY).toInt()) == '#') {
                return false
            }
        }

        return true
    }
}

private data class Enemy(
    var x: Double,
    var y: Double,
    var alive: Boolean = true,
    var animationTime: Double = 0.0,
)

private object AsciiDoomRenderer {

    private const val FieldOfView = PI / 3.1
    private const val ViewDepth = 16.0
    private const val RayStep = 0.035
    private const val SpriteScale = 1.35
    private const val CrosshairChar = '+'

    private val wallRamp = charArrayOf('@', '#', 'O', '=', '+', '-', '.', ' ')
    private val floorRamp = charArrayOf('#', 'x', '=', '-', '.', ' ')
    private val impSprite = listOf(
        "  ___  ",
        " /o o\\ ",
        "|  ^  |",
        "| '-' |",
        " \\_#_/ ",
        " /| |\\ ",
        "  / \\  ",
    )
    private val shotgunSprite = listOf(
        "         ____         ",
        "   _____/ __ \\_____   ",
        " _/____  ___  ____\\_  ",
        "       /_/  \\_\\       ",
    )
    private val muzzleSprite = listOf(
        "        . ** .        ",
        "         \\\\||//        ",
        "   _____/ __ \\_____   ",
        " _/____  ___  ____\\_  ",
        "       /_/  \\_\\       ",
    )

    fun render(
        state: DoomGameState,
        width: Int,
        height: Int,
        fps: Int,
    ): String {
        val sceneHeight = (height - HeaderLines).coerceAtLeast(minimumValue = 12)
        val scene = renderScene(
            state = state,
            width = width,
            height = sceneHeight,
        )

        val topLine = fitLine(
            text = "KLAF TERMINAL DOOM  HP ${state.health.roundToInt().coerceAtLeast(0)}  AMMO ${state.ammo}  " +
                "KILLS ${state.kills}/${state.totalEnemies}  FPS $fps",
            width = width,
        )
        val controlLine = fitLine(
            text = "W/S move  A/D strafe  Q/E or <-/-> turn  Space fire  Esc menu",
            width = width,
        )
        val statusLine = fitLine(text = state.statusText, width = width)

        return buildString {
            append(topLine)
            append('\n')
            append(controlLine)
            append('\n')
            append(statusLine)
            if (scene.isNotEmpty()) {
                append('\n')
                append(scene)
            }
        }
    }

    private fun renderScene(
        state: DoomGameState,
        width: Int,
        height: Int,
    ): String {
        val canvas = AsciiCanvas(width = width, height = height, fill = ' ')
        val depthBuffer = DoubleArray(size = width) { ViewDepth }
        val centerLine = (height / 2.0 + sin(state.motionBob * 1.9) * 0.6).roundToInt()

        for (column in 0 until width) {
            val rayAngle = normalizeAngle(
                angle = state.playerAngle - FieldOfView / 2.0 + (column.toDouble() / width) * FieldOfView
            )
            val rayResult = castRay(
                state = state,
                rayAngle = rayAngle,
            )
            depthBuffer[column] = rayResult.correctedDistance

            val wallHalfHeight = (height / rayResult.correctedDistance).coerceAtMost(height.toDouble())
            val ceiling = (centerLine - wallHalfHeight).roundToInt()
            val floor = (centerLine + wallHalfHeight).roundToInt()

            for (row in 0 until height) {
                canvas[column, row] = when {
                    row < ceiling -> skyShade(
                        row = row,
                        column = column,
                        horizon = centerLine,
                        elapsedSeconds = state.elapsedSeconds,
                    )

                    row in ceiling..floor -> wallShade(
                        correctedDistance = rayResult.correctedDistance,
                        boundary = rayResult.boundary,
                    )

                    else -> floorShade(
                        row = row,
                        height = height,
                        floor = floor,
                    )
                }
            }
        }

        renderEnemies(
            canvas = canvas,
            depthBuffer = depthBuffer,
            state = state,
            centerLine = centerLine,
        )
        drawMinimap(canvas = canvas, state = state)
        drawCrosshair(canvas = canvas)
        drawWeapon(canvas = canvas, state = state)
        return canvas.toDisplayString()
    }

    private fun castRay(
        state: DoomGameState,
        rayAngle: Double,
    ): RayResult {
        val eyeX = cos(rayAngle)
        val eyeY = sin(rayAngle)
        var distance = 0.0
        var boundary = false

        while (distance < ViewDepth) {
            distance += RayStep
            val sampleX = state.playerX + eyeX * distance
            val sampleY = state.playerY + eyeY * distance
            val mapX = floor(sampleX).toInt()
            val mapY = floor(sampleY).toInt()

            if (state.tileAt(x = mapX, y = mapY) == '#') {
                boundary = touchesCorner(
                    playerX = state.playerX,
                    playerY = state.playerY,
                    mapX = mapX,
                    mapY = mapY,
                    eyeX = eyeX,
                    eyeY = eyeY,
                )
                break
            }
        }

        val correctedDistance = (distance * cos(rayAngle - state.playerAngle))
            .coerceIn(minimumValue = 0.001, maximumValue = ViewDepth)
        return RayResult(
            correctedDistance = correctedDistance,
            boundary = boundary,
        )
    }

    private fun touchesCorner(
        playerX: Double,
        playerY: Double,
        mapX: Int,
        mapY: Int,
        eyeX: Double,
        eyeY: Double,
    ): Boolean {
        val cornerChecks = mutableListOf<Pair<Double, Double>>()

        for (offsetY in 0..1) {
            for (offsetX in 0..1) {
                val vectorX = mapX + offsetX - playerX
                val vectorY = mapY + offsetY - playerY
                val vectorLength = hypot(vectorX, vectorY)
                if (vectorLength > 0.0) {
                    val dotProduct = (eyeX * vectorX / vectorLength) + (eyeY * vectorY / vectorLength)
                    cornerChecks += vectorLength to dotProduct
                }
            }
        }

        val closestCorners = cornerChecks.sortedBy { (distance, _) -> distance }.take(n = 2)
        return closestCorners.any { (_, dotProduct) ->
            acos(dotProduct.coerceIn(minimumValue = -1.0, maximumValue = 1.0)) < 0.04
        }
    }

    private fun renderEnemies(
        canvas: AsciiCanvas,
        depthBuffer: DoubleArray,
        state: DoomGameState,
        centerLine: Int,
    ) {
        val enemiesByDistance = state.activeEnemies
            .sortedByDescending { enemy -> hypot(enemy.x - state.playerX, enemy.y - state.playerY) }

        for (enemy in enemiesByDistance) {
            val dx = enemy.x - state.playerX
            val dy = enemy.y - state.playerY
            val rawDistance = hypot(dx, dy)
            val angleDifference = normalizeAngle(angle = atan2(dy, dx) - state.playerAngle)

            if (abs(angleDifference) > FieldOfView / 2.0 + 0.25 || rawDistance <= 0.3) {
                continue
            }

            val correctedDistance = (rawDistance * cos(angleDifference)).coerceAtLeast(minimumValue = 0.05)
            val spriteHeight = (canvas.height / correctedDistance * SpriteScale)
                .roundToInt()
                .coerceAtLeast(minimumValue = 2)
            val spriteWidth = (spriteHeight * 0.75).roundToInt().coerceAtLeast(minimumValue = 2)
            val spriteScreenX = ((angleDifference + FieldOfView / 2.0) / FieldOfView * canvas.width).roundToInt()
            val top = centerLine - spriteHeight / 2 + (sin(enemy.animationTime * 6.0) * 0.8).roundToInt()
            val left = spriteScreenX - spriteWidth / 2

            for (spriteX in 0 until spriteWidth) {
                val screenX = left + spriteX
                if (screenX !in 0 until canvas.width || correctedDistance >= depthBuffer[screenX]) {
                    continue
                }

                val u = spriteX.toDouble() / spriteWidth

                for (spriteY in 0 until spriteHeight) {
                    val screenY = top + spriteY
                    if (screenY !in 0 until canvas.height) {
                        continue
                    }

                    val v = spriteY.toDouble() / spriteHeight
                    val spriteChar = sampleSprite(art = impSprite, u = u, v = v)
                    if (spriteChar == ' ') {
                        continue
                    }

                    canvas[screenX, screenY] = when {
                        correctedDistance < 2.2 -> spriteChar
                        correctedDistance < 4.5 -> spriteChar.lowercaseChar()
                        else -> '.'
                    }
                }
            }
        }
    }

    private fun drawMinimap(
        canvas: AsciiCanvas,
        state: DoomGameState,
    ) {
        if (canvas.width < state.mapWidth + 4 || canvas.height < state.mapHeight + 2) {
            return
        }

        canvas.drawText(x = 1, y = 0, text = "MAP")

        for (mapY in 0 until state.mapHeight) {
            for (mapX in 0 until state.mapWidth) {
                canvas[mapX + 1, mapY + 1] = if (state.tileAt(x = mapX, y = mapY) == '#') '#' else '.'
            }
        }

        for (enemy in state.activeEnemies) {
            val enemyX = enemy.x.toInt().coerceIn(minimumValue = 0, maximumValue = state.mapWidth - 1)
            val enemyY = enemy.y.toInt().coerceIn(minimumValue = 0, maximumValue = state.mapHeight - 1)
            canvas[enemyX + 1, enemyY + 1] = 'M'
        }

        canvas[state.playerX.toInt() + 1, state.playerY.toInt() + 1] = facingGlyph(angle = state.playerAngle)
    }

    private fun drawCrosshair(canvas: AsciiCanvas) {
        val centerX = canvas.width / 2
        val centerY = canvas.height / 2
        canvas[centerX, centerY] = CrosshairChar
    }

    private fun drawWeapon(
        canvas: AsciiCanvas,
        state: DoomGameState,
    ) {
        val art = if (state.muzzleFlashTime > 0.0) muzzleSprite else shotgunSprite
        val artWidth = art.maxOf { line -> line.length }
        val bobX = sin(state.motionBob * 2.0).roundToInt()
        val bobY = abs(sin(state.motionBob)).roundToInt()
        val startX = ((canvas.width - artWidth) / 2 + bobX).coerceAtLeast(minimumValue = 0)
        val startY = (canvas.height - art.size - 1 + bobY).coerceAtLeast(minimumValue = 0)

        for ((index, row) in art.withIndex()) {
            canvas.drawText(x = startX, y = startY + index, text = row)
        }
    }

    private fun skyShade(
        row: Int,
        column: Int,
        horizon: Int,
        elapsedSeconds: Double,
    ): Char {
        if (row < horizon / 2 && ((column * 19 + row * 23 + (elapsedSeconds * 10.0).toInt()) % 97 == 0)) {
            return '*'
        }

        val density = if (horizon <= 0) 0.0 else row.toDouble() / horizon
        return when {
            density < 0.18 -> ' '
            density < 0.42 -> '.'
            density < 0.72 -> '-'
            else -> '='
        }
    }

    private fun wallShade(
        correctedDistance: Double,
        boundary: Boolean,
    ): Char {
        if (boundary) {
            return '|'
        }

        val shadeIndex = ((correctedDistance / ViewDepth) * wallRamp.lastIndex)
            .roundToInt()
            .coerceIn(minimumValue = 0, maximumValue = wallRamp.lastIndex)
        return wallRamp[shadeIndex]
    }

    private fun floorShade(
        row: Int,
        height: Int,
        floor: Int,
    ): Char {
        val denominator = (height - floor).coerceAtLeast(minimumValue = 1)
        val distanceFactor = (row - floor).toDouble() / denominator
        val shadeIndex = ((1.0 - distanceFactor) * (floorRamp.lastIndex))
            .roundToInt()
            .coerceIn(minimumValue = 0, maximumValue = floorRamp.lastIndex)
        return floorRamp[shadeIndex]
    }

    private fun sampleSprite(
        art: List<String>,
        u: Double,
        v: Double,
    ): Char {
        val spriteWidth = art.maxOf { line -> line.length }
        val sampleX = (u * spriteWidth)
            .toInt()
            .coerceIn(minimumValue = 0, maximumValue = spriteWidth - 1)
        val sampleY = (v * art.size)
            .toInt()
            .coerceIn(minimumValue = 0, maximumValue = art.size - 1)
        val row = art[sampleY]
        return if (sampleX >= row.length) ' ' else row[sampleX]
    }
}

private data class RayResult(
    val correctedDistance: Double,
    val boundary: Boolean,
)

private class AsciiCanvas(
    val width: Int,
    val height: Int,
    fill: Char,
) {

    private val pixels = CharArray(size = width * height) { fill }

    operator fun set(
        x: Int,
        y: Int,
        value: Char,
    ) {
        if (x !in 0 until width || y !in 0 until height) {
            return
        }
        pixels[x + y * width] = value
    }

    fun drawText(
        x: Int,
        y: Int,
        text: String,
    ) {
        if (y !in 0 until height) {
            return
        }

        for ((offset, char) in text.withIndex()) {
            val drawX = x + offset
            if (drawX in 0 until width && char != ' ') {
                this[drawX, y] = char
            }
        }
    }

    fun toDisplayString(): String {
        return buildString {
            for (row in 0 until height) {
                append(pixels, row * width, width)
                if (row != height - 1) {
                    append('\n')
                }
            }
        }
    }
}

private fun renderTitleScreen(
    width: Int,
    height: Int,
    blinkOn: Boolean,
): String {
    val lines = listOf(
        " _  ____      _     _____                         _             _ ",
        "| |/ / /___ _| |__ |__  /___ _ __ _ __ ___  _ __(_) __ _  __ _| |",
        "| ' / / __` | '_ \\  / // _ \\ '__| '_ ` _ \\| '__| |/ _` |/ _` | |",
        "| . \\ \\__ \\ | |_) |/ /|  __/ |  | | | | | | |  | | (_| | (_| | |",
        "|_|\\_\\_|___/_|_.__/____\\___|_|  |_| |_| |_|_|  |_|\\__,_|\\__,_|_|",
        "",
        "Drop into a tiny Doom-like ASCII arena rendered with terminal raycasting.",
        "",
        "Controls",
        "W/S move  A/D strafe  Q/E or arrows turn",
        "Space fire  Esc returns to the title screen",
        "",
        if (blinkOn) "Press Enter to deploy. Press Q to quit." else "",
    )

    return centerScreen(lines = lines, width = width, height = height)
}

private fun renderGameOverScreen(
    width: Int,
    height: Int,
    kills: Int,
    totalEnemies: Int,
    blinkOn: Boolean,
): String {
    val lines = listOf(
        "   _____                         ____                 ",
        "  / ____|                       / __ \\                ",
        " | |  __  __ _ _ __ ___   ___  | |  | |_   _____ _ __ ",
        " | | |_ |/ _` | '_ ` _ \\ / _ \\ | |  | \\ \\ / / _ \\ '__|",
        " | |__| | (_| | | | | | |  __/ | |__| |\\ V /  __/ |   ",
        "  \\_____|\\__,_|_| |_| |_|\\___|  \\____/  \\_/ \\___|_|   ",
        "",
        "Hostiles eliminated: $kills / $totalEnemies",
        if (blinkOn) "Press Enter to run the sector again. Press M for title. Q quits." else "",
    )

    return centerScreen(lines = lines, width = width, height = height)
}

private fun centerScreen(
    lines: List<String>,
    width: Int,
    height: Int,
): String {
    val topPadding = ((height - lines.size) / 2).coerceAtLeast(minimumValue = 0)
    val bottomPadding = (height - topPadding - lines.size).coerceAtLeast(minimumValue = 0)

    return buildString {
        repeat(times = topPadding) {
            append(" ".repeat(width))
            append('\n')
        }

        lines.forEachIndexed { index, line ->
            append(centerLine(text = line, width = width))
            if (index != lines.lastIndex || bottomPadding > 0) {
                append('\n')
            }
        }

        repeat(times = bottomPadding) { index ->
            append(" ".repeat(width))
            if (index != bottomPadding - 1) {
                append('\n')
            }
        }
    }
}

private fun fitLine(
    text: String,
    width: Int,
): String {
    if (width <= 0) {
        return ""
    }

    return if (text.length <= width) {
        text.padEnd(length = width, padChar = ' ')
    } else {
        text.take(n = width)
    }
}

private fun centerLine(
    text: String,
    width: Int,
): String {
    if (width <= 0) {
        return ""
    }

    val trimmed = if (text.length <= width) text else text.take(n = width)
    val leftPadding = ((width - trimmed.length) / 2).coerceAtLeast(minimumValue = 0)
    return buildString(capacity = width) {
        append(" ".repeat(n = leftPadding))
        append(trimmed)
        append(" ".repeat(n = (width - leftPadding - trimmed.length).coerceAtLeast(minimumValue = 0)))
    }
}

private fun facingGlyph(angle: Double): Char {
    val normalized = wrapPositive(angle = angle)
    return when {
        normalized < PI / 4.0 || normalized >= PI * 7.0 / 4.0 -> '>'
        normalized < PI * 3.0 / 4.0 -> 'v'
        normalized < PI * 5.0 / 4.0 -> '<'
        else -> '^'
    }
}

private fun normalizeAngle(angle: Double): Double {
    var normalized = angle
    while (normalized <= -PI) {
        normalized += PI * 2.0
    }
    while (normalized > PI) {
        normalized -= PI * 2.0
    }
    return normalized
}

private fun wrapPositive(angle: Double): Double {
    var wrapped = angle
    while (wrapped < 0.0) {
        wrapped += PI * 2.0
    }
    while (wrapped >= PI * 2.0) {
        wrapped -= PI * 2.0
    }
    return wrapped
}
