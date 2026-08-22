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

    /**
     * 1. Registrar Saída
     * Decreases Almoxarifado stock, increases Poder de Terceiro, and records the movement
     */
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

    /**
     * 2. Reposição AD (Retorno de Materiais)
     * Handles returning items back to Almoxarifado or marking as permanently installed in client
     */
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
     * 4. Carga de Dados (Pre-population and Catalog Init)
     */
    suspend fun loadAdtSampleCatalog() {
        val defaultItems = listOf(
            // Centrais & Teclados
            InventoryItem(
                code = "ADT-CTR-V48",
                name = "Central de Alarme Vista 48D Honeywell",
                category = "Centrais & Teclados",
                unit = "UN",
                currentStock = 8,
                thirdPartyCustody = 4,
                targetStock = 15,
                totalOutCount = 12,
                totalReturnCount = 5,
                location = "Almox A - Prateleira 1",
                description = "Central microprocessada 8 a 48 zonas compatível com monitoramento ADT"
            ),
            InventoryItem(
                code = "ADT-CTR-2018",
                name = "Central Intelbras AMT 2018 E/EG com IP/GPRS",
                category = "Centrais & Teclados",
                unit = "UN",
                currentStock = 12,
                thirdPartyCustody = 6,
                targetStock = 20,
                totalOutCount = 18,
                totalReturnCount = 7,
                location = "Almox A - Prateleira 1",
                description = "Central de alarme monitorada com 18 zonas e módulo 4G acoplado"
            ),
            InventoryItem(
                code = "ADT-CTR-PC18",
                name = "Central DSC PowerSeries PC1832 8-32 Zonas",
                category = "Centrais & Teclados",
                unit = "UN",
                currentStock = 5,
                thirdPartyCustody = 3,
                targetStock = 10,
                totalOutCount = 9,
                totalReturnCount = 2,
                location = "Almox A - Prateleira 2",
                description = "Painel modular DSC alta confiabilidade para médios e grandes clientes"
            ),
            InventoryItem(
                code = "ADT-TEC-6160",
                name = "Teclado Alfanumérico Honeywell 6160CR Custom",
                category = "Centrais & Teclados",
                unit = "UN",
                currentStock = 10,
                thirdPartyCustody = 5,
                targetStock = 18,
                totalOutCount = 15,
                totalReturnCount = 4,
                location = "Almox A - Prateleira 3",
                description = "Teclado com display em português e teclas de emergência rápida"
            ),
            InventoryItem(
                code = "ADT-TEC-LCD4",
                name = "Teclado LCD Intelbras XAT 4000 TFT Touch",
                category = "Centrais & Teclados",
                unit = "UN",
                currentStock = 14,
                thirdPartyCustody = 4,
                targetStock = 20,
                totalOutCount = 16,
                totalReturnCount = 6,
                location = "Almox A - Prateleira 3",
                description = "Teclado capacitivo com iluminação azul e feedback sonoro"
            ),

            // Sensores & Detectores
            InventoryItem(
                code = "ADT-SEN-IVP3",
                name = "Sensor IVP 3000 Pet Imunidade 20kg",
                category = "Sensores & Detectores",
                unit = "UN",
                currentStock = 42,
                thirdPartyCustody = 15,
                targetStock = 60,
                totalOutCount = 78,
                totalReturnCount = 22,
                location = "Almox B - Gaveteiro 1",
                description = "Detector de movimento infravermelho com compensação automática de temperatura"
            ),
            InventoryItem(
                code = "ADT-SEN-DUAL",
                name = "Detector Dupla Tecnologia Micro-ondas + IVP DT-7235",
                category = "Sensores & Detectores",
                unit = "UN",
                currentStock = 18,
                thirdPartyCustody = 12,
                targetStock = 35,
                totalOutCount = 44,
                totalReturnCount = 10,
                location = "Almox B - Gaveteiro 2",
                description = "Detector imune a falsos disparos com sensor micro-ondas integrado"
            ),
            InventoryItem(
                code = "ADT-SEN-MAGN",
                name = "Sensor Magnético Metálico Sobrepor Porta de Aço",
                category = "Sensores & Detectores",
                unit = "UN",
                currentStock = 75,
                thirdPartyCustody = 20,
                targetStock = 100,
                totalOutCount = 130,
                totalReturnCount = 35,
                location = "Almox B - Gaveteiro 3",
                description = "Blindado para portas de enrolar, portões e galpões industriais"
            ),
            InventoryItem(
                code = "ADT-SEN-CORT",
                name = "Sensor Ativo de Barreira IVA 3110 Duplo Feixe 100m",
                category = "Sensores & Detectores",
                unit = "PAR",
                currentStock = 6,
                thirdPartyCustody = 7,
                targetStock = 15,
                totalOutCount = 18,
                totalReturnCount = 3,
                location = "Almox B - Prateleira 4",
                description = "Par transmissor/receptor para perímetro externo com alinhamento óptico"
            ),
            InventoryItem(
                code = "ADT-SEN-FUMO",
                name = "Detector Óptico de Fumaça Convencional DFC 421",
                category = "Sensores & Detectores",
                unit = "UN",
                currentStock = 16,
                thirdPartyCustody = 8,
                targetStock = 25,
                totalOutCount = 32,
                totalReturnCount = 6,
                location = "Almox B - Gaveteiro 4",
                description = "Sensor de fumaça com câmara fotoelétrica para ambientes internos"
            ),
            InventoryItem(
                code = "ADT-SEN-QUEB",
                name = "Detector Acústico de Quebra de Vidro FG-1625",
                category = "Sensores & Detectores",
                unit = "UN",
                currentStock = 11,
                thirdPartyCustody = 5,
                targetStock = 20,
                totalOutCount = 21,
                totalReturnCount = 4,
                location = "Almox B - Gaveteiro 5",
                description = "Filtro de som bifásico ajustável para vitrines e fachadas de vidro"
            ),

            // CFTV & Câmeras
            InventoryItem(
                code = "ADT-CAM-IPB2",
                name = "Câmera Bullet IP HD 2MP IR 30m PoE Intelbras",
                category = "CFTV / Câmeras",
                unit = "UN",
                currentStock = 19,
                thirdPartyCustody = 9,
                targetStock = 30,
                totalOutCount = 38,
                totalReturnCount = 8,
                location = "Almox C - Prateleira 1",
                description = "Câmera externa IP67 com visão noturna infravermelho e protocolo ONVIF"
            ),
            InventoryItem(
                code = "ADT-CAM-IPD2",
                name = "Câmera Dome IP Full HD 2MP DWDR IR 30m PoE",
                category = "CFTV / Câmeras",
                unit = "UN",
                currentStock = 15,
                thirdPartyCustody = 6,
                targetStock = 25,
                totalOutCount = 29,
                totalReturnCount = 5,
                location = "Almox C - Prateleira 2",
                description = "Câmera discreta para forros e interiores com lente 2.8mm"
            ),
            InventoryItem(
                code = "ADT-DVR-08CH",
                name = "NVR Gravador Digital IP 8 Canais 4K PoE ADT",
                category = "CFTV / Câmeras",
                unit = "UN",
                currentStock = 4,
                thirdPartyCustody = 3,
                targetStock = 8,
                totalOutCount = 7,
                totalReturnCount = 1,
                location = "Almox C - Prateleira 3",
                description = "Gravador de rede com 8 portas PoE e suporte a compressão H.265+"
            ),
            InventoryItem(
                code = "ADT-FON-12V5",
                name = "Fonte Chaveada Estabilizada 12V 5A para CFTV",
                category = "CFTV / Câmeras",
                unit = "UN",
                currentStock = 28,
                thirdPartyCustody = 10,
                targetStock = 40,
                totalOutCount = 52,
                totalReturnCount = 14,
                location = "Almox C - Prateleira 4",
                description = "Fonte regulada com proteção contra surtos e curto-circuito"
            ),

            // Baterias & Energia
            InventoryItem(
                code = "ADT-BAT-1207",
                name = "Bateria Chumbo-Ácido Selada 12V 7Ah Unipower/Moura",
                category = "Baterias & Energia",
                unit = "UN",
                currentStock = 31,
                thirdPartyCustody = 16,
                targetStock = 50,
                totalOutCount = 85,
                totalReturnCount = 20,
                location = "Almox D - Prateleira Reforçada",
                description = "Bateria VRLA AGM para nobreak de centrais de alarme e sirenes"
            ),
            InventoryItem(
                code = "ADT-BAT-1204",
                name = "Bateria Estacionária 12V 4.5Ah para Centrais",
                category = "Baterias & Energia",
                unit = "UN",
                currentStock = 14,
                thirdPartyCustody = 8,
                targetStock = 25,
                totalOutCount = 30,
                totalReturnCount = 6,
                location = "Almox D - Prateleira Reforçada",
                description = "Bateria compacta para painéis auxiliares e módulos remotos"
            ),
            InventoryItem(
                code = "ADT-TRA-1650",
                name = "Transformador Bivolt 16.5VAC 40VA para Central",
                category = "Baterias & Energia",
                unit = "UN",
                currentStock = 13,
                thirdPartyCustody = 5,
                targetStock = 20,
                totalOutCount = 24,
                totalReturnCount = 5,
                location = "Almox D - Prateleira 2",
                description = "Trafo bivolt 110/220V para alimentação principal da placa"
            ),

            // Sirenes & Comunicação
            InventoryItem(
                code = "ADT-SIR-BITO",
                name = "Sirene Bitonal Eletrônica 120dB 12V Branca ADT",
                category = "Sirenes & Comunicação",
                unit = "UN",
                currentStock = 26,
                thirdPartyCustody = 11,
                targetStock = 40,
                totalOutCount = 60,
                totalReturnCount = 12,
                location = "Almox E - Prateleira 1",
                description = "Sirene potente com alta pressão sonora e baixo consumo"
            ),
            InventoryItem(
                code = "ADT-SIR-STRO",
                name = "Sirene com Flash Estroboscópico Visual LED",
                category = "Sirenes & Comunicação",
                unit = "UN",
                currentStock = 8,
                thirdPartyCustody = 4,
                targetStock = 15,
                totalOutCount = 14,
                totalReturnCount = 2,
                location = "Almox E - Prateleira 2",
                description = "Sinalizador audiovisual para postos de combustível e garagens"
            ),
            InventoryItem(
                code = "ADT-MOD-4GIP",
                name = "Módulo Comunicador Celular 4G LTE + IP ADT Cloud",
                category = "Sirenes & Comunicação",
                unit = "UN",
                currentStock = 12,
                thirdPartyCustody = 9,
                targetStock = 25,
                totalOutCount = 33,
                totalReturnCount = 6,
                location = "Almox E - Prateleira 3",
                description = "Transmissor redundancy dual-SIM para monitoramento 24h em tempo real"
            ),
            InventoryItem(
                code = "ADT-REP-RF43",
                name = "Repetidor de Sinal Sem Fio 433MHz / 868MHz",
                category = "Sirenes & Comunicação",
                unit = "UN",
                currentStock = 7,
                thirdPartyCustody = 3,
                targetStock = 12,
                totalOutCount = 11,
                totalReturnCount = 2,
                location = "Almox E - Prateleira 4",
                description = "Extensor de alcance de sensores sem fio para grandes galpões"
            ),

            // Cabos & Conexões
            InventoryItem(
                code = "ADT-CAB-4V10",
                name = "Cabo 4 Vias Multicores 100% Cobre (Rolo 100m)",
                category = "Cabos & Conectores",
                unit = "ROLO",
                currentStock = 11,
                thirdPartyCustody = 6,
                targetStock = 20,
                totalOutCount = 35,
                totalReturnCount = 8,
                location = "Almox F - Estrutura Cabos",
                description = "Cabo blindado para barramento de teclados e sensores de zona"
            ),
            InventoryItem(
                code = "ADT-CAB-UTP5",
                name = "Cabo de Rede UTP Cat5e Blindado Azul (Caixa 305m)",
                category = "Cabos & Conectores",
                unit = "CX",
                currentStock = 5,
                thirdPartyCustody = 3,
                targetStock = 10,
                totalOutCount = 14,
                totalReturnCount = 2,
                location = "Almox F - Estrutura Cabos",
                description = "Caixa de cabo homologado Anatel para infraestrutura IP"
            ),
            InventoryItem(
                code = "ADT-CON-BNC0",
                name = "Conector BNC com Mola e Parafuso (Pct c/ 50un)",
                category = "Cabos & Conectores",
                unit = "PCT",
                currentStock = 9,
                thirdPartyCustody = 4,
                targetStock = 15,
                totalOutCount = 22,
                totalReturnCount = 5,
                location = "Almox F - Gaveteiro Conexões",
                description = "Conector profissional para fixação rápida sem solda"
            )
        )

        val defaultClients = listOf(
            Client(
                code = "CLI-1001",
                name = "Condomínio Residencial Grand Parc",
                contractNumber = "ADT-CTR-2024-089",
                segment = "Condomínio",
                address = "Av. Paulista, 1420 - Cerqueira César, SP",
                contactName = "Síndico Roberto Alcantara",
                phone = "(11) 98765-4321"
            ),
            Client(
                code = "CLI-1002",
                name = "Supermercados Rede Boa Vista - Loja 03",
                contractNumber = "ADT-CTR-2023-412",
                segment = "Comercial",
                address = "Rua das Flores, 850 - Moema, SP",
                contactName = "Gerente Patrícia Lima",
                phone = "(11) 97654-3210"
            ),
            Client(
                code = "CLI-1003",
                name = "Hospital Santa Marina - Bloco Diagnóstico",
                contractNumber = "ADT-CTR-2025-104",
                segment = "Saúde / Corporativo",
                address = "Av. Brasil, 2100 - Jardins, SP",
                contactName = "Eng. Marcos Segurança",
                phone = "(11) 96543-2109"
            ),
            Client(
                code = "CLI-1004",
                name = "Centro Logístico Bandeirantes Armazém 2",
                contractNumber = "ADT-CTR-2022-771",
                segment = "Industrial",
                address = "Rodovia Anhanguera, km 22 - SP",
                contactName = "Superv. Claudio Silva",
                phone = "(11) 95432-1098"
            ),
            Client(
                code = "CLI-1005",
                name = "Residência Família Albuquerque",
                contractNumber = "ADT-CTR-2025-339",
                segment = "Residencial",
                address = "Alameda dos Ipês, 45 - Alphaville, SP",
                contactName = "Dr. Marcelo Albuquerque",
                phone = "(11) 94321-0987"
            ),
            Client(
                code = "CLI-1006",
                name = "Farmácias Vida & Saúde - Unidade Centro",
                contractNumber = "ADT-CTR-2024-518",
                segment = "Comercial",
                address = "Praça da Sé, 110 - Centro Histórico, SP",
                contactName = "Farmacêutico Chefe André",
                phone = "(11) 93210-9876"
            )
        )

        inventoryDao.insertItems(defaultItems)
        clientDao.insertClients(defaultClients)
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
