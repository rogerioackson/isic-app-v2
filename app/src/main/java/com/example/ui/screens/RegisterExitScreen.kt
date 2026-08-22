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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Client
import com.example.data.model.InventoryItem
import com.example.ui.components.NewClientDialog
import com.example.ui.theme.AmberThirdParty
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenInStock
import com.example.ui.theme.RedContainer
import com.example.ui.theme.RedLowStock
import com.example.ui.theme.RoyalBlue100
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.RoyalBlue900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.IsicViewModel
import com.example.ui.viewmodel.ScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterExitScreen(
    viewModel: IsicViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val selectedClient by viewModel.exitSelectedClient.collectAsStateWithLifecycle()
    val technician by viewModel.exitTechnician.collectAsStateWithLifecycle()
    val osNumber by viewModel.exitOsNumber.collectAsStateWithLifecycle()
    val notes by viewModel.exitNotes.collectAsStateWithLifecycle()
    val cart by viewModel.exitCart.collectAsStateWithLifecycle()

    var showClientPicker by remember { mutableStateOf(false) }
    var showNewClientDialog by remember { mutableStateOf(false) }
    var itemSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("TODOS") }

    val categories = listOf("TODOS", "Centrais & Teclados", "Sensores & Detectores", "CFTV / Câmeras", "Baterias & Energia", "Sirenes & Comunicação", "Cabos & Conectores")

    val filteredItems = items.filter { item ->
        val matchesSearch = itemSearchQuery.isBlank() ||
                item.name.contains(itemSearchQuery, ignoreCase = true) ||
                item.code.contains(itemSearchQuery, ignoreCase = true)
        val matchesCat = selectedCategoryFilter == "TODOS" || item.category.equals(selectedCategoryFilter, ignoreCase = true)
        matchesSearch && matchesCat
    }

    val totalItemsInCart = cart.values.sumOf { it.second }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            .background(RoyalBlue800.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Outbox,
                            contentDescription = null,
                            tint = RoyalBlue800,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "1. Registrar Saída de Materiais",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )
                        Text(
                            text = "Baixa do almoxarifado para atendimento técnico",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    }
                }
            }
        }

        // Section 2: Seleção de Cliente e Dados da O.S.
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DADOS DO CLIENTE & ORDEM DE SERVIÇO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate500,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    // Client Selector Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedClient != null) RoyalBlue100.copy(alpha = 0.3f) else Slate100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClientPicker = true }
                            .testTag("select_client_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = if (selectedClient != null) RoyalBlue800 else Slate500,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedClient?.name ?: "Toque para selecionar o Cliente *",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selectedClient != null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedClient != null) Slate900 else Slate500
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (selectedClient != null) {
                                        Text(
                                            text = "${selectedClient?.code} • ${selectedClient?.segment} • ${selectedClient?.address}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Slate700),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (selectedClient != null) {
                                IconButton(
                                    onClick = { viewModel.setExitClient(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpar cliente",
                                        tint = Slate500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // OS & Technician Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = osNumber,
                            onValueChange = { viewModel.setExitOsNumber(it) },
                            label = { Text("Nº da O.S. / Chamado") },
                            placeholder = { Text("Ex: OS-98421") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue800,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("os_number_input")
                        )

                        OutlinedTextField(
                            value = technician,
                            onValueChange = { viewModel.setExitTechnician(it) },
                            label = { Text("Técnico Responsável") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue800,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("technician_input")
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.setExitNotes(it) },
                        label = { Text("Observações da Saída / Motivo (Opcional)") },
                        placeholder = { Text("Ex: Instalação de novo ponto ou ampliação") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue800,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section 3: Selecionar Itens e Quantidades
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECIONAR ITENS DO ESTOQUE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate500,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "${filteredItems.size} disponíveis",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }

                    // Search input
                    OutlinedTextField(
                        value = itemSearchQuery,
                        onValueChange = { itemSearchQuery = it },
                        placeholder = { Text("Buscar item por nome ou código...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Slate500)
                        },
                        trailingIcon = {
                            if (itemSearchQuery.isNotBlank()) {
                                IconButton(onClick = { itemSearchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar busca")
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue800,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) },
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

        // Product Catalog Items List for Exit
        items(filteredItems, key = { it.id }) { item ->
            val inCartQty = cart[item.code]?.second ?: 0
            val isOutOfStock = item.currentStock <= 0

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (inCartQty > 0) RoyalBlue100.copy(alpha = 0.25f) else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (inCartQty > 0) RoyalBlue800 else Slate200,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    fontSize = 9.sp,
                                    color = Slate700
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Estoque: ${item.currentStock} ${item.unit}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOutOfStock) RedLowStock else GreenInStock
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• Com terceiros: ${item.thirdPartyCustody}",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quantity Controls
                    if (isOutOfStock) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Sem Estoque",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedLowStock
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (inCartQty > 0) {
                                IconButton(
                                    onClick = { viewModel.updateExitCartQty(item.code, inCartQty - 1) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Slate100)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Diminuir",
                                        tint = Slate900,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "$inCartQty",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue800
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.addItemToExitCart(item, 1) },
                                enabled = inCartQty < item.currentStock,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (inCartQty < item.currentStock) RoyalBlue800 else Slate200)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Adicionar",
                                    tint = if (inCartQty < item.currentStock) Color.White else Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Resumo e Confirmação da Saída
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = RoyalBlue800,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Resumo da Saída",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "$totalItemsInCart itens selecionados",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue800
                        )
                    }

                    if (cart.isEmpty()) {
                        Text(
                            text = "Nenhum item selecionado. Use o botão '+' acima nos itens desejados.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            cart.values.forEach { (cartItem, qty) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate50, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cartItem.name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = Slate900
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${cartItem.code} • Local: ${cartItem.location}",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$qty ${cartItem.unit}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBlue800
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { viewModel.removeExitCartItem(cartItem.code) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remover item",
                                                tint = Color(0xFFC62828),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearExitForm() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Limpar")
                        }

                        Button(
                            onClick = {
                                viewModel.confirmExitMovement {
                                    viewModel.navigateTo(ScreenDestination.DASHBOARD)
                                }
                            },
                            enabled = selectedClient != null && cart.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("confirm_exit_button")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmar Saída")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal Sheet for Client Selection
    if (showClientPicker) {
        ModalBottomSheet(
            onDismissRequest = { showClientPicker = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            var clientSearch by remember { mutableStateOf("") }
            val filteredClients = clients.filter {
                clientSearch.isBlank() || it.name.contains(clientSearch, ignoreCase = true) || it.code.contains(clientSearch, ignoreCase = true)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selecionar Cliente ADT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = {
                        showClientPicker = false
                        showNewClientDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo Cliente")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientSearch,
                    onValueChange = { clientSearch = it },
                    placeholder = { Text("Pesquisar cliente...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredClients) { client ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedClient?.id == client.id) RoyalBlue100.copy(alpha = 0.4f) else Slate50,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setExitClient(client)
                                    showClientPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = client.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${client.code} • ${client.segment} • ${client.address}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (selectedClient?.id == client.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = RoyalBlue800
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showNewClientDialog) {
        NewClientDialog(
            onDismiss = { showNewClientDialog = false },
            onConfirm = { name, code, segment, address, phone ->
                viewModel.addNewClient(name, code, segment, address, phone)
                showNewClientDialog = false
            }
        )
    }
}
