package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.viewmodel.IsicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: IsicViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var technicianNameInput by remember { mutableStateOf(viewModel.getTechnicianName()) }
    var exportCsvText by remember { mutableStateOf<String?>(null) }

    // Launcher para importar arquivo CSV
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val csvContent = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                    val count = viewModel.importCsvData(csvContent)
                    snackbarHostState.showSnackbar("Sucesso! $count itens importados via CSV.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Erro ao ler arquivo CSV: ${e.localizedMessage}")
                }
            }
        }
    }

    // Launcher para importar arquivo JSON
    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val jsonContent = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                    val count = viewModel.importJsonData(jsonContent)
                    snackbarHostState.showSnackbar("Sucesso! $count itens importados via JSON.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Erro ao ler arquivo JSON: ${e.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciamento de Dados & Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seção de Configuração do Perfil do Técnico
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PERFIL DO TÉCNICO OPERACIONAL", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Este nome aparecerá nos registros de saída e relatórios de campo.", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = technicianNameInput,
                        onValueChange = { technicianNameInput = it },
                        label = { Text("Nome do Técnico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.saveTechnicianName(technicianNameInput)
                            scope.launch { snackbarHostState.showSnackbar("Nome do técnico atualizado com sucesso!") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salvar Nome do Técnico")
                    }
                }
            }

            // Seção de Exportação (CSV e JSON)
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("EXPORTAÇÃO DE DADOS (BACKUP / PLANILHA)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Gere arquivos para conferência externa ou backup.", style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    exportCsvText = viewModel.exportCsvData()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    viewModel.exportJsonBackup()
                                    snackbarHostState.showSnackbar("Backup JSON gerado!")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup JSON")
                        }
                    }
                }
            }

            // Seção de Importação (CSV e JSON) e Limpeza
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("IMPORTAÇÃO & GESTÃO DA BASE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Carregue sua planilha personalizada de Poder de Terceiros.", style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { csvLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importar CSV")
                        }

                        Button(
                            onClick = { jsonLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importar JSON")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.resetDatabase()
                                snackbarHostState.showSnackbar("Banco de dados limpo com sucesso.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zerar Banco de Dados")
                    }
                }
            }
        }
    }

    // Diálogo para exibir o conteúdo do CSV gerado caso queira copiar
    exportCsvText?.let { csvData ->
        AlertDialog(
            onDismissRequest = { exportCsvText = null },
            title = { Text("Exportação de Inventário (CSV)") },
            text = {
                OutlinedTextField(
                    value = csvData,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { exportCsvText = null }) {
                    Text("Fechar")
                }
            }
        )
    }
}
