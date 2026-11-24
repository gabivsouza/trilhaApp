package com.example.trilhaapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class ConsultActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private long trilhaId;

    private TextView tvDataInicio, tvDistanciaTotal, tvDuracao, tvVelMedia, tvVelMaxima;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consult);

        // 1. Liga os TextViews do painel de resumo
        tvDataInicio = findViewById(R.id.tvDataInicio);
        tvDistanciaTotal = findViewById(R.id.tvDistanciaTotal);
        tvDuracao = findViewById(R.id.tvDuracao);
        tvVelMedia = findViewById(R.id.tvVelMedia);
        tvVelMaxima = findViewById(R.id.tvVelMaxima);

        // 2. Recebe o ID da trilha clicada na ViewActivity
        trilhaId = getIntent().getLongExtra("TRILHA_ID", -1);

        // 3. Inicializa o fragmento do mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    //Retorna o mapa quando pronto

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Aplica o tipo de mapa escolhido na ConfigurationActivity
        SharedPreferences prefs = getSharedPreferences(ConfigurationActivity.PREFS_NAME, MODE_PRIVATE);
        String tipoMapa = prefs.getString(ConfigurationActivity.KEY_MAP_TYPE, ConfigurationActivity.MAP_VETORIAL);

        if (tipoMapa.equals(ConfigurationActivity.MAP_SATELITE)) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        carregarDadosDaTrilha();
    }

    private void carregarDadosDaTrilha() {
        if (trilhaId == -1) {
            Toast.makeText(this, "Nenhuma trilha selecionada.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Quando o SQLite estiver pronto, buscar aqui pelo 'trilhaId':
        // 1. Preencher os TextViews com os dados salvos da trilha:
        //    tvDataInicio.setText("Início: " + trilha.getDataInicio());
        //    tvDistanciaTotal.setText(String.format("Distância: %.2f km", trilha.getDistanciaKm()));
        //    tvDuracao.setText("Duração: " + trilha.getDuracao());
        //    tvVelMedia.setText(String.format("Velocidade Média: %.1f km/h", trilha.getVelMedia()));
        //    tvVelMaxima.setText(String.format("Velocidade Máxima: %.1f km/h", trilha.getVelMaxima()));

        // 2. Buscar a lista de coordenadas (LatLng) dessa trilha e chamar o método abaixo:
        List<LatLng> pontosDaTrilha = new ArrayList<>();
        desenharTrajetoNoMapa(pontosDaTrilha);
    }

    private void desenharTrajetoNoMapa(List<LatLng> pontos) {
        if (pontos == null || pontos.isEmpty()) {
            return;
        }

        // Desenha a linha do percurso
        PolylineOptions linha = new PolylineOptions()
                .addAll(pontos)
                .width(10f)
                .color(Color.parseColor("#2E7D32")); // Verde
        mMap.addPolyline(linha);

        // Marca o ponto de início e o ponto final
        mMap.addMarker(new MarkerOptions().position(pontos.get(0)).title("Início da Trilha"));
        if (pontos.size() > 1) {
            mMap.addMarker(new MarkerOptions()
                    .position(pontos.get(pontos.size() - 1))
                    .title("Fim da Trilha"));
        }

        // Centraliza e ajusta o zoom para caber a trilha inteira na tela
        mMap.setOnMapLoadedCallback(() -> {
            if (pontos.size() == 1) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pontos.get(0), 16f));
            } else {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng p : pontos) {
                    builder.include(p);
                }
                LatLngBounds limites = builder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(limites, 120));
            }
        });
    }
}