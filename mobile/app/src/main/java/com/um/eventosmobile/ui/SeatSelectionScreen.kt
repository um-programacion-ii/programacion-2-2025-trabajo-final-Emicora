package com.um.eventosmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.um.eventosmobile.core.di.ViewModelFactory
import com.um.eventosmobile.domain.model.SeatMapDomain
import com.um.eventosmobile.ui.theme.AccentGreen
import com.um.eventosmobile.ui.theme.AccentOrange
import com.um.eventosmobile.ui.theme.AccentRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    viewModelFactory: ViewModelFactory,
    eventId: Long,
    refreshKey: Int = 0,
    onBack: () -> Unit,
    onContinue: (Long, List<Pair<String, Int>>, String?) -> Unit
) {
    val viewModel = remember(eventId) { viewModelFactory.createSeatSelectionViewModel(eventId) }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(eventId, refreshKey) {
        viewModel.load()
    }

    fun loadSeatMap() {
        viewModel.load()
    }

    fun blockAndContinue() {
        viewModel.blockAndContinue(
            onSuccess = { seats, expiresAt ->
                onContinue(eventId, seats, expiresAt)
            },
            onError = { msg ->
                // Mostrar error en UI
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Asientos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Asientos seleccionados: ${uiState.selectedSeats.size}/4",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { blockAndContinue() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
                        enabled = uiState.selectedSeats.isNotEmpty() && !uiState.isBlocking,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isBlocking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Continuar")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null && uiState.seatMap == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error?.message ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.load() }) {
                            Text("Reintentar")
                        }
                    }
                }
                uiState.seatMap != null -> {
                    if (uiState.seatMap!!.asientos.isEmpty()) {
                        // Lista vacía
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No hay asientos disponibles",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadSeatMap() }) {
                                Text("Reintentar")
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Leyenda mejorada con iconos y mejor diseño
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Leyenda",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LegendItem(
                                            icon = Icons.Default.EventSeat,
                                            label = "Libre",
                                            color = AccentGreen
                                        )
                                        LegendItem(
                                            icon = Icons.Default.Person,
                                            label = "Ocupado",
                                            color = AccentRed
                                        )
                                        LegendItem(
                                            icon = Icons.Default.Lock,
                                            label = "Bloqueado",
                                            color = AccentOrange
                                        )
                                        LegendItem(
                                            icon = Icons.Default.CheckCircle,
                                            label = "Seleccionado",
                                            color = MaterialTheme.colorScheme.primary,
                                            isSelected = true
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Error si hay
                            uiState.error?.let {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = it.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                        // Mapa de asientos - agrupar por fila y mostrar en grid
                        val seatsByRow = uiState.seatMap!!.asientos.groupBy { it.fila }
                        val columnas = uiState.eventDetail?.columnAsientos ?: seatsByRow.values.maxOfOrNull { it.size } ?: 10
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnas),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            seatsByRow.forEach { (fila, seats) ->
                                item(span = { GridItemSpan(columnas) }) {
                                    Text(
                                        text = "Fila $fila",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    )
                                }
                                items(seats.sortedBy { it.numero }) { seat ->
                                    SeatItem(
                                        seat = seat,
                                        isSelected = uiState.selectedSeats.contains(seat.fila to seat.numero),
                                        onClick = {
                                            if (seat.estado == SeatMapDomain.SeatDomain.SeatStatusDomain.LIBRE || uiState.selectedSeats.contains(seat.fila to seat.numero)) {
                                                viewModel.toggleSeat(seat.fila, seat.numero, seat.estado)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun SeatItem(
    seat: SeatMapDomain.SeatDomain,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (seat.estado) {
        SeatMapDomain.SeatDomain.SeatStatusDomain.LIBRE -> AccentGreen
        SeatMapDomain.SeatDomain.SeatStatusDomain.OCUPADO -> AccentRed
        SeatMapDomain.SeatDomain.SeatStatusDomain.BLOQUEADO -> AccentOrange
    }
    
    val icon = when {
        isSelected -> Icons.Default.CheckCircle
        seat.estado == SeatMapDomain.SeatDomain.SeatStatusDomain.OCUPADO -> Icons.Default.Person
        seat.estado == SeatMapDomain.SeatDomain.SeatStatusDomain.BLOQUEADO -> Icons.Default.Lock
        else -> Icons.Default.EventSeat
    }
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    val borderWidth = if (isSelected) 3.dp else 0.dp
    val elevation = if (isSelected) 8.dp else 2.dp
    
    Card(
        modifier = Modifier
            .size(56.dp)
            .clickable(
                enabled = seat.estado == SeatMapDomain.SeatDomain.SeatStatusDomain.LIBRE || isSelected,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Asiento ${seat.fila}-${seat.numero}",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun LegendItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    isSelected: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Card(
            modifier = Modifier.size(32.dp),
            colors = CardDefaults.cardColors(containerColor = color),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

