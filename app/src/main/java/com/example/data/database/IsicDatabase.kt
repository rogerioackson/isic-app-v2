package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.dao.ClientDao
import com.example.data.dao.InventoryDao
import com.example.data.dao.MovementDao
import com.example.data.model.Client
import com.example.data.model.InventoryItem
import com.example.data.model.ItemCondition
import com.example.data.model.MovementItem
import com.example.data.model.MovementType
import com.example.data.model.StockMovement

class Converters {
    @TypeConverter
    fun fromMovementType(value: MovementType): String = value.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = try {
        MovementType.valueOf(value)
    } catch (e: Exception) {
        MovementType.SAIDA
    }

    @TypeConverter
    fun fromItemCondition(value: ItemCondition): String = value.name

    @TypeConverter
    fun toItemCondition(value: String): ItemCondition = try {
        ItemCondition.valueOf(value)
    } catch (e: Exception) {
        ItemCondition.BOM_ESTADO
    }
}

@Database(
    entities = [
        InventoryItem::class,
        Client::class,
        StockMovement::class,
        MovementItem::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class IsicDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun clientDao(): ClientDao
    abstract fun movementDao(): MovementDao

    companion object {
        @Volatile
        private var INSTANCE: IsicDatabase? = null

        fun getInstance(context: Context): IsicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IsicDatabase::class.java,
                    "isic_adt_inventory.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
