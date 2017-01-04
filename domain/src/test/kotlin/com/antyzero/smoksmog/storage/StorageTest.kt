package com.antyzero.smoksmog.storage

import com.antyzero.smoksmog.storage.model.Item
import com.antyzero.smoksmog.storage.model.Item.Station
import com.antyzero.smoksmog.storage.model.Module
import com.antyzero.smoksmog.storage.model.Module.AirQualityIndex
import com.antyzero.smoksmog.storage.model.Module.AirQualityIndex.Type.POLISH
import com.antyzero.smoksmog.storage.model.Module.Measurements
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.*

class StorageTest {

    private lateinit var storage: Storage

    @Before
    fun setUp() {
        storage = JsonFileStorage()
    }

    @Test
    fun addById() {
        val id = 3L

        storage.addStation(id)

        assertThat(storage.fetchAll()).hasSize(1)
        assertThat(storage.fetchAll()[0].id).isEqualTo(id)
    }

    @Test
    fun remove() {
        with(storage) {
            addStation(1)
            addStation(2)
        }

        storage.removeById(1)

        assertThat(storage.fetchAll()).hasSize(1)
        assertThat(storage.fetchAll()[0].id).isEqualTo(2)
    }

    @Test
    fun update() {
        with(storage) {
            add(Station(3))
        }

        storage.update(3, Station().apply {
            modules.add(Measurements())
            modules.add(AirQualityIndex())
        })

        val stations = storage.fetchAll()
        assertThat(stations).hasSize(1)
        assertThat(stations[0].id).isEqualTo(3)
        assertThat(stations[0].modules).hasSize(2)
        assertThat(stations[0].modules.first()).isInstanceOf(Measurements::class.java)
    }

    @Test(expected = NoSuchElementException::class)
    fun updateNonExisting() {
        with(storage){
            addStation(1)
        }

        storage.update(9, Station())
    }

    @Test
    fun reference() {
        val list = storage.fetchAll()

        storage.addStation(1)
        storage.addStation(2)

        assertThat(list).hasSize(2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidStationIdValues() {
        storage.addStation(0)
    }

    @Test
    fun addNearestAtTheBeginning() {
        with(storage) {
            addStation(1)
            addStation(2)
            addStation(3)
        }
        val nearest = Item.Nearest()

        storage.add(nearest)

        assertThat(storage.fetchAll()[0].id).isEqualTo(nearest.id)
        assertThat(storage.fetchAll()).hasSize(4)
    }

    @Test
    fun duplicatesPrevented() {
        storage.addStation(1)
        storage.addStation(1)

        assertThat(storage.fetchAll()).hasSize(1)
    }
}
