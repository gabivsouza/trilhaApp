package com.example.trilhaapp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

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

        // Evita que o painel inferior fique escondido atrás da barra de navegação (< O |||)
        View painelResumo = findViewById(R.id.painelResumo);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) painelResumo.getLayoutParams();
            lp.bottomMargin = bars.bottom + Math.round(16 * getResources().getDisplayMetrics().density);
            painelResumo.setLayoutParams(lp);
            return insets;
        });

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

        SharedPreferences prefs = getSharedPreferences(ConfigurationActivity.PREFS_NAME, MODE_PRIVATE);
        String tipoMapa = prefs.getString(ConfigurationActivity.KEY_MAP_TYPE, ConfigurationActivity.MAP_VETORIAL);
        tipoNavegacao = prefs.getString(ConfigurationActivity.KEY_NAV_TYPE, ConfigurationActivity.NAV_NORTH_UP);

        if (tipoMapa.equals(ConfigurationActivity.MAP_SATELITE)) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        carregarDadosDaTrilha();
    }

    private BitmapDescriptor obterIconePersonalizado(int drawableId, int larguraDp, int alturaDp) {
        float densidade = getResources().getDisplayMetrics().density;
        int larguraPx = Math.round(larguraDp * densidade);
        int alturaPx = Math.round(alturaDp * densidade);

        Bitmap imagemOriginal = BitmapFactory.decodeResource(getResources(), drawableId);
        Bitmap imagemRedimensionada = Bitmap.createScaledBitmap(
                imagemOriginal, larguraPx, alturaPx, false
        );
        return BitmapDescriptorFactory.fromBitmap(imagemRedimensionada);
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
            Toast.makeText(this, getString(R.string.view_no_points), Toast.LENGTH_SHORT).show();
            return;
        }

        PolylineOptions linha = new PolylineOptions()
                .addAll(pontos)
                .width(10f)
                .color(Color.parseColor("#2E7D32"));
        mMap.addPolyline(linha);

        BitmapDescriptor iconeCachorro = obterIconePersonalizado(R.drawable.ic_marcador_jojo, 52, 52);

        // Marcador personalizado no início da trilha
        mMap.addMarker(new MarkerOptions()
                .position(pontos.get(0))
                .title(getString(R.string.view_marker_start))
                .anchor(0.5f, 1.0f)
                .icon(iconeCachorro));

        if (pontos.size() > 1) {
            mMap.addMarker(new MarkerOptions()
                    .position(pontos.get(pontos.size() - 1))
                    .title(getString(R.string.view_marker_end))
                    .anchor(0.5f, 1.0f)
                    .icon(iconeCachorro));
        }

        float rumoInicial = 0f;
        if (ConfigurationActivity.NAV_COURSE_UP.equals(tipoNavegacao) && pontos.size() >= 2) {
            float[] resultados = new float[3];
            Location.distanceBetween(
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

                if (ConfigurationActivity.NAV_COURSE_UP.equals(tipoNavegacao)) {
                    CameraPosition cameraComRumo = new CameraPosition.Builder(mMap.getCameraPosition())
                            .bearing(bearingFinal)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraComRumo));
                }
            }
        });
    }
}