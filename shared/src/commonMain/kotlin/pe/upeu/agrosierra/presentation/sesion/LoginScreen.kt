package pe.upeu.agrosierra.presentation.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(vm: LoginViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Agriculture, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Text("AgroCampo", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Cooperativa AgroSierra · Labores de campo", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
        Column(Modifier.widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = s.usuario,
                onValueChange = vm::onUsuario,
                label = { Text("Usuario") },
                singleLine = true,
                isError = s.errorUsuario != null,
                supportingText = s.errorUsuario?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.contrasena,
                onValueChange = vm::onContrasena,
                label = { Text("Contrasena") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = s.errorContrasena != null,
                supportingText = s.errorContrasena?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.ingresar() }),
                modifier = Modifier.fillMaxWidth(),
            )
            s.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::ingresar, enabled = !s.cargando, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (s.cargando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Ingresar")
            }
        }
    }
}
