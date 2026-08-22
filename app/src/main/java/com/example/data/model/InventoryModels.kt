package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [Index(value = ["code"], unique = true)]
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val category: String,
    val unit: String = "UN",
    val currentStock: Int = 0, // Almoxarifado / Disponível
    val thirdPartyCustody: Int = 0, // Poder de Terceiro (Com Técnicos / Clientes em campo)
    val targetStock: Int = 10, // Meta / Estoque Mínimo Padrão
    val totalOutCount: Int = 0, // Total de Saídas acumuladas
    val totalReturnCount: Int = 0, // Total de Reposições/Retornos
    val lastMovementTimestamp: Long = System.currentTimeMillis(),
    val location: String = "Prateleira A1",
    val description: String = ""
) {
    val totalPhysical: Int
        get() = currentStock + thirdPartyCustody

    val stockDifference: Int
        get() = (currentStock + thirdPartyCustody) - targetStock

    val utilizationRatePercent: Int
        get() = if (targetStock > 0) {
            ((totalOutCount.toFloat() / targetStock.toFloat()) * 100).toInt().coerceAtMost(999)
        } else {
            0
        }
}

@Entity(
    tableName = "clients",
    indices = [Index(value = ["code"], unique = true)]
)
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val contractNumber: String = "",
    val segment: String = "Comercial", // Comercial, Residencial, Condomínio, Industrial
    val address: String = "",
    val contactName: String = "",
    val phone: String = ""
)

enum class MovementType {
    SAIDA,          // Saída para OS / Cliente
    REPOSICAO_AD,   // Retorno / Devolução do Técnico AD ao Estoque
    AJUSTE_BALANCO  // Ajuste de inventário físico
}

enum class ItemCondition(val label: String) {
    BOM_ESTADO("Bom Estado (Retorna ao Estoque)"),
    COM_DEFEITO("Com Defeito (Assistência / Garantia)"),
    SUCATA("Sucata / Danificado"),
    INSTALADO_CLIENTE("Instalado em Definitivo (Baixa do Poder)")
}

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val movementType: MovementType,
    val osNumber: String,
    val clientName: String,
    val technicianName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalItemsCount: Int = 0,
    val notes: String = "",
    val status: String = "CONCLUÍDO"
)

@Entity(
    tableName = "movement_items",
    foreignKeys = [
        ForeignKey(
            entity = StockMovement::class,
            parentColumns = ["id"],
            childColumns = ["movementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("movementId")]
)
data class MovementItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val movementId: Long,
    val itemCode: String,
    val itemName: String,
    val quantity: Int,
    val condition: ItemCondition = ItemCondition.BOM_ESTADO,
    val serialNumber: String = ""
)
