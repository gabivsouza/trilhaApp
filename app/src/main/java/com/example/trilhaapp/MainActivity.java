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


public class MainActivity extends AppCompatActivity {

    // Constantes públicas para serem acessadas nas outras Activities (Maps e Register)
    public static final String PREFS_NAME = "TrilhaConfigPrefs";
    public static final String KEY_MAP_TYPE = "map_type";
    public static final String KEY_NAV_TYPE = "nav_type";

    // Valores possíveis das configurações
    public static final String MAP_VETORIAL = "vetorial";
    public static final String MAP_SATELITE = "satelite";
    public static final String NAV_NORTH_UP = "north_up";
    public static final String NAV_COURSE_UP = "course_up";

    // Componentes da interface
    private Button btnMapVet, btnMapSat;
    private Button btnNup, btnCup;
    private Button btnRegHike, btnViewHike;

    // Objeto para salvar as preferências do usuário
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa o SharedPreferences em modo privado
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Vincula os botões do XML com o código Java
        btnMapVet = findViewById(R.id.btn_mapVet);
        btnMapSat = findViewById(R.id.btn_mapSat);
        btnNup = findViewById(R.id.btn_Nup);
        btnCup = findViewById(R.id.btn_Cup);
        btnRegHike = findViewById(R.id.btn_regHike);
        btnViewHike = findViewById(R.id.btn_viewHike);

        // Carrega as configurações salvas (se não houver nada, usa os defaults do enunciado)
        String mapaAtual = sharedPreferences.getString(KEY_MAP_TYPE, MAP_VETORIAL);
        String navAtual = sharedPreferences.getString(KEY_NAV_TYPE, NAV_NORTH_UP);

        // Atualiza as cores iniciais dos botões conforme o que está salvo
        atualizarBotoesMapa(mapaAtual);
        atualizarBotoesNavegacao(navAtual);

        // Eventos de clique para Tipo de Mapa
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

        // Eventos de clique para Forma de Navegação
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

        // Navegação para a tela de Registrar Trilha
        btnRegHike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Navegação para a tela de Consultar Trilhas
        btnViewHike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ViewActivity.class);
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

    // Destaca em verde apenas o botão de mapa selecionado
    private void atualizarBotoesMapa(String tipoMapa) {
        int corAtivo = Color.parseColor("#2E7D32");   // Verde
        int corInativo = Color.parseColor("#78909C"); // Cinza

        /*
        O Color.parseColor("#2E7D32") é apenas um tradutor: como o Java do Android representa cores usando números inteiros (int),
        esse método pega o código hexadecimal (igual ao que usamos no XML) e
        converte no número correspondente àquela cor.

        Já o Paint (que aparece nos slides de Custom View) é usado quando você mesma está
        desenhando formas geométricas e textos do zero na tela dentro do
        método onDraw(Canvas canvas). Como aqui estamos usando um Button pronto do Android —
        que já sabe se desenhar sozinho —, não precisamos de Paint nem de Canvas;
        apenas avisamos ao botão qual cor aplicar no fundo.

        Se você achar mais fácil de ler e explicar na apresentação oral,
        dá para evitar o parseColor de duas maneiras:Usando constantes prontas do Android:
        trocar Color.parseColor(...) por cores nativas como Color.GREEN (ou Color.BLUE) e C
        olor.GRAY.Usando o arquivo colors.xml: cadastrar suas cores em res/values/colors.xml
        (como aparece na estrutura de pastas dos slides) e chamar no Java com
        ContextCompat.getColor(this, R.color.sua_cor).

         */


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

