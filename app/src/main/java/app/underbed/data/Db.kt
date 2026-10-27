package app.underbed.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Meal
import app.underbed.domain.Night

class Converters {
    @TypeConverter fun food(f: Food): String = f.name
    @TypeConverter fun food(s: String): Food = Food.entries.firstOrNull { it.name == s } ?: Food.CASE
    @TypeConverter fun genre(g: Genre): String = g.name
    @TypeConverter fun genre(s: String): Genre = Genre.of(s)
}

@Dao
interface BedDao {
    @Query("SELECT * FROM meal ORDER BY at, id") suspend fun meals(): List<Meal>
    @Query("SELECT * FROM night ORDER BY start, id") suspend fun nights(): List<Night>

    @Insert suspend fun insert(m: Meal): Long
    @Update suspend fun update(m: Meal)
    @Query("DELETE FROM meal WHERE id = :id") suspend fun deleteMeal(id: Long)
    @Insert suspend fun insert(n: Night): Long
    @Update suspend fun update(n: Night)
}

@Database(entities = [Meal::class, Night::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class BedDb : RoomDatabase() {
    abstract fun dao(): BedDao

    companion object {
        fun create(c: Context): BedDb = Room.databaseBuilder(c, BedDb::class.java, "underbed.db").build()
        fun memory(c: Context): BedDb = Room.inMemoryDatabaseBuilder(c, BedDb::class.java).build()
    }
}
