package com.example.trilhaapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;


public class ConfigurationActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "TrilhaConfigPrefs";
    public static final String KEY_MAP_TYPE = "map_type";
    public static final String KEY_NAV_TYPE = "nav_type";

    public static final String MAP_VETORIAL = "vetorial";
    public static final String MAP_SATELITE = "satelite";
    public static final String NAV_NORTH_UP = "north_up";
    public static final String NAV_COURSE_UP = "course_up";

    private Button btnMapVet, btnMapSat;
    private Button btnNup, btnCup;
    private Button btnRegHike, btnViewHike;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        btnMapVet = findViewById(R.id.btn_mapVet);
        btnMapSat = findViewById(R.id.btn_mapSat);
        btnNup = findViewById(R.id.btn_Nup);
        btnCup = findViewById(R.id.btn_Cup);
        btnRegHike = findViewById(R.id.btn_regHike);
        btnViewHike = findViewById(R.id.btn_viewHike);

        String mapaAtual = sharedPreferences.getString(KEY_MAP_TYPE, MAP_VETORIAL);
        String navAtual = sharedPreferences.getString(KEY_NAV_TYPE, NAV_NORTH_UP);


        atualizarBotoesMapa(mapaAtual);
        atualizarBotoesNavegacao(navAtual);


        btnMapVet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                salvarPreferencia(KEY_MAP_TYPE, MAP_VETORIAL);
                atualizarBotoesMapa(MAP_VETORIAL);
            }
        });

        btnMapSat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                salvarPreferencia(KEY_MAP_TYPE, MAP_SATELITE);
                atualizarBotoesMapa(MAP_SATELITE);

            }
        });


        btnNup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                salvarPreferencia(KEY_NAV_TYPE, NAV_NORTH_UP);
                atualizarBotoesNavegacao(NAV_NORTH_UP);
            }
        });

        btnCup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                salvarPreferencia(KEY_NAV_TYPE, NAV_COURSE_UP);
                atualizarBotoesNavegacao(NAV_COURSE_UP);
            }
        });

        btnRegHike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });

        btnViewHike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ConsultActivity.class);
                startActivity(intent);
            }
        });
    }

    // Salva um par chave-valor no SharedPreferences
    private void salvarPreferencia(String chave, String valor) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(chave, valor);
        editor.apply();
    }

    private void atualizarBotoesMapa(String tipoMapa) {
        int corAtivo = Color.parseColor("#2E7D32");   // Verde
        int corInativo = Color.parseColor("#78909C"); // Cinza


        boolean isSatelite = tipoMapa.equals(MAP_SATELITE);

        ViewCompat.setBackgroundTintList(btnMapVet, ColorStateList.valueOf(isSatelite ? corInativo : corAtivo));
        ViewCompat.setBackgroundTintList(btnMapSat, ColorStateList.valueOf(isSatelite ? corAtivo : corInativo));
    }

    // Destaca em verde apenas o botão de navegação selecionado
    private void atualizarBotoesNavegacao(String tipoNav) {
        int corAtivo = Color.parseColor("#2E7D32");   // Verde
        int corInativo = Color.parseColor("#78909C"); // Cinza

        boolean isCourseUp = tipoNav.equals(NAV_COURSE_UP);

        ViewCompat.setBackgroundTintList(btnNup, ColorStateList.valueOf(isCourseUp ? corInativo : corAtivo));
        ViewCompat.setBackgroundTintList(btnCup, ColorStateList.valueOf(isCourseUp ? corAtivo : corInativo));
    }

}



