package com.example.animalcatch.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AnimalDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(AnimalEntity animal);

    @Update
    void update(AnimalEntity animal);

    @Query("SELECT * FROM animals WHERE name = :name LIMIT 1")
    AnimalEntity getByName(String name);

    @Query("UPDATE animals SET count = count + 1, photo_path = :photoPath WHERE name = :name")
    void incrementCountAndUpdatePhoto(String name, String photoPath);

    /** Called after a won battle — awards XP and persists new level + boosted stats. */
    @Query("UPDATE animals SET xp = :xp, level = :level, hp = :hp, atk = :atk, def = :def, spd = :spd WHERE name = :name")
    void updateXpAndStats(String name, int xp, int level, int hp, int atk, int def, int spd);

    @Query("SELECT * FROM animals ORDER BY name ASC")
    LiveData<List<AnimalEntity>> getAllLive();

    @Query("SELECT * FROM animals ORDER BY name ASC")
    List<AnimalEntity> getAll();
}