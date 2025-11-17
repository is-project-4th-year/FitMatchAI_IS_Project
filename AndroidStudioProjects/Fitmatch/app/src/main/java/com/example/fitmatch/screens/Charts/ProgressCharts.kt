
package com.example.fitmatch.screens.Charts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun LineChartMP(values: List<Float>, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val entries = values.mapIndexed { x, y -> Entry(x.toFloat(), y) }
            val ds = LineDataSet(entries, "").apply {
                setDrawCircles(false); setDrawValues(false); lineWidth = 2f
            }
            chart.data = LineData(ds)
            chart.invalidate()
        }
    )
}

@Composable
fun BarChartMP(values: List<Float>, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
            }
        },
        update = { chart ->
            val entries = values.mapIndexed { x, y -> BarEntry(x.toFloat(), y) }
            val ds = BarDataSet(entries, "").apply {
                setColors(*ColorTemplate.MATERIAL_COLORS)
            }
            chart.data = BarData(ds).apply { setDrawValues(false) }
            chart.invalidate()
        }
    )
}

@Composable
fun PieChartMP(slices: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PieChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                isDrawHoleEnabled = false
            }
        },
        update = { chart ->
            val entries = slices.map { PieEntry(it.second, it.first) }
            val ds = PieDataSet(entries, "").apply {
                setColors(*ColorTemplate.MATERIAL_COLORS)
                setDrawValues(false)
            }
            chart.data = PieData(ds)
            chart.invalidate()
        }
    )
}