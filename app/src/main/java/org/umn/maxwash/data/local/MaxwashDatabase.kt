package org.umn.maxwash.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CustomerEntity::class, OutletEntity::class, ServiceEntity::class,
        OrderEntity::class, StatusEventEntity::class, NotificationEntity::class,
        SessionEntity::class, MetadataEntity::class, FragranceEntity::class, PromotionEntity::class,
        ReceiptEntity::class],
    version = 2, exportSchema = true
)
abstract class MaxwashDatabase : RoomDatabase() {
    abstract fun dao(): MaxwashDao

    companion object {
        const val NAME = "maxwash.db"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No historic payment method or paid state can be inferred from a laundry status.
                db.execSQL("""CREATE TABLE IF NOT EXISTS `receipts` (
                    `orderId` TEXT NOT NULL, `number` TEXT NOT NULL, `orderedAt` INTEGER NOT NULL, `paidAt` INTEGER,
                    `outletName` TEXT NOT NULL, `outletAddress` TEXT NOT NULL,
                    `customerName` TEXT NOT NULL, `customerPhone` TEXT NOT NULL, `customerEmail` TEXT NOT NULL, `customerAddress` TEXT NOT NULL,
                    `serviceName` TEXT NOT NULL, `weightKg` REAL NOT NULL, `pricePerKg` INTEGER NOT NULL,
                    `fragrance` TEXT NOT NULL, `clothingNotes` TEXT NOT NULL,
                    `subtotal` INTEGER NOT NULL, `detergent` INTEGER NOT NULL, `discount` INTEGER NOT NULL, `promoCode` TEXT NOT NULL,
                    `paymentMethod` TEXT NOT NULL, `total` INTEGER NOT NULL,
                    PRIMARY KEY(`orderId`), FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent())
            }
        }
        @Volatile private var instance: MaxwashDatabase? = null

        fun getInstance(context: Context): MaxwashDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MaxwashDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
