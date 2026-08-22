package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MovementType
import com.example.ui.components.DashboardActionCard
import com.example.ui.components.SummaryKpiCard
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberThirdParty
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenInStock
import com.example.ui.theme.RoyalBlue100
import com.example.ui.theme.RoyalBlue700
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.RoyalBlue900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.IsicViewModel
import com.example.ui.viewmodel.ScreenDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: IsicViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val recentMovements by viewModel.recentMovements.collectAsStateWithLifecycle()
    val totalCurrentStock by viewModel.totalCurrentStock.collectAsStateWithLifecycle()
    val totalThirdParty by viewModel.totalThirdPartyCustody.collectAsStateWithLifecycle()

    val totalSkus = items.size
    val lowStockCount = items.count { it.currentStock <= (it.targetStock / 2) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Technician & Station Header Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBlue900),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(RoyalBlue900, RoyalBlue700)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Técnico",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Estoque Técnico ADT",
                                    fontSize = 12.sp,
                                    color = RoyalBlue100,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyanAccent.copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "UNIDADE 01",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Técnico Carlos ADT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Base Operacional • Controle Offline Ativo",
                                fontSize = 11.sp,
                                color = RoyalBlue100.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Summary KPI Metrics (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "VISÃO GERAL DO ESTOQUE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryKpiCard(
                        label = "Estoque Almox.",
                        value = "$totalCurrentStock un",
                        subValue = "$totalSkus SKUs cadastrados",
                        icon = Icons.Default.Warehouse,
                        tintColor = RoyalBlue800,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryKpiCard(
                        label = "Poder de Terceiro",
                        value = "$totalThirdParty un",
                        subValue = "Em campo / técnicos",
                        icon = Icons.Default.Security,
                        tintColor = AmberThirdParty,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryKpiCard(
                        label = "Total Físico",
                        value = "${totalCurrentStock + totalThirdParty} un",
                        subValue = "Patrimônio total",
                        icon = Icons.Default.Inventory2,
                        tintColor = GreenInStock,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryKpiCard(
                        label = "Alertas de Estoque",
                        value = "$lowStockCount",
                        subValue = if (lowStockCount > 0) "Abaixo do mínimo" else "Estoque regular",
                        icon = Icons.Default.AssignmentReturn,
                        tintColor = if (lowStockCount > 0) Color(0xFFC62828) else Slate700,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // The 4 Primary Menu Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "MÓDULOS OPERACIONAIS iSiC",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                // 1. Registrar Saída
                DashboardActionCard(
                    number = "1",
                    title = "Registrar Saída",
                    description = "Baixa de materiais com seleção de Cliente, OS, itens e quantidades",
                    icon = Icons.Default.Outbox,
                    accentColor = RoyalBlue800,
                    badgeText = "Operação",
                    onClick = { viewModel.navigateTo(ScreenDestination.REGISTER_EXIT) }
                )

                // 2. Reposição AD
                DashboardActionCard(
                    number = "2",
                    title = "Reposição AD",
                    description = "Controle de retorno de materiais, sobras técnicas e equipamentos em garantia",
                    icon = Icons.Default.AssignmentReturn,
                    accentColor = Color(0xFF00897B),
                    badgeText = "Retorno",
                    onClick = { viewModel.navigateTo(ScreenDestination.AD_REPOSITION) }
                )

                // 3. Inventário
                DashboardActionCard(
                    number = "3",
                    title = "Inventário",
                    description = "Poder de Terceiro, Estoque Atual, Diferença e Estatísticas de Utilização",
                    icon = Icons.Default.Inventory2,
                    accentColor = AmberThirdParty,
                    badgeText = "$totalSkus Itens",
                    onClick = { viewModel.navigateTo(ScreenDestination.INVENTORY) }
                )

                // 4. Carga de Dados
                DashboardActionCard(
                    number = "4",
                    title = "Carga de Dados",
                    description = "Carga inicial do catálogo mestre ADT, importação e exportação de dados offline",
                    icon = Icons.Default.CloudDownload,
                    accentColor = Slate700,
                    badgeText = "Backup",
                    onClick = { viewModel.navigateTo(ScreenDestination.DATA_LOAD) }
                )
            }
        }

        // Recent Movements Feed
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ÚLTIMAS MOVIMENTAÇÕES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate500,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "${recentMovements.size} registros",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                if (recentMovements.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma movimentação registrada ainda.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
                            )
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    ) {
                        Column {
                            recentMovements.forEachIndexed { index, mov ->
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                val isExit = mov.movementType == MovementType.SAIDA

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isExit) RoyalBlue100.copy(alpha = 0.6f) else GreenContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isExit) Icons.Default.Outbox else Icons.Default.AssignmentReturn,
                                            contentDescription = null,
                                            tint = if (isExit) RoyalBlue800 else GreenInStock,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isExit) "Saída • ${mov.osNumber}" else "Reposição AD • ${mov.osNumber}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900
                                                )
                                            )
                                            Text(
                                                text = "${mov.totalItemsCount} itens",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExit) RoyalBlue800 else GreenInStock
                                            )
                                        }
                                        Text(
                                            text = mov.clientName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Slate700,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dateFormat.format(Date(mov.timestamp)),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate500,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                if (index < recentMovements.size - 1) {
                                    HorizontalDivider(color = Slate100)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
