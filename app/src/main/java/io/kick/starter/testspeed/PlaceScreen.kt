package io.kick.starter.testspeed

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaceScreen() {

    val coroutineScope = rememberCoroutineScope()
    val showDialog = remember { mutableStateOf(false) }

    if (showDialog.value)
        GameOver(onClose = {
            showDialog.value = false
        })
    val matrix = remember {

        val matrix = Array(8) { i ->
            Array(13) { j ->
                BoxData(
                    isPitFall = false, color = Animatable(Color.White),
                    adjPits = "", column = -1, row = -1
                )
            }
        }

        for (column in 0..7) {
            for (row in 0..12) {
                val randomValue = Random.nextInt(0, 10)
                val isPitfall = randomValue > 8
                matrix[column][row] = BoxData(
                    isPitFall = isPitfall,
                    color = if (isPitfall) Animatable(Color.Red) else Animatable(Color.White),
                    column = column,
                    row = row,
                )
            }
        }

        mutableStateOf(matrix)
    }

    fun updateAdjPits(column: Int, row: Int, totalPits: Int) {
        matrix.value = matrix.value.map { it.clone() }.toTypedArray().apply {
            this[column][row] = this[column][row].copy(adjPits = totalPits.toString())
        }
    }

    fun recalculateAdjacentBoxes(boxData: BoxData) {
        val row = boxData.row
        val column = boxData.column
        val c = maxOf(0, column - 1)
        val c2 = minOf(column + 1, 7)

        val r = maxOf(0, row - 1)
        val r2 = minOf(row + 1, 12)

        var totalPits = 0
        for (newColumn in c..c2) {
            for (newRow in r..r2) {
                totalPits += if (matrix.value[newColumn][newRow].isPitFall) 1 else 0
            }
        }
        updateAdjPits(column, row, totalPits)
    }

    fun getAdjacentBoxesGreen(boxData: BoxData): MutableList<BoxData> {
        val list = mutableListOf<BoxData>()
        val row = boxData.row
        val column = boxData.column
        val c = maxOf(0, column - 1)
        val c2 = minOf(column + 1, 7)

        val r = maxOf(0, row - 1)
        val r2 = minOf(row + 1, 12)

        var currentBox: BoxData

        //first row
        for (newColumn in c..c2) {
            currentBox = matrix.value[newColumn][r]
            if (currentBox.isNotOccupied()) {
                list.add(currentBox)
            }
        }
        //first column
        for (newRow in r + 1..r2) {
            currentBox = matrix.value[c2][newRow]
            if (currentBox.isNotOccupied()) {
                list.add(currentBox)
            }
        }
        //reverse row
        for (newColumn in c2 - 1 downTo c) {
            currentBox = matrix.value[newColumn][r2]
            if (currentBox.isNotOccupied()) {
                list.add(currentBox)
            }
        }
        //reverse column
        currentBox = matrix.value[c][r2 - 1]
        if (currentBox.isNotOccupied()) {
            list.add(currentBox)
        }
        //item itself
        list.add(boxData)
        return list
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("speed") },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Cyan)

            )
        },
    ) { paddingValues ->
        NumberGrid(
            matrix.value.toCustomList(),
            modifier = Modifier.padding(paddingValues),
            onLongClick = { boxData ->
                recalculateAdjacentBoxes(boxData)
            },
            onClick = { boxData ->
                if (boxData.isPitFall)
                    showDialog.value = true

                if (boxData.isNotOccupied()){
                    val toBeGreen = getAdjacentBoxesGreen(boxData)
                    coroutineScope.launch {
                        toBeGreen.forEach{
                            it.color.animateTo(Color.Green,
                                animationSpec = tween(300))
                        }
                    }
                }

            }
        )
    }
}

private fun BoxData.isNotOccupied(): Boolean =
    this.adjPits == ""
            && !this.isPitFall
            && this.color.value != Color.Green


fun Array<Array<BoxData>>.toCustomList(): List<BoxData> {
    val list = mutableListOf<BoxData>()
    for (row in 0..12) {
        for (colum in 0..7) {
            list.add(this[colum][row])
        }
    }
    return list
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberGrid(
    list: List<BoxData>,
    modifier: Modifier,
    onClick: (BoxData) -> Unit,
    onLongClick: (BoxData) -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(48.dp),
    ) {
        items(list) { item ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .height(48.dp)
                    .border(border = BorderStroke(1.dp, Color.Black))
                    .background(item.color.value)
                    .combinedClickable(
                        onLongClick = {
                            onLongClick(item)
                        },
                        onClick = {
                            onClick(item)
                        })
            ) {
                Text(
                    item.adjPits,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
fun GameOver(onClose: () -> Unit) {
    AlertDialog(
        icon = null,
        title = {
            Text(text = "Game Over")
        },
        onDismissRequest = {
            onClose()
        },
        confirmButton = {
            Button(
                onClick = {
                    onClose()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClose()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}

fun randomColor(alpha: Int = 255) = Color(
    Random.nextInt(256),
    Random.nextInt(256),
    Random.nextInt(256),
    alpha = alpha
)

data class BoxData(
    var isPitFall: Boolean = false,
    var color: Animatable<Color, AnimationVector4D> = Animatable(Color.White),
    var adjPits: String = "",
    var column: Int,
    var row: Int
)

@Preview
@Composable
fun PlacePreview() {
    PlaceScreen()
}