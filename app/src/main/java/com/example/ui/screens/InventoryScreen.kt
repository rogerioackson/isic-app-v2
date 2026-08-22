package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InventoryItem
import com.example.ui.components.ManualStockAdjustDialog
import com.example.ui.components.StockDifferenceBadge
import com.example.ui.components.UtilizationRateBadge
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberThirdParty
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenInStock
import com.example.ui.theme.RedContainer
import com.example.ui.theme.RedLowStock
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.RoyalBlue900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.IsicViewModel

@Composable
fun InventoryScreen(
    viewModel: IsicViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredInventoryItems.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.inventorySearch.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.inventoryCategoryFilter.collectAsStateWithLifecycle()
    val onlyThirdParty by viewModel.inventoryOnlyThirdParty.collectAsStateWithLifecycle()
    val onlyLowStock by viewModel.inventoryOnlyLowStock.collectAsStateWithLifecycle()
    val adjustingItem by viewModel.adjustingItem.collectAsStateWithLifecycle()

    val totalCurrentStock by viewModel.totalCurrentStock.collectAsStateWithLifecycle()
    val totalThirdParty by viewModel.totalThirdPartyCustody.collectAsStateWithLifecycle()

    val categories = listOf("TODOS", "Centrais & Teclados", "Sensores & Detectores", "CFTV / Câmeras", "Baterias & Energia", "Sirenes & Comunicação", "Cabos & Conectores")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Header / Title Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberThirdParty.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = AmberThirdParty,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "3. Inventário Geral ADT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )
                        Text(
                            text = "Poder de Terceiro, Estoque Atual, Diferença e Estatísticas",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    }
                }
            }
        }

        // Section 2: Resumo Rápido dos Totais
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBlue900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ESTOQUE ATUAL", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$totalCurrentStock un", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Almoxarifado", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.2f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PODER TERCEIRO", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$totalThirdParty un", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AmberContainer)
                        Text("Com Clientes / AD", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.2f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PATRIMÔNIO TOTAL", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${totalCurrentStock + totalThirdParty} un", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenContainer)
                        Text("${allItems.size} SKUs", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Section 3: Busca e Filtros
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setInventorySearch(it) },
                        placeholder = { Text("Filtrar por código, descrição ou categoria...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Slate500) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setInventorySearch("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar")
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue800,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_search_field")
                    )

                    // Quick Toggle Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = onlyThirdParty,
                            onClick = { viewModel.toggleInventoryOnlyThirdParty() },
                            label = { Text("Com Poder de Terceiro", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberThirdParty,
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = onlyLowStock,
                            onClick = { viewModel.toggleInventoryOnlyLowStock() },
                            label = { Text("Estoque Baixo", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedLowStock,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    // Category Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setInventoryCategory(cat) },
                                label = { Text(cat, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBlue800,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Tabela & Lista de Itens do Inventário
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TABELA DE ITENS (${items.size} SKUS)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Toque no lápis para ajuste manual",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }

        if (items.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum material corresponde aos filtros selecionados.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
                        )
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                InventoryItemRowCard(
                    item = item,
                    onAdjustClick = { viewModel.startAdjustingItem(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Manual Stock Adjustment Dialog
    adjustingItem?.let { itemToAdjust ->
        ManualStockAdjustDialog(
            item = itemToAdjust,
            onDismiss = { viewModel.cancelAdjustingItem() },
            onConfirm = { id, current, thirdParty ->
                viewModel.saveManualAdjustment(id, current, thirdParty)
            }
        )
    }
}

@Composable
fun InventoryItemRowCard(
    item: InventoryItem,
    onAdjustClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(14.dp))
            .testTag("inventory_item_${item.code}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Item Name, Code and Quick Edit Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.code,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue800
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Slate100)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.category,
                                fontSize = 9.sp,
                                color = Slate700,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "Localização: ${item.location}",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                IconButton(
                    onClick = onAdjustClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate100)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Ajuste manual",
                        tint = RoyalBlue800,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            // The 4 Required Columns & Indicators:
            // 1. Poder de Terceiro
            // 2. Estoque Atual
            // 3. Diferença
            // 4. Estatísticas de Utilização
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Poder de Terceiro
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "PODER TERCEIRO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.thirdPartyCustody > 0) AmberContainer else Slate100
                    ) {
                        Text(
                            text = "${item.thirdPartyCustody} ${item.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.thirdPartyCustody > 0) AmberThirdParty else Slate700,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Column 2: Estoque Atual
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ESTOQUE ATUAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.currentStock > (item.targetStock / 2)) GreenContainer else RedContainer
                    ) {
                        Text(
                            text = "${item.currentStock} ${item.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.currentStock > (item.targetStock / 2)) GreenInStock else RedLowStock,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Column 3: Diferença
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DIFERENÇA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    StockDifferenceBadge(difference = item.stockDifference)
                }

                // Column 4: Estatísticas de Utilização
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "UTILIZAÇÃO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    UtilizationRateBadge(
                        percent = item.utilizationRatePercent,
                        totalOut = item.totalOutCount
                    )
                }
            }
        }
    }
}
