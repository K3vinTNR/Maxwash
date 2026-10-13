package org.umn.maxwash

import android.app.Application
import org.umn.maxwash.data.AddressLocator
import org.umn.maxwash.data.TomTomAddressLocator
import org.umn.maxwash.data.TomTomClient
import org.umn.maxwash.data.RoomRepository
import org.umn.maxwash.data.local.MaxwashDatabase
import org.umn.maxwash.data.local.CatalogData

open class MaxwashApplication : Application() {
    open val tomTomClient: TomTomClient by lazy { TomTomClient(BuildConfig.TOMTOM_API_KEY) }
    open val addressLocator: AddressLocator by lazy { TomTomAddressLocator(tomTomClient) }
    open val repository: RoomRepository by lazy {
        RoomRepository(MaxwashDatabase.getInstance(this), CatalogData(this))
    }
}
