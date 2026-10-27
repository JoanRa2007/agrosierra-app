package pe.upeu.agrosierra.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.upeu.agrosierra.domain.model.TipoLabor
import pe.upeu.agrosierra.presentation.navigation.Destino

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioLaborScreen(
    destino: Destino.FormularioLabor,
    alTerminar: () -> Unit,
    vm: FormularioLaborViewModel = koinViewModel(parameters = { parametersOf(destino) }),
) {
    val s by vm.estado.collectAsState()
    LaunchedEffect(s.terminado) { if (s.terminado) alTerminar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.esEdicion) "Corregir labor" else "Registrar labor") },
                navigationIcon = { IconButton(onClick = alTerminar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Tipo de labor", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoLabor.entries.forEach { t ->
                    FilterChip(selected = s.tipo == t, onClick = { vm.onTipo(t) }, label = { Text(t.name) })
                }
            }
            s.errores["tipo"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Campo("Fecha (AAAA-MM-DD)", s.fecha, vm::onFecha, s.errores["fecha"])
            Campo("Responsable", s.responsable, vm::onResponsable, s.errores["responsable"])
            Campo(
                etiqueta = "Cantidad",
                valor = s.cantidad,
                alCambiar = vm::onCantidad,
                error = s.errores["cantidad"],
                ayuda = s.dosisMaximaTotal?.takeIf { s.tipo == TipoLabor.FUMIGACION }?.let { "Maximo para este lote: $it" },
                teclado = KeyboardType.Decimal,
            )
            Campo("Observacion", s.observacion, vm::onObservacion, s.errores["observacion"], unaLinea = false)

            s.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (s.reintentable) TextButton(onClick = vm::guardar) { Text("Reintentar") }

            Button(onClick = vm::guardar, enabled = !s.guardando, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (s.guardando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Guardar")
            }
            Text(
                "Sin conexion la labor se guarda en el telefono y se envia al sincronizar.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    error: String?,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    unaLinea: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = unaLinea,
        minLines = if (unaLinea) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth(),
    )
}
