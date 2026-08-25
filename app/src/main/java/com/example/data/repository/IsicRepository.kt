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
            technicianName = technicianName.ifBlank { "Rogério Ackson Santos" },
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
            clientName = "Almoxarifado Central ADT",
            technicianName = technicianName.ifBlank { "Rogério Ackson Santos" },
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
     * Catálogo Mestre ADT com os itens em Poder de Terceiros do Rogério.
     */
    suspend fun loadAdtSampleCatalog() {
        val rawData = listOf(
            Triple("2X PACK BATERIA DE LITHIUM 3,6V / 3500MAH", 1, 2),
            Triple("BALUN COM BORNE E RABICHO", 2, 3),
            Triple("BATERIA ALCALINA TIPO D – 1,5V - 2UN – BX-32", 1, 1),
            Triple("BATERIA CR123A - 3V", 20, 92),
            Triple("BATERIA CR2032 - 3V", 1, 4),
            Triple("BATERIA DE LITIO 3V CR2477", 3, 4),
            Triple("BATERIA ESTACIONARIA - 12V 7AH", 5, 4),
            Triple("BATERIA LI-ION - 3,7V / 1000MA/H - PM360", 2, 2),
            Triple("BATERIA LI-METAL 2GPCR123A 3V - NEXT CAM", 1, 1),
            Triple("BATERIA LITHIUM 1,5V AA -PACOTE C 2 UNID", 6, 10),
            Triple("BATERIA NIMH 4.8V/1.3AH - PM10 E RP-600", 3, 3),
            Triple("BOTAO PANICO FIXO COM FIO NF E PROTETOR", 1, 2),
            Triple("CAM CUBE IR IP WIRELESS2MP INT-ADCV522IR", 1, 4),
            Triple("CAMERA HD WIFI SPIDER - DENOX", 3, 3),
            Triple("CAMERA V515", 0, 8),
            Triple("CAMERA V723", 0, 3),
            Triple("CAMERA V724", 0, 10),
            Triple("CENTRAL DE CHOQUE - CP8000V12", 1, 1),
            Triple("CONTROLE REMOTO - 5834-4", 1, 1),
            Triple("CONTROLE REMOTO - DENOX 3 FUNCOES", 2, 1),
            Triple("CONTROLE REMOTO - RC-29", 4, 18),
            Triple("CONTROLE REMOTO - WS4939", 2, 3),
            Triple("CONTROLE REMOTO - XAC 4000 SMART", 1, 1),
            Triple("CONTROLE REMOTO SEM BEEP - KF-234 PG2", 3, 10),
            Triple("CXA PLASTICA - 102X102X55MM IP55 SSX111", 1, 6),
            Triple("FONTE BI VOLT INTELBRAS", 0, 6),
            Triple("FONTE BIVOLT - 12VDC 2A - 2118", 3, 2),
            Triple("FONTE BIVOLT INT CP8000V12 - 13.5V-0.4A", 1, 1),
            Triple("MODULO 4G IP - VIAWEB", 4, 3),
            Triple("MODULO ETH/IP - POWERLINK3", 1, 11),
            Triple("MODULO GPRS - GSM-350 PG2", 2, 3),
            Triple("MODULO GPRS - VIAWEB", 6, 0),
            Triple("MODULO GPRS E ETHERNET - VIAWEB", 3, 0),
            Triple("MODULO GPRS NANOCOMM 3G COM ANTENA", 1, 1),
            Triple("PAINEL DE ALARME - DENOX", 1, 0),
            Triple("PAINEL DE ALARME - HSGW-G2", 0, 6),
            Triple("PAINEL DE ALARME - HSGW-G8", 1, 4),
            Triple("PILHA ALCALINA 1,5V - AAA", 6, 6),
            Triple("PM 10 TRIPLE", 0, 4),
            Triple("PM 360", 0, 3),
            Triple("PM 360R", 0, 1),
            Triple("PM10", 0, 1),
            Triple("RECEPTOR SEM FIO - RF5132-433", 2, 3),
            Triple("RECEPTOR SEM FIO - XAR 4000 SMART", 1, 1),
            Triple("REPETIDOR RP~600", 0, 7),
            Triple("SD CARD", 0, 7),
            Triple("SES DE CORRENTE A2A P3 CONTATO SECO", 1, 2),
            Triple("SIRENE COM FIO - 120DB BRANCA", 2, 7),
            Triple("SIRENE INTERNA S/FIO C/BAT - SR-720B PG2", 1, 8),
            Triple("SIRENE INTERNA/EXTERNA SEM FIO - BX-32", 2, 8),
            Triple("SIV COM FOTO SEM FIO - NEXT CAM K9 PG2", 1, 1),
            Triple("SIV COM MICROONDAS SEM FIO - 5898", 1, 1),
            Triple("SIV EXTERNO SEM FIO - EIR-32", 4, 9),
            Triple("SIV EXTERNO SEM FIO - TOWER 20AM PG2", 1, 8),
            Triple("SIV MICRO-ONDAS COM FIO - LC-104-PIMW", 3, 4),
            Triple("SIV MICRO-ONDAS S/ FIO-TOWER32AMK9-90PG2", 1, 7),
            Triple("SIV MICRO-ONDAS SEM FIO - IRMP-23B-SL", 4, 14),
            Triple("SIV SEM FIO - 5800PIR-COM", 2, 3),
            Triple("SIV SEM FIO - DENOX", 3, 1),
            Triple("SIV SEM FIO - DENOX", 1, 8),
            Triple("SIV SEM FIO - IRP-16SL", 1, 2),
            Triple("SIV SEM FIO - IVP 4101 PET SMART", 2, 2),
            Triple("SIV SEM FIO - MP-802 K9-85 PG2", 2, 5),
            Triple("SIV SEM FIO - NEXT PG2", 3, 1),
            Triple("SMA 4945", 0, 1),
            Triple("SMA COM FIO - VIP130", 2, 1),
            Triple("SMA GRANDE COM FIO - SM20WG", 1, 1),
            Triple("SMA PORTA RETRATIL C/F -XASPORTADE ACOSP", 3, 3),
            Triple("SMA SEM FIO - 5816", 1, 4),
            Triple("SMA SEM FIO - DENOX", 3, 4),
            Triple("SMA SEM FIO - DENOX", 1, 2),
            Triple("SMA SEM FIO SLIM - MDC-3", 1, 13),
            Triple("SMA SEM FIO TRANSMISSOR - DC-23", 1, 20),
            Triple("SMA SEM FIO TRANSMISSOR - WS4925", 1, 1),
            Triple("SMA SLIM SEM FIO - EV-DW4975", 2, 1),
            Triple("SMA XAS 4010", 0, 3),
            Triple("TECLADO KP 241", 0, 1),
            Triple("TECLADO LCD 64 SETORES - PK5501ADT", 2, 1),
            Triple("TECLADO SEM FIO - KP-23B", 2, 8),
            Triple("TRANSFORMADOR - 16VAC 40VA", 1, 1),
            Triple("TRANSMISSOR SEM FIO - TX 4020 SMART", 1, 4),
            Triple("BATERIA CR 2450", 0, 28)
        )

        for ((index, data) in rawData.withIndex()) {
            val code = "ADT-%03d".format(index + 1)
            val existing = inventoryDao.getItemByCode(code)
            if (existing == null) {
                val item = InventoryItem(
                    code = code,
                    name = data.first,
                    category = "Poder de Terceiros",
                    unit = "UN",
                    currentStock = 0,
                    thirdPartyCustody = data.third,
                    targetStock = maxOf(10, data.third),
                    location = "Poder de Terceiros - Rogério"
                )
                inventoryDao.insertItem(item)
            }
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
     * Importação direta de dados via formato CSV (compatível com planilhas Excel / ponto e vírgula).
     * Formato esperado das colunas: CODIGO;NOME;CATEGORIA;UNIDADE;ESTOQUE_ATUAL;PODER_TERCEIRO;META_ESTOQUE;LOCALIZACAO
     */
    suspend fun importInventoryCsv(csvString: String): Int {
        val lines = csvString.lines()
        if (lines.isEmpty()) return 0
        
        val importedItems = mutableListOf<InventoryItem>()
        var startIndex = 0

        // Se a primeira linha for cabeçalho, pula
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
