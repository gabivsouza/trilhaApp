package com.example.trilhaapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.CameraPosition;

import java.util.List;
import java.util.Locale;

public class ViewHikeActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private long trilhaId;
    private HikeHelp dbHelper;
    private String tipoNavegacao;

    private TextView tvNomeTrilha, tvDataInicio, tvDistanciaTotal, tvDuracao, tvVelMedia, tvVelMaxima;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_hike);

        dbHelper = new HikeHelp(this);

        tvNomeTrilha = findViewById(R.id.tvNomeTrilha);
        tvDataInicio = findViewById(R.id.tvDataInicio);
        tvDistanciaTotal = findViewById(R.id.tvDistanciaTotal);
        tvDuracao = findViewById(R.id.tvDuracao);
        tvVelMedia = findViewById(R.id.tvVelMedia);
        tvVelMaxima = findViewById(R.id.tvVelMaxima);

        trilhaId = getIntent().getLongExtra("TRILHA_ID", -1);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Respeita o tipo de mapa definido na ConfigurationActivity
        SharedPreferences prefs = getSharedPreferences(ConfigurationActivity.PREFS_NAME, MODE_PRIVATE);
        String tipoMapa = prefs.getString(ConfigurationActivity.KEY_MAP_TYPE, ConfigurationActivity.MAP_VETORIAL);

        if (tipoMapa.equals(ConfigurationActivity.MAP_SATELITE)) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        carregarDadosDaTrilha();

        // Dentro de onMapReady(@NonNull GoogleMap googleMap):
        SharedPreferences prefs = getSharedPreferences(ConfigurationActivity.PREFS_NAME, MODE_PRIVATE);
        String tipoMapa = prefs.getString(ConfigurationActivity.KEY_MAP_TYPE, ConfigurationActivity.MAP_VETORIAL);
        tipoNavegacao = prefs.getString(ConfigurationActivity.KEY_NAV_TYPE, ConfigurationActivity.NAV_NORTH_UP);
    }

    private void desenharTrajetoNoMapa(List<LatLng> pontos) {
        if (pontos == null || pontos.isEmpty()) {
            Toast.makeText(this, getString(R.string.view_no_points), Toast.LENGTH_SHORT).show();
            return;
        }

        PolylineOptions linha = new PolylineOptions()
                .addAll(pontos)
                .width(10f)
                .color(Color.parseColor("#2E7D32"));
        mMap.addPolyline(linha);

        mMap.addMarker(new MarkerOptions().position(pontos.get(0)).title(getString(R.string.view_marker_start)));
        if (pontos.size() > 1) {
            mMap.addMarker(new MarkerOptions()
                    .position(pontos.get(pontos.size() - 1))
                    .title(getString(R.string.view_marker_end)));
        }

        // Calcula o rumo (bearing) inicial da trilha caso Course Up esteja ativo
        float rumoInicial = 0f;
        if (tipoNavegacao.equals(ConfigurationActivity.NAV_COURSE_UP) && pontos.size() >= 2) {
            float[] resultados = new float[3];
            android.location.Location.distanceBetween(
                    pontos.get(0).latitude, pontos.get(0).longitude,
                    pontos.get(1).latitude, pontos.get(1).longitude,
                    resultados
            );
            rumoInicial = resultados[1];
        }

        final float bearingFinal = rumoInicial;

        mMap.setOnMapLoadedCallback(() -> {
            if (pontos.size() == 1) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pontos.get(0), 16f));
            } else {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng p : pontos) {
                    builder.include(p);
                }
                LatLngBounds limites = builder.build();
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(limites, 120));

                // Rotaciona o mapa para o rumo da trilha se Course Up estiver selecionado
                if (tipoNavegacao.equals(ConfigurationActivity.NAV_COURSE_UP)) {
                    CameraPosition cameraComRumo = new CameraPosition.Builder(mMap.getCameraPosition())
                            .bearing(bearingFinal)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraComRumo));
                }
            }
        });
    }

    private void carregarDadosDaTrilha() {
        if (trilhaId == -1) {
            Toast.makeText(this, "Nenhuma trilha selecionada.", Toast.LENGTH_SHORT).show();
            return;
        }

        Hike trilha = dbHelper.buscarTrilhaPorId(trilhaId);
        if (trilha == null) {
            Toast.makeText(this, "Trilha não encontrada no banco de dados.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preenche os textos sobrepostos ao mapa conforme o enunciado
        tvNomeTrilha.setText(trilha.getNome());
        tvDataInicio.setText("Início: " + trilha.getDataInicio());
        tvDistanciaTotal.setText(String.format(Locale.getDefault(), "Distância Total: %.2f km", trilha.getDistanciaKm()));
        tvDuracao.setText("Duração: " + trilha.getDuracao());
        tvVelMedia.setText(String.format(Locale.getDefault(), "Velocidade Média: %.1f km/h", trilha.getVelMedia()));
        tvVelMaxima.setText(String.format(Locale.getDefault(), "Velocidade Máxima: %.1f km/h", trilha.getVelMaxima()));

        List<LatLng> pontosDaTrilha = dbHelper.buscarPontosDaTrilha(trilhaId);
        desenharTrajetoNoMapa(pontosDaTrilha);
    }

    private void desenharTrajetoNoMapa(List<LatLng> pontos) {
        if (pontos == null || pontos.isEmpty()) {
            Toast.makeText(this, "Esta trilha não possui coordenadas gravadas.", Toast.LENGTH_SHORT).show();
            return;
        }

        PolylineOptions linha = new PolylineOptions()
                .addAll(pontos)
                .width(10f)
                .color(Color.parseColor("#2E7D32"));
        mMap.addPolyline(linha);

        mMap.addMarker(new MarkerOptions().position(pontos.get(0)).title("Início da Trilha"));
        if (pontos.size() > 1) {
            mMap.addMarker(new MarkerOptions()
                    .position(pontos.get(pontos.size() - 1))
                    .title("Fim da Trilha"));
        }

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