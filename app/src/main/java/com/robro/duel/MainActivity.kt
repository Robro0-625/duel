package com.robro.duel
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.robro.Duel.R
import kotlin.math.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
const val speedCoefficient = 3f
const val imageHeight = 20
const val imageWidth = 20
fun Float.toRadians(): Float = this / 180f * PI.toFloat()
fun Float.toDegrees(): Float = this * 180f / PI.toFloat()
//var p1Color by mutableStateOf(Color.Cyan)
//var p2Color by mutableStateOf(Color.Yellow)
const val collisionBoxesShown = false
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val screenDensity = LocalConfiguration.current.densityDpi / 160f
            val screenHeight =
                LocalConfiguration.current.screenHeightDp.toFloat() * screenDensity
            val screenWidth =
                LocalConfiguration.current.screenWidthDp.toFloat() * screenDensity
            val p1TrackpadStruct by remember {
                mutableStateOf(
                    Trackpad(
                        Pair(screenWidth,screenHeight),
                        screenDensity,
                        (screenWidth) * 1 / 2 - imageWidth * (screenDensity) / 2,
                        (screenHeight) * 1 / 5 - imageHeight * (screenDensity) / 2,
                        90f,
                        Color.Cyan
                    )
                )
            }
            val p2TrackpadStruct by remember {
                mutableStateOf(
                    Trackpad(
                        Pair(screenWidth,screenHeight),
                        screenDensity,
                        (screenWidth) * 1 / 2 - imageWidth * screenDensity / 2,
                        (screenHeight) * 4 / 5 - imageHeight * screenDensity / 2,
                        270f,
                        Color.Yellow
                    )
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                //Holds main Screen
                Box(modifier = Modifier.fillMaxSize()) {
                    Log.e("Width", resources.getInteger(R.integer.imageWidth).toString())
                    Column(modifier = Modifier.fillMaxSize()) {
                        p1TrackpadStruct.Pad(
                            modifier = Modifier.weight(1f, true),
                            color = p1TrackpadStruct.color()
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(0.5f)
                                .background(
                                    Brush.verticalGradient(
                                        arrayListOf(p1TrackpadStruct.color(), p2TrackpadStruct.color())
                                    )
                                )
                        ){
                           if ((p1TrackpadStruct.state == TState.Dead ) || (p2TrackpadStruct.state == TState.Dead)){
                                Button(
                                    onClick = {
                                        p1TrackpadStruct.reset()
                                        p2TrackpadStruct.reset()
                                    },
                                    Modifier.align(Alignment.Center)
                                ) {
                                    Text(text = "Reset")
                                }
                           }
                        }
                        p2TrackpadStruct.Pad(
                            modifier = Modifier.weight(1f, true),
                            color = p2TrackpadStruct.color()
                        )
                    }
                    p1TrackpadStruct.Cursor(image = R.drawable.cyan)
                    p2TrackpadStruct.Cursor(image = R.drawable.yellow)
                }
                Canvas(modifier = Modifier.fillMaxSize().align(Alignment.Center)) {
                    val hurtcol = Color(0, 0, 255, if (collisionBoxesShown) { 127 } else { 0 })
                    val hitcol = Color(255, 0, 0, if (collisionBoxesShown) { 127 } else { 0 })
                    drawCircle(hurtcol, 25f, Offset(p1TrackpadStruct.hurtCoords.first, p1TrackpadStruct.hurtCoords.second))
                    drawCircle(hitcol, 15f, Offset(p1TrackpadStruct.hitCoords.first, p1TrackpadStruct.hitCoords.second))
                    drawCircle(hurtcol, 25f, Offset(p2TrackpadStruct.hurtCoords.first, p2TrackpadStruct.hurtCoords.second))
                    drawCircle(hitcol, 15f, Offset(p2TrackpadStruct.hitCoords.first, p2TrackpadStruct.hitCoords.second))
                }
            }
            winCheck(p1TrackpadStruct, p2TrackpadStruct)
        }
    }
}
fun winCheck(p1:Trackpad, p2:Trackpad) {
    val state1 = p1.updateState(p2)
    Log.e("STATE P1",state1.toString())
    val state2 = p2.updateState(p1)
    Log.e("STATE P2",state2.toString())
}

fun Pair<Float,Float>.distanceTo(inp:Pair<Float,Float>) =
    hypot(
        this.first-inp.first,
        this.second-inp.second
    )
enum class TState {
    Vulnerable,
    Dead,
    Immune,
    Cooldown
}
class Trackpad(
        private val screenDimensions:Pair<Float,Float>,
        private val screenDensity: Float,
        private val startx: Float = 0f,
        private val starty: Float = 0f,
        private val startSpin: Float = 90f,
        val startcolor:Color = Color.White
    ) {
    var state by mutableStateOf(TState.Vulnerable)
    private var offset by mutableStateOf(Pair(startx, starty))
    private var spinOffset by mutableFloatStateOf(startSpin)
    private var oldTime by mutableLongStateOf(System.currentTimeMillis())
    fun color(): Color {
        return if (state == TState.Dead){Color.White} else {startcolor}
    }
    var hitCoords by mutableStateOf(Pair(Float.NaN, 0f))
        private set
    private fun generateHitCoords(): Pair<Float, Float> = Pair(
        (offset.first + (imageWidth / 2 + 7 * cos(spinOffset.toRadians())) * screenDensity),
        (offset.second + (imageHeight / 2 + 7 * sin(spinOffset.toRadians())) * screenDensity)
    )
    var hurtCoords by mutableStateOf(Pair(0f, 0f))
        private set
    private fun generateHurtCoords(): Pair<Float, Float> = Pair(
        (offset.first + (imageHeight / 2) * screenDensity) - 15 * cos((spinOffset).toRadians()),
        (offset.second + (imageHeight / 2) * screenDensity) - 15 * sin((spinOffset).toRadians())
    )

    private fun updateOffset(deltaX: Float, deltaY: Float) {
        offset = Pair(
            max(
                -imageWidth / 2 * screenDensity,
                min(
                    screenDimensions.first - imageWidth / 2 * screenDensity,
                    offset.first + deltaX * speedCoefficient
                )
            ),
            max(
                -imageHeight / 2f * screenDensity,
                min(
                    screenDimensions.second - imageHeight / 2 * screenDensity,
                    offset.second + deltaY * speedCoefficient
                )
            )
        )
        val weightCoef = 20
        val spinVec: Pair<Float, Float> =
            Pair(cos(spinOffset.toRadians()), sin(spinOffset.toRadians()))
        val dragVec: Pair<Float, Float> = Pair(deltaX, deltaY)
        val deltaR = (hypot(deltaX, deltaY))
        val dragUnit = Pair(1 * dragVec.first / deltaR, 1 * dragVec.second / deltaR)
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - oldTime
        val deltaWeight = deltaR * max(deltaTime.toFloat(), 30.0F) / 15
        oldTime = currentTime
        spinOffset = (atan2(
            //dragUnit.second,dragUnit.first
            (spinVec.second * weightCoef + dragUnit.second * deltaWeight),
            (spinVec.first * weightCoef + dragUnit.first * deltaWeight)
        )).toDegrees()
        Log.e("Angle Draw ", dragUnit.toString())
    }

    init {
        hitCoords = generateHitCoords()
        hurtCoords = generateHurtCoords()
    }

    @Composable
    fun Pad(modifier: Modifier = Modifier, color: Color = Color.White) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        updateOffset(dragAmount.x, dragAmount.y)
                        hitCoords = generateHitCoords()
                        hurtCoords = generateHurtCoords()
                    }
                },
            color = color
        ) {}
    }

    @Composable
    fun Cursor(modifier: Modifier = Modifier, image: Int = R.drawable.white) {
        val paintImage = painterResource(
            if (state != TState.Dead) {
                image
            } else {
                R.drawable.white
            }
        )
        Box(modifier = modifier
            .offset {
                IntOffset(
                    (offset.first.roundToInt()),
                    (offset.second.roundToInt())
                )
            }
            .rotate(spinOffset + 90)
        ) {
            Image(
                painter = paintImage,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(imageHeight.dp, imageHeight.dp)
            )
        }
    }
    @Suppress("LiftReturnOrAssignment")
    fun updateState(other: Trackpad): TState {
        if (other.state != TState.Dead) {
            when (state) {
                TState.Vulnerable -> {
                    if (hitCoords.distanceTo(other.hitCoords) < 30) {
                        state = TState.Immune
                    } else if (hurtCoords.distanceTo(other.hitCoords) < 40) {
                        state = TState.Dead
                    }
                }

                TState.Immune -> {
                    if (hurtCoords.distanceTo(other.hitCoords) > 30 && hitCoords.distanceTo(other.hitCoords) > 30) {
                        state = TState.Cooldown
                    }
                }

                TState.Cooldown -> {
                    if (hurtCoords.distanceTo(other.hitCoords) < 30 || hitCoords.distanceTo(other.hitCoords) < 30) {
                        state = TState.Immune
                    } else {
                        state = TState.Vulnerable
                    }
                }

                TState.Dead -> {}
            }
        }
        return state
    }
    fun reset(){
        state = TState.Vulnerable
        offset = Pair(startx,starty)
        spinOffset = startSpin
        hitCoords = generateHitCoords()
        hurtCoords = generateHurtCoords()
    }
}