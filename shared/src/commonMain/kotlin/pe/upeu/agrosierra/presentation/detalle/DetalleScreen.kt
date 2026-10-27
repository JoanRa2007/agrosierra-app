package pe.upeu.agrosierra.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    loteId: Long,
    alVolver: () -> Unit,
    alRegistrar: () -> Unit,
    alEditar: (Long) -> Unit,
    vm: DetalleViewModel = koinViewModel(parameters = { parametersOf(loteId) }),
) {
    val s by vm.estado.collectAsState()
    var confirmarCierre by remember { mutableStateOf(false) }
    var porEliminar by remember { mutableStateOf<Labor?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.lote?.let { "${it.codigo} · ${it.nombre}" } ?: "Detalle del lote") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            )
        },
        floatingActionButton = {
            if (s.puedeRegistrar) {
                ExtendedFloatingActionButton(
                    onClick = alRegistrar,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Registrar labor") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (s.procesando) LinearProgressIndicator(Modifier.fillMaxWidth())
            val lote = s.lote
            when {
                s.cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                lote == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("El lote no existe en la base local") }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { DatosLote(lote, s.puedeCerrarCampania, s.procesando) { confirmarCierre = true } }
                    item { Text("Labores (${s.labores.size})", style = MaterialTheme.typography.titleMedium) }
                    if (s.labores.isEmpty()) {
                        item { Text("Este lote aun no tiene labores registradas.") }
                    }
                    items(s.labores, key = { it.id }) { labor ->
                        TarjetaLabor(
                            labor = labor,
                            puedeEditar = s.puedeEditar,
                            puedeEliminar = s.puedeEliminar,
                            alEditar = { alEditar(labor.id) },
                            alEliminar = { porEliminar = labor },
                        )
                    }
                }
            }
        }
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("Cerrar campania") },
            text = { Text("Despues de cerrar no se podran registrar ni corregir labores en este lote.") },
            confirmButton = { TextButton(onClick = { confirmarCierre = false; vm.cerrarCampania() }) { Text("Cerrar campania") } },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } },
        )
    }
    porEliminar?.let { labor ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar labor") },
            text = { Text("Se eliminara la labor de ${labor.tipo.name.lowercase()} del ${labor.fecha}.") },
            confirmButton = { TextButton(onClick = { porEliminar = null; vm.eliminar(labor) }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DatosLote(lote: Lote, puedeCerrar: Boolean, procesando: Boolean, alCerrar: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(lote.nombre, style = MaterialTheme.typography.titleLarge)
            Text("Cultivo: ${lote.cultivo} · Valle: ${lote.valle}")
            Text("Hectareas: ${lote.hectareas} · Dosis maxima: ${lote.dosisMaximaPorHectarea}/ha")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (lote.campaniaCerrada) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Campania cerrada", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text("Campania abierta", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (puedeCerrar) {
                OutlinedButton(onClick = alCerrar, enabled = !procesando) {
                    Icon(Icons.Filled.Lock, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Cerrar campania")
                }
            }
        }
    }
}

@Composable
private fun TarjetaLabor(
    labor: Labor,
    puedeEditar: Boolean,
    puedeEliminar: Boolean,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(labor.tipo.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(labor.fecha, style = MaterialTheme.typography.bodySmall)
                    if (labor.pendiente) {
                        Spacer(Modifier.width(8.dp))
                        // Marca de fila pendiente de envio (id provisional negativo).
                        AssistChip(
                            onClick = {},
                            label = { Text("Pendiente") },
                            leadingIcon = { Icon(Icons.Filled.CloudUpload, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        )
                    }
                }
                Text("Responsable: ${labor.responsable}")
                Text("Cantidad: ${labor.cantidad}")
                if (labor.observacion.isNotBlank()) Text(labor.observacion, style = MaterialTheme.typography.bodySmall)
            }
            if (puedeEditar) IconButton(onClick = alEditar) { Icon(Icons.Filled.Edit, "Corregir") }
            if (puedeEliminar) IconButton(onClick = alEliminar) { Icon(Icons.Filled.Delete, "Eliminar") }
        }
    }
}
