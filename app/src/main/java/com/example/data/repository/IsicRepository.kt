package com.example.data.repository

import com.example.data.database.IsicDatabase
import com.example.data.model.Client
import com.example.data.model.InventoryItem
import com.example.data.model.ItemCondition
import com.example.data.model.MovementItem
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class IsicRepository(private val database: IsicDatabase) {

    private val inventoryDao = database.inventoryDao()
    private val clientDao = database.clientDao()
    private val movementDao = database.movementDao()

    val allItems: Flow<List<InventoryItem>> = inventoryDao.getAllItemsFlow()
    val allClients: Flow<List<Client>> = clientDao.getAllClientsFlow()
    val allMovements: Flow<List<StockMovement>> = movementDao.getAllMovementsFlow()
    val recentMovements: Flow<List<StockMovement>> = movementDao.getRecentMovementsFlow()

    val totalCurrentStock: Flow<Int?> = inventoryDao.getTotalCurrentStockFlow()
    val totalThirdPartyCustody: Flow<Int?> = inventoryDao.getTotalThirdPartyCustodyFlow()

    fun searchItems(query: String): Flow<List<InventoryItem>> = inventoryDao.searchItemsFlow(query)
    fun searchClients(query: String): Flow<List<Client>> = clientDao.searchClientsFlow(query)

    suspend fun getItemByCode(code: String): InventoryItem? = inventoryDao.getItemByCode(code)
    suspend fun getItemsForMovement(movementId: Long): List<MovementItem> = movementDao.getItemsForMovement(movementId)

    suspend fun getSummaryCounts(): Triple<Int, Int, Int> {
        val items = inventoryDao.getItemsCount()
        val clients = clientDao.getClientsCount()
        val movements = movementDao.getMovementsCount()
        return Triple(items, clients, movements)
    }

    suspend fun registerExit(
        clientName: String,
        technicianName: String,
        osNumber: String,
        notes: String,
        itemsToExit: List<Pair<InventoryItem, Int>>
    ): Long {
        val timestamp = System.currentTimeMillis()
        val totalCount = itemsToExit.sumOf { it.second }

        val movement = StockMovement(
            movementType = MovementType.SAIDA,
            osNumber = osNumber.ifBlank { "OS-${System.currentTimeMillis() % 100000}" },
            clientName = clientName,
            technicianName = technicianName,
            timestamp = timestamp,
            totalItemsCount = totalCount,
            notes = notes,
            status = "CONCLUÍDO"
        )

        val movementId = movementDao.insertMovement(movement)

        val movementItems = itemsToExit.map { (item, qty) ->
            MovementItem(
                movementId = movementId,
                itemCode = item.code,
                itemName = item.name,
                quantity = qty,
                condition = ItemCondition.BOM_ESTADO
            )
        }
        movementDao.insertMovementItems(movementItems)

        for ((item, qty) in itemsToExit) {
            inventoryDao.registerItemExit(item.code, qty, timestamp)
        }

        return movementId
    }

    suspend fun registerAdReposition(
        technicianName: String,
        osOrReference: String,
        notes: String,
        itemsToReturn: List<Triple<InventoryItem, Int, ItemCondition>>
    ): Long {
        val timestamp = System.currentTimeMillis()
        val totalCount = itemsToReturn.sumOf { it.second }

        val movement = StockMovement(
            movementType = MovementType.REPOSICAO_AD,
            osNumber = osOrReference.ifBlank { "REP-${System.currentTimeMillis() % 100000}" },
            clientName = "Almoxarifado ADT Central",
            technicianName = technicianName,
            timestamp = timestamp,
            totalItemsCount = totalCount,
            notes = notes,
            status = "CONCLUÍDO"
        )

        val movementId = movementDao.insertMovement(movement)

        val movementItems = itemsToReturn.map { (item, qty, condition) ->
            MovementItem(
                movementId = movementId,
                itemCode = item.code,
                itemName = item.name,
                quantity = qty,
                condition = condition
            )
        }
        movementDao.insertMovementItems(movementItems)

        for ((item, qty, condition) in itemsToReturn) {
            when (condition) {
                ItemCondition.BOM_ESTADO, ItemCondition.COM_DEFEITO, ItemCondition.SUCATA -> {
                    inventoryDao.registerItemReturnToStock(item.code, qty, timestamp)
                }
                ItemCondition.INSTALADO_CLIENTE -> {
                    inventoryDao.registerItemInstalledAtClient(item.code, qty, timestamp)
                }
            }
        }

        return movementId
    }

    suspend fun updateStockManual(itemId: Long, currentStock: Int, thirdPartyCustody: Int) {
        inventoryDao.updateStockManual(itemId, currentStock, thirdPartyCustody, System.currentTimeMillis())
    }

    suspend fun insertItem(item: InventoryItem): Long = inventoryDao.insertItem(item)
    suspend fun updateItem(item: InventoryItem) = inventoryDao.updateItem(item)
    suspend fun deleteItem(item: InventoryItem) = inventoryDao.deleteItem(item)

    suspend fun insertClient(client: Client): Long = clientDao.insertClient(client)

    /**
     * Catálogo inicial de materiais ADT para o inventário técnico.
     */
    suspend fun loadAdtSampleCatalog() {
        val count = inventoryDao.getItemsCount()
        if (count == 0) {
            val defaultItems = listOf(
                InventoryItem(code = "CNT-01", name = "Central de Alarme ADT", category = "Centrais", unit = "UN", currentStock = 5, targetStock = 10, location = "Almoxarifado"),
                InventoryItem(code = "SNS-01", name = "Sensor Infravermelho (IVP)", category = "Sensores", unit = "UN", currentStock = 15, targetStock = 20, location = "Almoxarifado"),
                InventoryItem(code = "MAG-01", name = "Contato Magnético de Abertura", category = "Acessórios", unit = "UN", currentStock = 30, targetStock = 40, location = "Almoxarifado"),
                InventoryItem(code = "SRN-01", name = "Sirene Piezoelétrica", category = "Acessórios", unit = "UN", currentStock = 8, targetStock = 12, location = "Almoxarifado"),
                InventoryItem(code = "BAT-01", name = "Bateria Selada 12V 7A", category = "Alimentação", unit = "UN", currentStock = 10, targetStock = 15, location = "Almoxarifado")
            )
            inventoryDao.insertItems(defaultItems)
        }
    }

    suspend fun resetDatabase() {
        inventoryDao.clearAllItems()
        clientDao.clearClients()
        movementDao.clearAllMovements()
    }

    suspend fun exportDataJson(items: List<InventoryItem>, movements: List<StockMovement>): String {
        val root = JSONObject()
        root.put("app", "iSiC - ADT Controle Técnico")
        root.put("exportedAt", System.currentTimeMillis())

        val itemsArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("code", item.code)
            obj.put("name", item.name)
            obj.put("category", item.category)
            obj.put("unit", item.unit)
            obj.put("currentStock", item.currentStock)
            obj.put("thirdPartyCustody", item.thirdPartyCustody)
            obj.put("targetStock", item.targetStock)
            obj.put("totalOutCount", item.totalOutCount)
            obj.put("totalReturnCount", item.totalReturnCount)
            obj.put("location", item.location)
            itemsArray.put(obj)
        }
        root.put("items", itemsArray)

        val movementsArray = JSONArray()
        for (mov in movements) {
            val obj = JSONObject()
            obj.put("movementType", mov.movementType.name)
            obj.put("osNumber", mov.osNumber)
            obj.put("clientName", mov.clientName)
            obj.put("technicianName", mov.technicianName)
            obj.put("timestamp", mov.timestamp)
            obj.put("totalItemsCount", mov.totalItemsCount)
            obj.put("notes", mov.notes)
            movementsArray.put(obj)
        }
        root.put("movements", movementsArray)

        return root.toString(2)
    }

    suspend fun importDataJson(jsonString: String): Int {
        val root = JSONObject(jsonString)
        val itemsArray = root.optJSONArray("items") ?: return 0
        val importedItems = mutableListOf<InventoryItem>()

        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            val item = InventoryItem(
                code = obj.getString("code"),
                name = obj.getString("name"),
                category = obj.optString("category", "Geral"),
                unit = obj.optString("unit", "UN"),
                currentStock = obj.optInt("currentStock", 0),
                thirdPartyCustody = obj.optInt("thirdPartyCustody", 0),
                targetStock = obj.optInt("targetStock", 10),
                totalOutCount = obj.optInt("totalOutCount", 0),
                totalReturnCount = obj.optInt("totalReturnCount", 0),
                location = obj.optString("location", "Almoxarifado"),
                description = obj.optString("description", "")
            )
            importedItems.add(item)
        }

        if (importedItems.isNotEmpty()) {
            inventoryDao.insertItems(importedItems)
        }
        return importedItems.size
    }

    suspend fun exportInventoryCsv(items: List<InventoryItem>): String {
        val sb = StringBuilder()
        sb.append("CODIGO;NOME;CATEGORIA;UNIDADE;ESTOQUE_ATUAL;PODER_TERCEIRO;TOTAL_FISICO;META_ESTOQUE;DIFERENCA;TOTAL_SAIDAS;TOTAL_RETORNOS;TAXA_UTILIZACAO_PCT;LOCALIZACAO\n")
        for (it in items) {
            sb.append("${it.code};${it.name};${it.category};${it.unit};${it.currentStock};${it.thirdPartyCustody};${it.totalPhysical};${it.targetStock};${it.stockDifference};${it.totalOutCount};${it.totalReturnCount};${it.utilizationRatePercent}%;${it.location}\n")
        }
        return sb.toString()
    }
}
