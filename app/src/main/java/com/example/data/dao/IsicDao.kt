package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Client
import com.example.data.model.InventoryItem
import com.example.data.model.MovementItem
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY category ASC, name ASC")
    fun getAllItemsFlow(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchItemsFlow(query: String): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE code = :code LIMIT 1")
    suspend fun getItemByCode(code: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>)

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items")
    suspend fun clearAllItems()

    @Query("SELECT COUNT(*) FROM inventory_items")
    suspend fun getItemsCount(): Int

    @Query("SELECT SUM(currentStock) FROM inventory_items")
    fun getTotalCurrentStockFlow(): Flow<Int?>

    @Query("SELECT SUM(thirdPartyCustody) FROM inventory_items")
    fun getTotalThirdPartyCustodyFlow(): Flow<Int?>

    @Query("UPDATE inventory_items SET currentStock = currentStock - :qty, thirdPartyCustody = thirdPartyCustody + :qty, totalOutCount = totalOutCount + :qty, lastMovementTimestamp = :timestamp WHERE code = :code")
    suspend fun registerItemExit(code: String, qty: Int, timestamp: Long)

    @Query("UPDATE inventory_items SET currentStock = currentStock + :qty, thirdPartyCustody = CASE WHEN thirdPartyCustody >= :qty THEN thirdPartyCustody - :qty ELSE 0 END, totalReturnCount = totalReturnCount + :qty, lastMovementTimestamp = :timestamp WHERE code = :code")
    suspend fun registerItemReturnToStock(code: String, qty: Int, timestamp: Long)

    @Query("UPDATE inventory_items SET thirdPartyCustody = CASE WHEN thirdPartyCustody >= :qty THEN thirdPartyCustody - :qty ELSE 0 END, lastMovementTimestamp = :timestamp WHERE code = :code")
    suspend fun registerItemInstalledAtClient(code: String, qty: Int, timestamp: Long)

    @Query("UPDATE inventory_items SET currentStock = :newStock, thirdPartyCustody = :newThirdParty, lastMovementTimestamp = :timestamp WHERE id = :itemId")
    suspend fun updateStockManual(itemId: Long, newStock: Int, newThirdParty: Int, timestamp: Long)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClientsFlow(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchClientsFlow(query: String): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<Client>)

    @Query("DELETE FROM clients")
    suspend fun clearClients()

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getClientsCount(): Int
}

@Dao
interface MovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovementsFlow(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE movementType = :type ORDER BY timestamp DESC")
    fun getMovementsByTypeFlow(type: MovementType): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT 5")
    fun getRecentMovementsFlow(): Flow<List<StockMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovementItems(items: List<MovementItem>)

    @Query("SELECT * FROM movement_items WHERE movementId = :movementId")
    suspend fun getItemsForMovement(movementId: Long): List<MovementItem>

    @Query("DELETE FROM stock_movements")
    suspend fun clearAllMovements()

    @Query("SELECT COUNT(*) FROM stock_movements")
    suspend fun getMovementsCount(): Int
}
