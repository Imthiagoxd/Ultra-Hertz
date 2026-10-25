package com.devthiago.ultrahertz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// PALETA E FORMAS (altere aqui para mudar o visual)
// ============================================================
object Pal {
    /** Definido pelo AppShell conforme o tema escolhido. */
    @Volatile
    var dark: Boolean = true

    private fun c(d: Long, l: Long): Color = Color(if (dark) d else l)

    val bg: Color get() = c(0xFF0B0C10, 0xFFF3F1F9)
    val surface1: Color get() = c(0xFF14161C, 0xFFFFFFFF)
    val surface2: Color get() = c(0xFF1B1E26, 0xFFE9E5F6)
    val outline: Color get() = c(0xFF2A2E38, 0xFFDAD5EC)
    val accent: Color get() = c(0xFF9B8CFF, 0xFF6A52E0)       // violeta
    val accentDim: Color get() = c(0xFF2A2547, 0xFFE0DAFA)
    val onAccent: Color get() = c(0xFF14102B, 0xFFFFFFFF)
    val warm: Color get() = c(0xFFFFB454, 0xFFC26E00)         // destaque quente
    val textHi: Color get() = c(0xFFECEEF4, 0xFF17161F)
    val textLo: Color get() = c(0xFF8D93A3, 0xFF6A677E)
    val danger: Color get() = c(0xFFFF7A7A, 0xFFD13B3B)
    val cornerCard = 22.dp
    val cornerChip = 12.dp
}

/** Cartão com borda fina. */
@Composable
fun Panel(bg: Color = Pal.surface1, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(Pal.cornerCard)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, Pal.outline, shape)
    ) { content() }
}

@Composable
fun Caption(text: String) {
    Text(
        text.uppercase(),
        color = Pal.textLo,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun ScreenTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = Pal.textHi, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Pal.textLo, fontSize = 14.sp)
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Pal.textHi, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = Pal.textLo, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Pal.onAccent,
                checkedTrackColor = Pal.accent,
                uncheckedThumbColor = Pal.textLo,
                uncheckedTrackColor = Pal.surface2
            )
        )
    }
}

/** Fileira de opções que rola na horizontal. */
@Composable
fun ChipRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(label, color = Pal.textLo, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { i, text ->
                val sel = i == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(Pal.cornerChip))
                        .background(if (sel) Pal.accent else Pal.surface2)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text,
                        color = if (sel) Pal.onAccent else Pal.textHi,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun HzChips(label: String, value: Float, rates: List<Float>, onChange: (Float) -> Unit) {
    val idx = rates.indexOfFirst { it == value }
    ChipRow("$label: ${value.toInt()} Hz", rates.map { it.toInt().toString() }, idx) { onChange(rates[it]) }
}

/** Seletor segmentado (opcionalmente com legenda embaixo de cada opção). */
@Composable
fun Segmented(options: List<String>, captions: List<String>?, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.surface1)
            .border(1.dp, Pal.outline, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, text ->
            val sel = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (sel) Pal.accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text,
                    color = if (sel) Pal.onAccent else Pal.textHi,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (captions != null && i < captions.size) {
                    Text(
                        captions[i],
                        color = if (sel) Pal.onAccent else Pal.textLo,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/** Cartão de destaque com gradiente e a barra da faixa de Hz. */
@Composable
fun HeroCard(label: String, big: String, sub: String, min: Float, max: Float, panelMax: Float) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Pal.accentDim, Pal.surface2, Pal.surface1)))
            .border(1.dp, Pal.outline, shape)
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Text(label, color = Pal.warm, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(big, color = Pal.textHi, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = Pal.textLo, fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.height(18.dp))
            RangeBar(min, max, panelMax)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0", color = Pal.textLo, fontSize = 11.sp)
                Text("${panelMax.toInt()} Hz", color = Pal.textLo, fontSize = 11.sp)
            }
        }
    }
}

/** Trilho com o trecho da faixa de Hz preenchido. */
@Composable
fun RangeBar(min: Float, max: Float, panelMax: Float) {
    val top = if (panelMax <= 0f) 120f else panelMax
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val r = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(Pal.outline, size = size, cornerRadius = r)
        val x0 = size.width * (min / top).coerceIn(0f, 1f)
        val x1 = size.width * (max / top).coerceIn(0f, 1f)
        drawRoundRect(
            Pal.accent,
            topLeft = Offset(x0.coerceAtMost(size.width - size.height), 0f),
            size = Size(maxOf(x1 - x0, size.height), size.height),
            cornerRadius = r
        )
    }
}

/** Barra de baixo com 4 abas. */
@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    val labels = listOf("Taxa", "Situações", "Apps", "Extras", "Ajustes")
    Row(
        Modifier
            .fillMaxWidth()
            .background(Pal.surface1)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val sel = i == tab
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) Pal.accentDim else Color.Transparent)
                    .clickable { onTab(i) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .width(if (sel) 22.dp else 6.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (sel) Pal.accent else Pal.outline)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    color = if (sel) Pal.accent else Pal.textLo,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/** Linha clicável com seta (abre outra aba ou ação). */
@Composable
fun ActionRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Pal.textHi, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = Pal.textLo, fontSize = 12.sp)
        }
        Text("›", color = Pal.textLo, fontSize = 24.sp)
    }
}

/** Linha fina de separação dentro de um cartão. */
@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pal.outline))
}
