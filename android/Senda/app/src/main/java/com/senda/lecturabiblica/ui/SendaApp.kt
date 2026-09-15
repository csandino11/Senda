@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.senda.lecturabiblica.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senda.lecturabiblica.AppUiState
import com.senda.lecturabiblica.AppViewModel
import com.senda.lecturabiblica.data.BibleData
import com.senda.lecturabiblica.domain.youVersionUrl
import com.senda.lecturabiblica.model.DayPlan
import com.senda.lecturabiblica.model.DayStatus
import com.senda.lecturabiblica.model.ProgressStats
import com.senda.lecturabiblica.model.Reading
import com.senda.lecturabiblica.model.ReadingPlan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val spanish = Locale.forLanguageTag("es-ES")
private val longDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", spanish)
private val shortDate = DateTimeFormatter.ofPattern("d MMM", spanish)

private enum class Destination(val label: String, val glyph: String) {
    DAY("Día", "D"), WEEK("Semana", "S"), MONTH("Mes", "M"), SETTINGS("Ajustes", "A"), ADVANCED("Avanzado", "+")
}

@Composable
fun SendaApp(state: AppUiState, viewModel: AppViewModel) {
    when {
        state.loading -> LoadingScreen()
        state.plan == null -> OnboardingScreen(state, viewModel)
        else -> Planner(state.plan, state, viewModel)
    }
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("Entendido") } },
            title = { Text("No se pudo completar") }, text = { Text(error) },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            CircularProgressIndicator()
            Text("Preparando tu espacio de lectura", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun OnboardingScreen(state: AppUiState, viewModel: AppViewModel) {
    var theme by rememberSaveable { mutableStateOf("faith") }
    var extra by rememberSaveable { mutableStateOf(true) }
    Scaffold { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                Text("SENDA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, letterSpacing = 3.sp)
                Spacer(Modifier.height(22.dp))
                Text("Un año en\nla Palabra.", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Un recorrido personal que conecta la Biblia entera, día a día, y guarda tu avance solamente en este dispositivo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge,
                )
            }
            item { ThemeSelector(theme, onSelect = { theme = it }) }
            item { DeuterocanonSwitch(extra, onChange = { extra = it }) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Tu recorrido incluirá", style = MaterialTheme.typography.titleMedium)
                        Text("• Un Evangelio todos los días\n• Salmos cada semestre\n• Proverbios cada trimestre\n• 2–4 lecturas en fin de semana; hasta 5 entre semana")
                    }
                }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !state.generating,
                    onClick = { viewModel.generate(theme, extra, false) },
                ) {
                    if (state.generating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Crear mi plan de ${LocalDate.now().year}")
                }
            }
        }
    }
}

@Composable
private fun Planner(plan: ReadingPlan, state: AppUiState, viewModel: AppViewModel) {
    var destinationName by rememberSaveable { mutableStateOf(Destination.DAY.name) }
    val destination = Destination.valueOf(destinationName)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(Modifier.fillMaxHeight().statusBarsPadding()) {
                    Spacer(Modifier.height(20.dp))
                    Destination.entries.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { destinationName = item.name },
                            icon = { DestinationGlyph(item) }, label = { Text(item.label) },
                        )
                    }
                }
                PlannerContent(destination, plan, state, viewModel, Modifier.weight(1f))
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(Modifier.navigationBarsPadding()) {
                        Destination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destinationName = item.name },
                                icon = { DestinationGlyph(item) }, label = { Text(item.label, maxLines = 1) },
                            )
                        }
                    }
                },
            ) { insets -> PlannerContent(destination, plan, state, viewModel, Modifier.padding(insets)) }
        }
    }
}

@Composable
private fun DestinationGlyph(destination: Destination) {
    Surface(
        modifier = Modifier.size(25.dp), shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(destination.glyph, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PlannerContent(
    destination: Destination,
    plan: ReadingPlan,
    state: AppUiState,
    viewModel: AppViewModel,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Column {
                    Text("Senda", style = MaterialTheme.typography.titleLarge)
                    Text("${plan.year} · ${BibleData.themes.first { it.id == plan.theme }.name}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        AnimatedContent(destination, label = "pantalla") { screen ->
            when (screen) {
                Destination.DAY -> DayScreen(plan, state, viewModel)
                Destination.WEEK -> WeekScreen(plan, state, viewModel)
                Destination.MONTH -> MonthScreen(plan, viewModel)
                Destination.SETTINGS -> SettingsScreen(state, viewModel)
                Destination.ADVANCED -> AdvancedScreen(plan, state, viewModel)
            }
        }
    }
}

@Composable
private fun DayScreen(plan: ReadingPlan, state: AppUiState, viewModel: AppViewModel) {
    val initial = LocalDate.now().takeIf { it.year == plan.year } ?: plan.days.first().date
    var dateText by rememberSaveable(plan.id) { mutableStateOf(initial.toString()) }
    val date = LocalDate.parse(dateText)
    val day = plan.days.first { it.date == date }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize(),
    ) {
        item {
            DateHeading(
                date = date,
                onPrevious = { if (date > plan.days.first().date) dateText = date.minusDays(1).toString() },
                onNext = { if (date < plan.days.last().date) dateText = date.plusDays(1).toString() },
            )
        }
        item {
            val done = day.readings.indices.count { "$date#$it" in state.completed }
            ProgressSummary(done, day.readings.size)
        }
        items(day.readings.indices.toList(), key = { "$date-$it" }) { index ->
            ReadingCard(day, index, "$date#$index" in state.completed) { viewModel.toggleReading(date, index) }
        }
        item {
            Text(day.connection, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun DateHeading(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(date.format(longDate).replaceFirstChar { it.uppercase(spanish) }, style = MaterialTheme.typography.headlineMedium)
            Text("Día ${date.dayOfYear} de ${date.lengthOfYear()}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Día anterior") }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Día siguiente") }
    }
}

@Composable
private fun ProgressSummary(done: Int, total: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (done == total) "Lectura del día completa" else "$done de $total capítulos leídos", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
            }
            if (done == total) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun ReadingCard(day: DayPlan, index: Int, complete: Boolean, onToggle: () -> Unit) {
    val reading = day.readings[index]
    val context = androidx.compose.ui.platform.LocalContext.current
    var showVersions by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (complete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (complete) MaterialTheme.colorScheme.primary.copy(alpha = .35f) else MaterialTheme.colorScheme.outline.copy(alpha = .24f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(reading.label, style = MaterialTheme.typography.titleMedium)
                    Text(readingCaption(reading), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
                Checkbox(checked = complete, onCheckedChange = { onToggle() }, modifier = Modifier.semantics { contentDescription = "Marcar ${reading.label} como leída" })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { openBible(context, reading, if (reading.isDeuterocanonical) "TLAI" else "RVC") }) {
                    Text("Leer ahora…"); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(17.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showVersions = true }) { Icon(Icons.Default.MoreVert, "Elegir versión bíblica") }
            }
        }
    }
    if (showVersions) VersionDialog(reading, onDismiss = { showVersions = false }) { version ->
        showVersions = false; openBible(context, reading, version)
    }
}

private fun readingCaption(reading: Reading): String = when {
    reading.isGospel && reading.cycle == 2 -> "Evangelio · recorrido cronológico"
    reading.isGospel -> "Evangelio · recorrido temático"
    reading.book == "PSA" -> "Salmos · ciclo ${reading.cycle}"
    reading.book == "PRO" -> "Proverbios · ciclo ${reading.cycle}"
    reading.isDeuterocanonical -> "Lectura deuterocanónica"
    else -> "Capítulo completo"
}

@Composable
private fun VersionDialog(reading: Reading, onDismiss: () -> Unit, onChoose: (String) -> Unit) {
    val versions = if (reading.isDeuterocanonical) listOf("TLAI" to "Traducción al Lenguaje Actual Interconfesional") else listOf(
        "RVC" to "Reina Valera Contemporánea", "NTV" to "Nueva Traducción Viviente", "TLAI" to "Traducción al Lenguaje Actual Interconfesional",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿En qué versión deseas leer esta lectura?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(reading.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                versions.forEach { (id, name) ->
                    OutlinedButton(onClick = { onChoose(id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("$id  ·  $name", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun WeekScreen(plan: ReadingPlan, state: AppUiState, viewModel: AppViewModel) {
    val today = LocalDate.now().takeIf { it.year == plan.year } ?: plan.days.first().date
    var anchorText by rememberSaveable(plan.id) { mutableStateOf(today.toString()) }
    var selectedText by rememberSaveable(plan.id) { mutableStateOf(today.toString()) }
    val anchor = LocalDate.parse(anchorText)
    val monday = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    val available = (0L..6L).map { monday.plusDays(it) }.filter { it.year == plan.year }
    val selected = LocalDate.parse(selectedText).takeIf { it in available } ?: available.first()
    val day = plan.days.first { it.date == selected }
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Esta semana", style = MaterialTheme.typography.headlineMedium)
                    Text("${monday.format(shortDate)} — ${monday.plusDays(6).format(shortDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    val prior = anchor.minusWeeks(1); if (prior.year == plan.year || prior.plusDays(6).year == plan.year) anchorText = prior.toString()
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Semana anterior") }
                IconButton(onClick = {
                    val next = anchor.plusWeeks(1); if (next.year == plan.year || next.minusDays(6).year == plan.year) anchorText = next.toString()
                }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Semana siguiente") }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(available) { date ->
                    val status = viewModel.dayStatus(date)
                    val selectedDay = date == selected
                    Surface(
                        modifier = Modifier.width(66.dp).clickable { selectedText = date.toString() },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedDay) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selectedDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .25f)),
                    ) {
                        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, spanish).uppercase(spanish), style = MaterialTheme.typography.labelMedium)
                            Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleLarge)
                            StatusDot(status)
                        }
                    }
                }
            }
        }
        item { Text(selected.format(longDate).replaceFirstChar { it.uppercase(spanish) }, style = MaterialTheme.typography.titleLarge) }
        items(day.readings.indices.toList()) { index ->
            ReadingCard(day, index, "$selected#$index" in state.completed) { viewModel.toggleReading(selected, index) }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatusDot(status: DayStatus) {
    val color = statusColor(status)
    Box(Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(color))
}

@Composable
private fun MonthScreen(plan: ReadingPlan, viewModel: AppViewModel) {
    val current = LocalDate.now().takeIf { it.year == plan.year } ?: plan.days.first().date
    var month by rememberSaveable(plan.id) { mutableIntStateOf(current.monthValue) }
    var selectedText by rememberSaveable(plan.id) { mutableStateOf(current.toString()) }
    val yearMonth = YearMonth.of(plan.year, month)
    val selected = LocalDate.parse(selectedText).takeIf { YearMonth.from(it) == yearMonth }
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(yearMonth.month.getDisplayName(TextStyle.FULL, spanish).replaceFirstChar { it.uppercase(spanish) }, style = MaterialTheme.typography.headlineMedium)
                    Text("Toca un día para ver su estado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(enabled = month > 1, onClick = { month-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mes anterior") }
                IconButton(enabled = month < 12, onClick = { month++ }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Mes siguiente") }
            }
        }
        item { CalendarGrid(yearMonth, current, selected, viewModel) { selectedText = it.toString() } }
        item { CalendarLegend() }
        selected?.let { date ->
            item {
                val day = plan.days.first { it.date == date }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(date.format(longDate).replaceFirstChar { it.uppercase(spanish) }, style = MaterialTheme.typography.titleLarge)
                        day.readings.forEachIndexed { index, reading ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(if (viewModel.isComplete(date, index)) DayStatus.COMPLETE else DayStatus.NONE)
                                Text(reading.shortLabel, Modifier.padding(start = 10.dp).weight(1f))
                                if (viewModel.isComplete(date, index)) Icon(Icons.Default.Check, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun CalendarGrid(month: YearMonth, today: LocalDate, selected: LocalDate?, viewModel: AppViewModel, onSelect: (LocalDate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        val offset = month.atDay(1).dayOfWeek.value - 1
        val cells = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (week + List(7 - week.size) { null }).forEach { date ->
                    Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val status = viewModel.dayStatus(date)
                            val label = when (status) { DayStatus.COMPLETE -> "completo"; DayStatus.PARTIAL -> "parcial"; DayStatus.NONE -> "no leído" }
                            Surface(
                                modifier = Modifier.size(43.dp).semantics { contentDescription = "$date, $label" }.clickable { onSelect(date) },
                                shape = CircleShape, color = statusColor(status),
                                border = when { date == today -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary); date == selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface); else -> null },
                            ) { Box(contentAlignment = Alignment.Center) { Text("${date.dayOfMonth}", fontWeight = if (date == selected) FontWeight.Bold else FontWeight.Medium, color = statusTextColor(status)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun statusColor(status: DayStatus) = when (status) {
    DayStatus.COMPLETE -> Color(0xFFBCEBCB)
    DayStatus.PARTIAL -> Color(0xFFD5D8D5)
    DayStatus.NONE -> Color(0xFFFFDAD6)
}
private fun statusTextColor(status: DayStatus) = when (status) {
    DayStatus.COMPLETE -> Color(0xFF114C2B)
    DayStatus.PARTIAL -> Color(0xFF343735)
    DayStatus.NONE -> Color(0xFF7C2924)
}

@Composable
private fun CalendarLegend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(DayStatus.COMPLETE to "Completo", DayStatus.PARTIAL to "Parcial", DayStatus.NONE to "No leído").forEach { (status, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) { StatusDot(status); Text(text, Modifier.padding(start = 7.dp), style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
            Text("Personaliza la apariencia sin cambiar tu plan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Tema", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "Automático", "light" to "Claro", "dark" to "Oscuro").forEach { (id, label) ->
                    FilterChip(selected = state.themeMode == id, onClick = { viewModel.setAppearance(mode = id) }, label = { Text(label) })
                }
            }
        }
        item {
            Text("Color de énfasis", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accentPalettes.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { palette ->
                            Surface(
                                modifier = Modifier.weight(1f).clickable { viewModel.setAppearance(accent = palette.id) },
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(if (state.accent == palette.id) 2.dp else 1.dp, if (state.accent == palette.id) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = .3f)),
                            ) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(30.dp).clip(CircleShape).background(palette.color))
                                    Text(palette.name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 7.dp), maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null); Column(Modifier.padding(start = 14.dp)) {
                        Text("Guardado local", fontWeight = FontWeight.SemiBold)
                        Text("Tu plan y progreso permanecen en este dispositivo y pueden incluirse en su copia de seguridad.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedScreen(plan: ReadingPlan, state: AppUiState, viewModel: AppViewModel) {
    var theme by rememberSaveable(plan.id) { mutableStateOf(plan.theme) }
    var extra by rememberSaveable(plan.id) { mutableStateOf(plan.includeDeuterocanon) }
    var confirm by remember { mutableStateOf(false) }
    val stats = viewModel.stats()
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Opciones avanzadas", style = MaterialTheme.typography.headlineMedium)
            Text("Ajusta el énfasis del recorrido o genera uno nuevo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { ThemeDropdown(theme) { theme = it } }
        item { DeuterocanonSwitch(extra) { extra = it } }
        item {
            Button(
                onClick = { confirm = true }, enabled = !state.generating,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(if (state.generating) "Generando…" else "Generar un nuevo plan") }
        }
        item { HorizontalDivider() }
        item { Statistics(stats) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "Nota de método: para cumplir el capítulo evangélico diario, cada semestre recorre los Evangelios de forma continua y vuelve al inicio cuando es necesario. El primer semestre usa un orden temático aleatorio; el segundo, una armonización cronológica por etapas.",
                    modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("¿Sustituir el plan actual?") },
            text = { Text("Se creará un recorrido diferente y todo el progreso marcado volverá a 0 %. Esta acción no puede deshacerse.") },
            confirmButton = {
                Button(onClick = { confirm = false; viewModel.generate(theme, extra, true) }) { Text("Sí, generar") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ThemeSelector(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("¿Qué deseas profundizar?", style = MaterialTheme.typography.titleLarge)
        Text("Las conexiones diarias darán prioridad a este hilo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(BibleData.themes) { theme ->
                Card(
                    modifier = Modifier.width(230.dp).clickable { onSelect(theme.id) },
                    colors = CardDefaults.cardColors(containerColor = if (theme.id == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    border = if (theme.id == selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(theme.name, style = MaterialTheme.typography.titleMedium)
                        Text(theme.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 7.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = BibleData.themes.first { it.id == selected }
    Column {
        Text("Temática principal", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(current.name, fontWeight = FontWeight.SemiBold)
                    Text(current.description, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                BibleData.themes.forEach { theme ->
                    DropdownMenuItem(text = { Text(theme.name) }, onClick = { onSelect(theme.id); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun DeuterocanonSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("¿Deseas incluir lecturas adicionales?", style = MaterialTheme.typography.titleMedium)
                Text("Libros deuterocanónicos", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun Statistics(stats: ProgressStats) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("Tu avance", style = MaterialTheme.typography.headlineMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${stats.percent}%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(" de la Biblia completada", Modifier.padding(start = 8.dp, bottom = 6.dp))
                }
                LinearProgressIndicator(progress = { stats.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                Text("${stats.completedReadings} de ${stats.totalReadings} capítulos del plan", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Completos", stats.completeDays, Modifier.weight(1f), Color(0xFFBCEBCB))
            StatCard("Parciales", stats.partialDays, Modifier.weight(1f), Color(0xFFD5D8D5))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Días leídos", stats.readDays, Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer)
            StatCard("No leídos", stats.unreadDays, Modifier.weight(1f), Color(0xFFFFDAD6))
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier, color: Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF172019))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF354139))
        }
    }
}

private fun openBible(context: Context, reading: Reading, version: String) {
    val uri = youVersionUrl(reading, version).toUri()
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.sirma.mobile.bible.android"))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
