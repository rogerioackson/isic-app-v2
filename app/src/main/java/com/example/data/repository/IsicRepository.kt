package com.example.data.repository

import android.content.Context
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
        val finalTech = technicianName.ifBlank { "Rogério Ackson Santos" }

        val movement = StockMovement(
            movementType = MovementType.SAIDA,
            osNumber = osNumber.ifBlank { "OS-${System.currentTimeMillis() % 100000}" },
            clientName = clientName,
            technicianName = finalTech,
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
        val finalTech = technicianName.ifBlank { "Rogério Ackson Santos" }

        val movement = StockMovement(
            movementType = MovementType.REPOSICAO_AD,
            osNumber = osOrReference.ifBlank { "REP-${System.currentTimeMillis() % 100000}" },
            clientName = "Almoxarifado Central ADT",
            technicianName = finalTech,
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
                location = obj.optString("location", "Almoxarifado")
            )
            importedItems.add(item)
        }

        if (importedItems.isNotEmpty()) {
            inventoryDao.insertItems(importedItems)
        }
        return importedItems.size
    }

    /**
     * Importação direta via CSV (separado por ponto e vírgula).
     * Colunas esperadas: CODIGO;NOME;CATEGORIA;UNIDADE;ESTOQUE_ATUAL;PODER_TERCEIRO;META_ESTOQUE;LOCALIZACAO
     */
    suspend fun importInventoryCsv(csvString: String): Int {
        val lines = csvString.lines()
        if (lines.isEmpty()) return 0
        
        val importedItems = mutableListOf<InventoryItem>()
        var startIndex = 0

        if (lines[0].uppercase().contains("CODIGO") || lines[0].uppercase().contains("NOME")) {
            startIndex = 1
        }

        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue

            val cols = line.split(";")
            if (cols.size >= 2) {
                val code = cols[0].trim().ifBlank { "ADT-${System.currentTimeMillis() % 10000}" }
                val name = cols[1].trim()
                val category = if (cols.size > 2) cols[2].trim().ifBlank { "Poder de Terceiros" } else "Poder de Terceiros"
                val unit = if (cols.size > 3) cols[3].trim().ifBlank { "UN" } else "UN"
                val currentStock = if (cols.size > 4) cols[4].trim().toIntOrNull() ?: 0 else 0
                val thirdPartyCustody = if (cols.size > 5) cols[5].trim().toIntOrNull() ?: 0 else 0
                val targetStock = if (cols.size > 7) cols[7].trim().toIntOrNull() ?: maxOf(10, thirdPartyCustody) else maxOf(10, thirdPartyCustody)
                val location = if (cols.size > 8) cols[8].trim().ifBlank { "Poder de Terceiros" } else "Poder de Terceiros"

                importedItems.add(
                    InventoryItem(
                        code = code,
                        name = name,
                        category = category,
                        unit = unit,
                        currentStock = currentStock,
                        thirdPartyCustody = thirdPartyCustody,
                        targetStock = targetStock,
                        location = location
                    )
                )
            }
        }

        if (importedItems.isNotEmpty()) {
            inventoryDao.insertItems(importedItems)
        }
        return importedItems.size
    }

    suspend fun exportInventoryCsv(items: List<InventoryItem>): String {
        val sb = StringBuilder()
        sb.append("CODIGO;NOME;CATEGORIA;UNIDADE;ESTOQUE_ATUAL;PODER_TERCEIRO;TOTAL_FISICO;META_ESTOQUE;LOCALIZACAO\n")
        for (it in items) {
            sb.append("${it.code};${it.name};${it.category};${it.unit};${it.currentStock};${it.thirdPartyCustody};${it.totalPhysical};${it.targetStock};${it.location}\n")
        }
        return sb.toString()
    }
}
