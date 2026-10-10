package org.umn.maxwash

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.runner.AndroidJUnitRunner
import org.umn.maxwash.data.RoomRepository
import org.umn.maxwash.data.local.MaxwashDatabase
import org.umn.maxwash.data.local.SeedData

/** UI tests use their own database and never reset the installed app's customer data. */
class TestMaxwashApplication : MaxwashApplication() {
    val database by lazy { Room.inMemoryDatabaseBuilder(this, MaxwashDatabase::class.java).build() }
    override val repository by lazy { RoomRepository(database, SeedData(this)) }
}

class MaxwashTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, TestMaxwashApplication::class.java.name, context)
}
