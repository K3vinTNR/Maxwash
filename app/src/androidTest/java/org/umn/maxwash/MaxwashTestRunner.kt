package org.umn.maxwash

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.runner.AndroidJUnitRunner
import org.umn.maxwash.data.RoomRepository
import org.umn.maxwash.data.AddressLocator
import org.umn.maxwash.data.MapPoint
import org.umn.maxwash.data.TomTomClient
import org.umn.maxwash.data.MapHttpTransport
import org.umn.maxwash.data.MapHttpResponse
import org.umn.maxwash.data.local.MaxwashDatabase
import org.umn.maxwash.data.local.CatalogData

/** UI tests use their own database and never reset the installed app's customer data. */
class TestMaxwashApplication : MaxwashApplication() {
    // UI fixtures never make billable network requests; a separate test uses the live provider.
    var mapClient = offlineMapClient()
    override val tomTomClient: TomTomClient get() = mapClient
    var mapLookup: suspend (String) -> MapPoint? = { MapPoint(-6.23, 106.81) }
    override val addressLocator = AddressLocator { mapLookup(it) }
    val database by lazy { Room.inMemoryDatabaseBuilder(this, MaxwashDatabase::class.java).build() }
    override val repository by lazy { RoomRepository(database, CatalogData(this)) }

    fun offlineMapClient() = TomTomClient("instrumentation-fixture", MapHttpTransport { _, _ ->
        MapHttpResponse(503, "text/plain", byteArrayOf())
    })
}

class MaxwashTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, TestMaxwashApplication::class.java.name, context)
}
