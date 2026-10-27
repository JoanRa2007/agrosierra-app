package pe.upeu.agrosierra.presentation.listado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.agrosierra.domain.model.Lote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    alAbrirLote: (Long) -> Unit,
    alAbrirPerfil: () -> Unit,
    vm: ListadoViewModel = koinViewModel(),
) {
    val s by vm.estado.collectAsState()
    val texto by vm.texto.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Padron de lotes") },
                actions = {
                    IconButton(onClick = vm::sincronizar, enabled = !s.sincronizando) {
                        Icon(Icons.Filled.Sync, "Sincronizar")
                    }
                    IconButton(onClick = alAbrirPerfil) { Icon(Icons.Filled.Person, "Perfil") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (s.sincronizando) LinearProgressIndicator(Modifier.fillMaxWidth())
            EstadoSincronizacion(s)
            OutlinedTextField(
                value = texto,
                onValueChange = vm::onTexto,
                placeholder = { Text("Buscar por nombre o codigo") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = s.valle == null, onClick = { vm.onValle(null) }, label = { Text("Todos") }) }
                items(s.valles) { v ->
                    FilterChip(selected = s.valle == v, onClick = { vm.onValle(v) }, label = { Text(v) })
                }
            }
            when {
                s.cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                s.lotes.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                    Text(
                        if (texto.isBlank() && s.valle == null) "Aun no hay lotes guardados. Sincroniza para descargarlos."
                        else "No hay lotes que coincidan con el filtro.",
                        textAlign = TextAlign.Center,
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(s.lotes, key = { it.id }) { lote -> TarjetaLote(lote) { alAbrirLote(lote.id) } }
                }
            }
        }
    }
}

@Composable
private fun EstadoSincronizacion(s: ListadoUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (s.conectado) Icons.Filled.CloudDone else Icons.Filled.CloudOff, null,
            tint = if (s.conectado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            buildString {
                append(if (s.conectado) "En linea" else "Sin conexion")
                append(" · Ultima sincronizacion: ")
                append(s.ultimaSincronizacion ?: "nunca")
                if (s.pendientes > 0) append(" · ${s.pendientes} pendiente(s)")
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TarjetaLote(lote: Lote, alClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = alClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(lote.codigo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(lote.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (lote.campaniaCerrada) {
                    AssistChip(
                        onClick = alClick,
                        label = { Text("Cerrada") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, Modifier.size(16.dp)) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${lote.cultivo} · Valle ${lote.valle} · ${lote.hectareas} ha", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
