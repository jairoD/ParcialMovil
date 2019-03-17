package com.uninorte.edu.co.tracku.database.core;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

import com.uninorte.edu.co.tracku.database.daos.UserDao;
import com.uninorte.edu.co.tracku.database.daos.UbicacionDao;
import com.uninorte.edu.co.tracku.database.entities.Ubicacion;
import com.uninorte.edu.co.tracku.database.entities.User;

@Database(entities = {User.class, Ubicacion.class},version = 3)
public abstract class TrackUDatabaseManager extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract UbicacionDao ubicacionDao();



    public static Migration MIGRATION_2_3 = new Migration(2,3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Ubicacion` (`ubicacionId` INTEGER NOT NULL, `startTime` INTEGER, `email` TEXT," +
                    " `latitud` TEXT, `longitud` TEXT, PRIMARY KEY(`ubicacionId`))");
        }
    };

}
