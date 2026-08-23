package com.example.isicapp.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * ==========================================
 * 1. ENTIDADES (Database Tables)
 * ==========================================
 */

@Entity(tableName = "isic_items")
data class IsicItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val code: String,          // Código do equipamento ou SKU
    val name: String,          // Nome do produto (ex: Central ADT, Sensor IVP)
    val category: String,      // Categoria (Ex: Central, Sensor, Acessório)
    val stockQuantity: Int,    // Quantidade em estoque
    val notes: String = ""     // Observações técnicas
)

@Entity(tableName = "adt_clients")
data class AdtClient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,          // Nome do cliente
    val address: String,       // Endereço da instalação
    val panelModel: String,    // Modelo do painel/central instalado
    val contactPhone: String   // Telefone de contato
)

/**
 * ==========================================
 * 2. DAOs (Data Access Objects)
 * ==========================================
 */

@Dao
interface IsicDao {
    @Query("SELECT * FROM isic_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<IsicItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: IsicItem)

    @Query("DELETE FROM isic_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("SELECT * FROM adt_clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<AdtClient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: AdtClient)

    @Query("DELETE FROM adt_clients WHERE id = :clientId")
    suspend fun deleteClient(clientId: Long)
}

/**
 * ==========================================
 * 3. DATABASE ROOM CONFIGURATION
 * ==========================================
 */

@Database(entities = [IsicItem::class, AdtClient::class], version = 1, exportSchema = false)
abstract class IsicDatabase : RoomDatabase() {
    abstract fun isicDao(): IsicDao

    companion object {
        @Volatile
        private var INSTANCE: IsicDatabase? = null

        fun getDatabase(context: Context): IsicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IsicDatabase::class.java,
                    "isic_adt_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * ==========================================
 * 4. REPOSITORY & SAMPLE DATA CLEANUP
 * ==========================================
 */

class IsicRepository(private val isicDao: IsicDao) {

    val allItems: Flow<List<IsicItem>> = isicDao.getAllItems()
    val allClients: Flow<List<AdtClient>> = isicDao.getAllClients()

    suspend fun insert(item: IsicItem) {
        isicDao.insertItem(item)
    }

    suspend fun delete(itemId: Long) {
        isicDao.deleteItem(itemId)
    }

    suspend fun insertClient(client: AdtClient) {
        isicDao.insertClient(client)
    }

    suspend fun deleteClient(clientId: Long) {
        isicDao.deleteClient(clientId)
    }

    /**
     * Catálogo de amostra desativado para iniciar o app totalmente limpo.
     */
    suspend fun loadAdtSampleCatalog() {
        // Banco inicializa vazio, pronto para cadastros reais.
    }
}
