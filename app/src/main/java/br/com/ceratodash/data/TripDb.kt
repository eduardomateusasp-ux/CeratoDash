package br.com.ceratodash.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Banco local das viagens (SQLite puro, sem Room, para manter o app leve). Nada sai do aparelho. */
class TripDb private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "viagens.db", null, 2), TripStore {

    companion object {
        @Volatile private var instance: TripDb? = null

        fun get(ctx: Context): TripDb =
            instance ?: synchronized(this) {
                instance ?: TripDb(ctx.applicationContext).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE trips(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "start_ms INTEGER NOT NULL, " +
                "end_ms INTEGER NOT NULL, " +
                "updated_ms INTEGER NOT NULL, " +
                "ended INTEGER NOT NULL DEFAULT 0, " +
                "distance_km REAL NOT NULL DEFAULT 0, " +
                "max_speed REAL NOT NULL DEFAULT 0, " +
                "fuel_l REAL NOT NULL DEFAULT 0, " +
                "fuel_dist_km REAL NOT NULL DEFAULT 0, " +
                "score INTEGER, " +
                "events INTEGER NOT NULL DEFAULT 0)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE trips ADD COLUMN score INTEGER")
            db.execSQL("ALTER TABLE trips ADD COLUMN events INTEGER NOT NULL DEFAULT 0")
        }
    }

    override fun insert(startMs: Long): Long {
        val v = ContentValues().apply {
            put("start_ms", startMs)
            put("end_ms", startMs)
            put("updated_ms", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("trips", null, v)
    }

    override fun update(trip: Trip, ended: Boolean) {
        val v = ContentValues().apply {
            put("end_ms", trip.endMs)
            put("updated_ms", System.currentTimeMillis())
            put("ended", if (ended) 1 else 0)
            put("distance_km", trip.distanceKm)
            put("max_speed", trip.maxSpeed.toDouble())
            put("fuel_l", trip.fuelL)
            put("fuel_dist_km", trip.fuelDistKm)
            if (trip.score != null) put("score", trip.score) else putNull("score")
            put("events", trip.events)
        }
        writableDatabase.update("trips", v, "id = ?", arrayOf(trip.id.toString()))
    }

    /** Viagens que ficaram abertas porque o app foi encerrado de repente: fecha na última gravação. */
    fun closeOpen() {
        writableDatabase.execSQL("UPDATE trips SET ended = 1, end_ms = MAX(start_ms, MIN(end_ms, updated_ms)) WHERE ended = 0")
    }

    fun list(limit: Int = 200): List<Trip> {
        val out = ArrayList<Trip>()
        readableDatabase.query(
            "trips", null, "ended = 1", null, null, null, "start_ms DESC", limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Trip(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        startMs = c.getLong(c.getColumnIndexOrThrow("start_ms")),
                        endMs = c.getLong(c.getColumnIndexOrThrow("end_ms")),
                        distanceKm = c.getDouble(c.getColumnIndexOrThrow("distance_km")),
                        maxSpeed = c.getFloat(c.getColumnIndexOrThrow("max_speed")),
                        fuelL = c.getDouble(c.getColumnIndexOrThrow("fuel_l")),
                        fuelDistKm = c.getDouble(c.getColumnIndexOrThrow("fuel_dist_km")),
                        score = c.getColumnIndexOrThrow("score").let { i -> if (c.isNull(i)) null else c.getInt(i) },
                        events = c.getInt(c.getColumnIndexOrThrow("events"))
                    )
                )
            }
        }
        return out
    }

    fun delete(id: Long) {
        writableDatabase.delete("trips", "id = ?", arrayOf(id.toString()))
    }

    fun deleteAll() {
        writableDatabase.delete("trips", null, null)
    }
}
