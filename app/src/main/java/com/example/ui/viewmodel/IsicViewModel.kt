package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.IsicDatabase
import com.example.data.model.Client
import com.example.data.model.InventoryItem
import com.example.data.model.ItemCondition
import com.example.data.model.StockMovement
import com.example.data.repository.IsicRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    DASHBOARD,
    REGISTER_EXIT,
    AD_REPOSITION,
    INVENTORY,
    DATA_LOAD
}

data class ReturnItemDraft(
    val item: InventoryItem,
    val quantity: Int,
    val condition: ItemCondition
)

class IsicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IsicRepository

    init {
        val db = IsicDatabase.getInstance(application)
        repository = IsicRepository(db)

        // Preload sample data if database is empty on first launch
        viewModelScope.launch {
            val counts = repository.getSummaryCounts()
            if (counts.first == 0) {
                repository.loadAdtSampleCatalog()
            }
        }
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow(ScreenDestination.DASHBOARD)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    // Notification / Toast Feedback
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun postMessage(msg: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(msg)
        }
    }

    // Repository Flows
    val allItems: StateFlow<List<InventoryItem>> = repository.allItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allClients: StateFlow<List<Client>> = repository.allClients.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMovements: StateFlow<List<StockMovement>> = repository.allMovements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentMovements: StateFlow<List<StockMovement>> = repository.recentMovements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalCurrentStock: StateFlow<Int> = repository.totalCurrentStock.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    ).let { flow ->
        combine(flow) { it.firstOrNull() ?: 0 }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    }

    val totalThirdPartyCustody: StateFlow<Int> = repository.totalThirdPartyCustody.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    ).let { flow ->
        combine(flow) { it.firstOrNull() ?: 0 }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    }

    // ==========================================
    // 1. REGISTRAR SAÍDA STATE
    // ==========================================
    private val _exitSelectedClient = MutableStateFlow<Client?>(null)
    val exitSelectedClient: StateFlow<Client?> = _exitSelectedClient.asStateFlow()

    private val _exitTechnician = MutableStateFlow("Técnico Carlos ADT")
    val exitTechnician: StateFlow<String> = _exitTechnician.asStateFlow()

    private val _exitOsNumber = MutableStateFlow("")
    val exitOsNumber: StateFlow<String> = _exitOsNumber.asStateFlow()

    private val _exitNotes = MutableStateFlow("")
    val exitNotes: StateFlow<String> = _exitNotes.asStateFlow()

    // Map: Item Code -> Pair(InventoryItem, Quantity)
    private val _exitCart = MutableStateFlow<Map<String, Pair<InventoryItem, Int>>>(emptyMap())
    val exitCart: StateFlow<Map<String, Pair<InventoryItem, Int>>> = _exitCart.asStateFlow()

    fun setExitClient(client: Client?) {
        _exitSelectedClient.value = client
    }

    fun setExitTechnician(tech: String) {
        _exitTechnician.value = tech
    }

    fun setExitOsNumber(os: String) {
        _exitOsNumber.value = os
    }

    fun setExitNotes(notes: String) {
        _exitNotes.value = notes
    }

    fun addItemToExitCart(item: InventoryItem, qty: Int = 1) {
        val current = _exitCart.value.toMutableMap()
        val existingQty = current[item.code]?.second ?: 0
        val newQty = (existingQty + qty).coerceAtMost(item.currentStock)
        if (newQty > 0) {
            current[item.code] = Pair(item, newQty)
            _exitCart.value = current
            postMessage("Adicionado: ${item.name} (${newQty} ${item.unit})")
        }
    }

    fun updateExitCartQty(itemCode: String, qty: Int) {
        val current = _exitCart.value.toMutableMap()
        val pair = current[itemCode] ?: return
        if (qty <= 0) {
            current.remove(itemCode)
        } else {
            val maxAllowed = pair.first.currentStock
            val finalQty = qty.coerceAtMost(maxAllowed)
            current[itemCode] = Pair(pair.first, finalQty)
        }
        _exitCart.value = current
    }

    fun removeExitCartItem(itemCode: String) {
        val current = _exitCart.value.toMutableMap()
        current.remove(itemCode)
        _exitCart.value = current
    }

    fun clearExitForm() {
        _exitSelectedClient.value = null
        _exitOsNumber.value = ""
        _exitNotes.value = ""
        _exitCart.value = emptyMap()
    }

    fun confirmExitMovement(onSuccess: () -> Unit) {
        val client = _exitSelectedClient.value
        val items = _exitCart.value.values.toList()

        if (client == null) {
            postMessage("Selecione um cliente para registrar a saída.")
            return
        }
        if (items.isEmpty()) {
            postMessage("Selecione ao menos 1 item para a saída.")
            return
        }

        viewModelScope.launch {
            try {
                repository.registerExit(
                    clientName = "${client.name} (${client.code})",
                    technicianName = _exitTechnician.value.ifBlank { "Técnico ADT" },
                    osNumber = _exitOsNumber.value,
                    notes = _exitNotes.value,
                    itemsToExit = items
                )
                postMessage("Saída de ${items.sumOf { it.second }} itens registrada com sucesso!")
                clearExitForm()
                onSuccess()
            } catch (e: Exception) {
                postMessage("Erro ao registrar saída: ${e.message}")
            }
        }
    }

    // ==========================================
    // 2. REPOSIÇÃO AD STATE
    // ==========================================
    private val _repoTechnician = MutableStateFlow("Técnico Carlos ADT")
    val repoTechnician: StateFlow<String> = _repoTechnician.asStateFlow()

    private val _repoReference = MutableStateFlow("")
    val repoReference: StateFlow<String> = _repoReference.asStateFlow()

    private val _repoNotes = MutableStateFlow("")
    val repoNotes: StateFlow<String> = _repoNotes.asStateFlow()

    private val _repoDraftList = MutableStateFlow<List<ReturnItemDraft>>(emptyList())
    val repoDraftList: StateFlow<List<ReturnItemDraft>> = _repoDraftList.asStateFlow()

    fun setRepoTechnician(tech: String) {
        _repoTechnician.value = tech
    }

    fun setRepoReference(ref: String) {
        _repoReference.value = ref
    }

    fun setRepoNotes(notes: String) {
        _repoNotes.value = notes
    }

    fun addRepoDraftItem(item: InventoryItem, qty: Int, condition: ItemCondition) {
        if (qty <= 0) return
        val current = _repoDraftList.value.toMutableList()
        // Check if already in draft with same condition
        val existingIndex = current.indexOfFirst { it.item.code == item.code && it.condition == condition }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + qty)
        } else {
            current.add(ReturnItemDraft(item = item, quantity = qty, condition = condition))
        }
        _repoDraftList.value = current
        postMessage("Adicionado à reposição: ${item.name} (${qty} ${item.unit})")
    }

    fun removeRepoDraftItem(index: Int) {
        val current = _repoDraftList.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _repoDraftList.value = current
        }
    }

    fun clearRepoForm() {
        _repoReference.value = ""
        _repoNotes.value = ""
        _repoDraftList.value = emptyList()
    }

    fun confirmRepositionMovement(onSuccess: () -> Unit) {
        val drafts = _repoDraftList.value
        if (drafts.isEmpty()) {
            postMessage("Adicione itens para confirmar a Reposição AD.")
            return
        }

        viewModelScope.launch {
            try {
                val itemsToReturn = drafts.map { Triple(it.item, it.quantity, it.condition) }
                repository.registerAdReposition(
                    technicianName = _repoTechnician.value.ifBlank { "Técnico ADT" },
                    osOrReference = _repoReference.value,
                    notes = _repoNotes.value,
                    itemsToReturn = itemsToReturn
                )
                postMessage("Reposição AD de ${drafts.sumOf { it.quantity }} itens concluída!")
                clearRepoForm()
                onSuccess()
            } catch (e: Exception) {
                postMessage("Erro ao registrar reposição: ${e.message}")
            }
        }
    }

    // ==========================================
    // 3. INVENTÁRIO (FILTROS & AJUSTE MANUAL)
    // ==========================================
    private val _inventorySearch = MutableStateFlow("")
    val inventorySearch: StateFlow<String> = _inventorySearch.asStateFlow()

    private val _inventoryCategoryFilter = MutableStateFlow("TODOS")
    val inventoryCategoryFilter: StateFlow<String> = _inventoryCategoryFilter.asStateFlow()

    private val _inventoryOnlyThirdParty = MutableStateFlow(false)
    val inventoryOnlyThirdParty: StateFlow<Boolean> = _inventoryOnlyThirdParty.asStateFlow()

    private val _inventoryOnlyLowStock = MutableStateFlow(false)
    val inventoryOnlyLowStock: StateFlow<Boolean> = _inventoryOnlyLowStock.asStateFlow()

    fun setInventorySearch(q: String) {
        _inventorySearch.value = q
    }

    fun setInventoryCategory(cat: String) {
        _inventoryCategoryFilter.value = cat
    }

    fun toggleInventoryOnlyThirdParty() {
        _inventoryOnlyThirdParty.value = !_inventoryOnlyThirdParty.value
    }

    fun toggleInventoryOnlyLowStock() {
        _inventoryOnlyLowStock.value = !_inventoryOnlyLowStock.value
    }

    val filteredInventoryItems: StateFlow<List<InventoryItem>> = combine(
        allItems,
        _inventorySearch,
        _inventoryCategoryFilter,
        _inventoryOnlyThirdParty,
        _inventoryOnlyLowStock
    ) { items, search, category, onlyThirdParty, onlyLowStock ->
        items.filter { item ->
            val matchesSearch = search.isBlank() ||
                    item.name.contains(search, ignoreCase = true) ||
                    item.code.contains(search, ignoreCase = true) ||
                    item.category.contains(search, ignoreCase = true)

            val matchesCategory = category == "TODOS" || item.category.equals(category, ignoreCase = true)

            val matchesThirdParty = !onlyThirdParty || item.thirdPartyCustody > 0

            val matchesLowStock = !onlyLowStock || item.currentStock <= (item.targetStock / 2)

            matchesSearch && matchesCategory && matchesThirdParty && matchesLowStock
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Manual Stock Adjustment Dialog
    private val _adjustingItem = MutableStateFlow<InventoryItem?>(null)
    val adjustingItem: StateFlow<InventoryItem?> = _adjustingItem.asStateFlow()

    fun startAdjustingItem(item: InventoryItem) {
        _adjustingItem.value = item
    }

    fun cancelAdjustingItem() {
        _adjustingItem.value = null
    }

    fun saveManualAdjustment(itemId: Long, newCurrentStock: Int, newThirdPartyCustody: Int) {
        viewModelScope.launch {
            try {
                repository.updateStockManual(itemId, newCurrentStock, newThirdPartyCustody)
                postMessage("Estoque atualizado manualmente com sucesso.")
                _adjustingItem.value = null
            } catch (e: Exception) {
                postMessage("Erro ao atualizar estoque: ${e.message}")
            }
        }
    }

    // ==========================================
    // 4. CARGA DE DADOS (IMPORT / EXPORT / RESET)
    // ==========================================
    fun loadAdtMasterCatalog() {
        viewModelScope.launch {
            try {
                repository.loadAdtSampleCatalog()
                postMessage("Catálogo Técnico ADT carregado com sucesso!")
            } catch (e: Exception) {
                postMessage("Erro ao carregar catálogo: ${e.message}")
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            try {
                repository.resetDatabase()
                postMessage("Banco de dados local zerado com sucesso.")
            } catch (e: Exception) {
                postMessage("Erro ao zerar banco: ${e.message}")
            }
        }
    }

    suspend fun getExportJsonString(): String {
        return repository.exportDataJson(allItems.value, allMovements.value)
    }

    suspend fun getExportCsvString(): String {
        return repository.exportInventoryCsv(allItems.value)
    }

    fun importJsonData(jsonString: String) {
        viewModelScope.launch {
            try {
                val count = repository.importDataJson(jsonString)
                if (count > 0) {
                    postMessage("Carga concluída: $count itens importados/atualizados.")
                } else {
                    postMessage("Nenhum item válido encontrado no JSON.")
                }
            } catch (e: Exception) {
                postMessage("Erro na importação JSON: ${e.message}")
            }
        }
    }

    fun addNewClient(name: String, code: String, segment: String, address: String, phone: String) {
        viewModelScope.launch {
            try {
                repository.insertClient(
                    Client(
                        code = code.ifBlank { "CLI-${(1000..9999).random()}" },
                        name = name,
                        segment = segment,
                        address = address,
                        phone = phone
                    )
                )
                postMessage("Cliente $name cadastrado!")
            } catch (e: Exception) {
                postMessage("Erro ao cadastrar cliente: ${e.message}")
            }
        }
    }
}
