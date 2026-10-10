package org.umn.maxwash

import android.app.Application
import org.umn.maxwash.data.RoomRepository
import org.umn.maxwash.data.local.MaxwashDatabase
import org.umn.maxwash.data.local.SeedData

open class MaxwashApplication : Application() {
    open val repository: RoomRepository by lazy {
        RoomRepository(MaxwashDatabase.getInstance(this), SeedData(this))
    }
}
