package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.IsicDatabase
import com.example.data.model.Client
import com.example.data.model.InventoryItem
import com.example.data.model.ItemCondition
import com.example.data.model.StockMovement
import com.example.data.repository.IsicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IsicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = IsicDatabase.getDatabase(application)
    private val repository = IsicRepository(database)
    private val sharedPrefs = application.getSharedPreferences("isic_prefs", Context.MODE_PRIVATE)

    val allItems: Flow<List<InventoryItem>> = repository.allItems
    val allClients: Flow<List<Client>> = repository.allClients
    val allMovements: Flow<List<StockMovement>> = repository.allMovements
    val recentMovements: Flow<List<StockMovement>> = repository.recentMovements

    val totalCurrentStock = repository.totalCurrentStock.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val totalThirdPartyCustody = repository.totalThirdPartyCustody.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    fun getTechnicianName(): String {
        return sharedPrefs.getString("technician_name", "Rogério Ackson Santos") ?: "Rogério Ackson Santos"
    }

    fun saveTechnicianName(name: String) {
        sharedPrefs.edit().putString("technician_name", name.ifBlank { "Rogério Ackson Santos" }).apply()
    }

    fun searchItems(query: String): Flow<List<InventoryItem>> = repository.searchItems(query)
    fun searchClients(query: String): Flow<List<Client>> = repository.searchClients(query)

    suspend fun getItemByCode(code: String): InventoryItem? = repository.getItemByCode(code)
    suspend fun getItemsForMovement(movementId: Long) = repository.getItemsForMovement(movementId)
    suspend fun getSummaryCounts() = repository.getSummaryCounts()

    fun registerExit(
        clientName: String,
        osNumber: String,
        notes: String,
        itemsToExit: List<Pair<InventoryItem, Int>>,
        onComplete: (Long) -> Unit
    ) {
        val techName = getTechnicianName()
        viewModelScope.launch {
            val id = repository.registerExit(clientName, techName, osNumber, notes, itemsToExit)
            onComplete(id)
        }
    }

    fun registerAdReposition(
        osOrReference: String,
        notes: String,
        itemsToReturn: List<Triple<InventoryItem, Int, ItemCondition>>,
        onComplete: (Long) -> Unit
    ) {
        val techName = getTechnicianName()
        viewModelScope.launch {
            val id = repository.registerAdReposition(techName, osOrReference, notes, itemsToReturn)
            onComplete(id)
        }
    }

    fun updateStockManual(itemId: Long, currentStock: Int, thirdPartyCustody: Int) {
        viewModelScope.launch {
            repository.updateStockManual(itemId, currentStock, thirdPartyCustody)
        }
    }

    fun insertItem(item: InventoryItem, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertItem(item)
            onComplete()
        }
    }

    fun updateItem(item: InventoryItem, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateItem(item)
            onComplete()
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun insertClient(client: Client, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertClient(client)
            onComplete()
        }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            repository.resetDatabase()
        }
    }

    suspend fun exportJsonBackup(): String {
        val items = mutableListOf<InventoryItem>() // Poderia coletar do fluxo se necessário
        val movements = mutableListOf<StockMovement>()
        return repository.exportDataJson(items, movements)
    }

    suspend fun importJsonData(json: String): Int {
        return repository.importDataJson(json)
    }

    suspend fun importCsvData(csvString: String): Int {
        return repository.importInventoryCsv(csvString)
    }

    suspend fun exportCsvData(): String {
        // Coleta os itens atuais para exportar
        val items = database.inventoryDao().getAllItemsList()
        return repository.exportInventoryCsv(items)
    }
}
