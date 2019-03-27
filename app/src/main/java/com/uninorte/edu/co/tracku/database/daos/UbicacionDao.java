package com.uninorte.edu.co.tracku.database.daos;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.uninorte.edu.co.tracku.database.entities.Ubicacion;

import java.util.List;

@Dao
public interface UbicacionDao {

    @Query("select * from ubicacion")
    List<Ubicacion> getAllUsers();

    @Query("select * from ubicacion where email=:email")
    List<Ubicacion> getUserByEmail(String  email);

    @Query("select * from ubicacion where sincronizado=:sinc and email=:user")
    List<Ubicacion> notSincUsers(int sinc, String user);

    @Insert
    void insertUbicacion(Ubicacion ubicacion);

    @Delete
    void deleteUbicacion(Ubicacion ubicacion);
}
