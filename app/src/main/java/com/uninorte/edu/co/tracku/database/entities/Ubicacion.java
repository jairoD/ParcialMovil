package com.uninorte.edu.co.tracku.database.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.uninorte.edu.co.tracku.database.converters.DateConverter;

import java.util.Date;

import androidx.annotation.NonNull;

@TypeConverters(DateConverter.class)

@Entity
public class Ubicacion {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    public int ubicacionId;

    public Date startTime;

    @ColumnInfo(name="email")
    public String email;

    @ColumnInfo(name="latitud")
    public String latitud;

    @ColumnInfo(name="longitud")
    public String longitud;

    @ColumnInfo(name="sincronizado")
    public boolean sincronizado;

}
