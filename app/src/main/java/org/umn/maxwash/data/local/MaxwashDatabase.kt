package org.umn.maxwash.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CustomerEntity::class, OutletEntity::class, ServiceEntity::class,
        OrderEntity::class, StatusEventEntity::class, NotificationEntity::class,
        SessionEntity::class, MetadataEntity::class, FragranceEntity::class, PromotionEntity::class],
    version = 1, exportSchema = true
)
abstract class MaxwashDatabase : RoomDatabase() {
    abstract fun dao(): MaxwashDao

    companion object {
        const val NAME = "maxwash.db"
        @Volatile private var instance: MaxwashDatabase? = null

        fun getInstance(context: Context): MaxwashDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MaxwashDatabase::class.java, NAME)
                .build().also { instance = it }
        }
    }
}
