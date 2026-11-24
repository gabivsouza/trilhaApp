package com.example.trilhaapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class HikeHelp extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trilhas_app.db";
    private static final int DATABASE_VERSION = 1;

    // Tabela 1: Metadados da trilha
    public static final String TABELA_TRILHAS = "trilhas";
    public static final String COL_ID = "id";
    public static final String COL_NOME = "nome";
    public static final String COL_DATA_INICIO = "data_inicio";
    public static final String COL_DATA_FIM = "data_fim";
    public static final String COL_TIMESTAMP_INICIO = "timestamp_inicio";
    public static final String COL_DURACAO = "duracao";
    public static final String COL_DISTANCIA = "distancia_km";
    public static final String COL_VEL_MEDIA = "vel_media";
    public static final String COL_VEL_MAXIMA = "vel_maxima";

    // Tabela 2: Coordenadas (percurso) da trilha
    public static final String TABELA_PONTOS = "pontos_trilha";
    public static final String COL_PONTO_ID = "id";
    public static final String COL_TRILHA_ID = "trilha_id";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";

    public HikeHelp(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlTrilhas = "CREATE TABLE " + TABELA_TRILHAS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOME + " TEXT NOT NULL, " +
                COL_DATA_INICIO + " TEXT NOT NULL, " +
                COL_DATA_FIM + " TEXT NOT NULL, " +
                COL_TIMESTAMP_INICIO + " INTEGER NOT NULL, " +
                COL_DURACAO + " TEXT NOT NULL, " +
                COL_DISTANCIA + " REAL NOT NULL, " +
                COL_VEL_MEDIA + " REAL NOT NULL, " +
                COL_VEL_MAXIMA + " REAL NOT NULL)";

        String sqlPontos = "CREATE TABLE " + TABELA_PONTOS + " (" +
                COL_PONTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TRILHA_ID + " INTEGER NOT NULL, " +
                COL_LATITUDE + " REAL NOT NULL, " +
                COL_LONGITUDE + " REAL NOT NULL, " +
                "FOREIGN KEY(" + COL_TRILHA_ID + ") REFERENCES " +
                TABELA_TRILHAS + "(" + COL_ID + ") ON DELETE CASCADE)";

        db.execSQL(sqlTrilhas);
        db.execSQL(sqlPontos);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABELA_PONTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABELA_TRILHAS);
        onCreate(db);
    }

    // Insere a trilha e todas as suas coordenadas em uma transação
    public long salvarTrilhaCompleta(String nome, String dataInicio, String dataFim,
                                     long timestampInicio, String duracao, double distanciaKm,
                                     double velMedia, double velMaxima, List<LatLng> pontos) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long trilhaId = -1;
        try {
            ContentValues cvTrilha = new ContentValues();
            cvTrilha.put(COL_NOME, nome);
            cvTrilha.put(COL_DATA_INICIO, dataInicio);
            cvTrilha.put(COL_DATA_FIM, dataFim);
            cvTrilha.put(COL_TIMESTAMP_INICIO, timestampInicio);
            cvTrilha.put(COL_DURACAO, duracao);
            cvTrilha.put(COL_DISTANCIA, distanciaKm);
            cvTrilha.put(COL_VEL_MEDIA, velMedia);
            cvTrilha.put(COL_VEL_MAXIMA, velMaxima);

            trilhaId = db.insert(TABELA_TRILHAS, null, cvTrilha);

            if (trilhaId != -1 && pontos != null) {
                for (LatLng p : pontos) {
                    ContentValues cvPonto = new ContentValues();
                    cvPonto.put(COL_TRILHA_ID, trilhaId);
                    cvPonto.put(COL_LATITUDE, p.latitude);
                    cvPonto.put(COL_LONGITUDE, p.longitude);
                    db.insert(TABELA_PONTOS, null, cvPonto);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return trilhaId;
    }

    // Lista todas as trilhas registradas (da mais recente para a mais antiga)
    public List<Hike> listarTodasTrilhas() {
        List<Hike> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABELA_TRILHAS, null, null, null, null, null, COL_ID + " DESC");

        while (cursor.moveToNext()) {
            lista.add(cursorParaTrilha(cursor));
        }
        cursor.close();
        return lista;
    }

    // Busca os dados de uma trilha específica pelo ID
    public Hike buscarTrilhaPorId(long trilhaId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABELA_TRILHAS, null, COL_ID + " = ?",
                new String[]{String.valueOf(trilhaId)}, null, null, null);

        Hike hike = null;
        if (cursor.moveToFirst()) {
            hike = cursorParaTrilha(cursor);
        }
        cursor.close();
        return hike;
    }

    // Busca todas as coordenadas de uma trilha para desenhar o trajeto
    public List<LatLng> buscarPontosDaTrilha(long trilhaId) {
        List<LatLng> pontos = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABELA_PONTOS,
                new String[]{COL_LATITUDE, COL_LONGITUDE},
                COL_TRILHA_ID + " = ?",
                new String[]{String.valueOf(trilhaId)},
                null, null, COL_PONTO_ID + " ASC");

        while (cursor.moveToNext()) {
            pontos.add(new LatLng(cursor.getDouble(0), cursor.getDouble(1)));
        }
        cursor.close();
        return pontos;
    }

    // Editar o nome de uma trilha selecionada
    public void atualizarNomeTrilha(long trilhaId, String novoNome) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NOME, novoNome);
        db.update(TABELA_TRILHAS, cv, COL_ID + " = ?", new String[]{String.valueOf(trilhaId)});
    }

    // Apagar uma trilha específica
    public void apagarTrilha(long trilhaId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABELA_PONTOS, COL_TRILHA_ID + " = ?", new String[]{String.valueOf(trilhaId)});
        db.delete(TABELA_TRILHAS, COL_ID + " = ?", new String[]{String.valueOf(trilhaId)});
    }

    // Apagar trilhas em um determinado intervalo de tempo (timestamps em ms)
    public int apagarTrilhasPorIntervalo(long inicioMs, long fimMs) {
        SQLiteDatabase db = getWritableDatabase();
        String where = COL_TIMESTAMP_INICIO + " BETWEEN ? AND ?";
        String[] args = new String[]{String.valueOf(inicioMs), String.valueOf(fimMs)};

        // Remove primeiro os pontos das trilhas no intervalo
        db.execSQL("DELETE FROM " + TABELA_PONTOS + " WHERE " + COL_TRILHA_ID +
                " IN (SELECT " + COL_ID + " FROM " + TABELA_TRILHAS + " WHERE " + where + ")", args);

        return db.delete(TABELA_TRILHAS, where, args);
    }

    // Apagar todas as trilhas percorridas
    public void apagarTodasTrilhas() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABELA_PONTOS, null, null);
        db.delete(TABELA_TRILHAS, null, null);
    }

    private Hike cursorParaTrilha(Cursor c) {
        return new Hike(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_NOME)),
                c.getString(c.getColumnIndexOrThrow(COL_DATA_INICIO)),
                c.getString(c.getColumnIndexOrThrow(COL_DATA_FIM)),
                c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP_INICIO)),
                c.getString(c.getColumnIndexOrThrow(COL_DURACAO)),
                c.getDouble(c.getColumnIndexOrThrow(COL_DISTANCIA)),
                c.getDouble(c.getColumnIndexOrThrow(COL_VEL_MEDIA)),
                c.getDouble(c.getColumnIndexOrThrow(COL_VEL_MAXIMA))
        );
    }
}
