package com.example.trilhaapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegisterActivity extends FragmentActivity implements OnMapReadyCallback {


    /*
    1. Permissões (AndroidManifest.xml) e Biblioteca (build.gradle.kts)
       ACCESS_FINE_LOCATION: Autoriza o aplicativo a acessar
       a localização exata usando o chip de GPS do aparelho.
       É o que permite traçar a linha da trilha com precisão de poucos metros.

        ACCESS_COARSE_LOCATION: Autoriza o acesso à localização aproximada
        (baseada em Wi-Fi e antenas de celular).
        Nas versões atuais do Android,
        o sistema exige que essa permissão
        seja declarada junto com a FINE_LOCATION para que a janela
        pedindo permissão ao usuário apareça corretamente.
        implementation("com.google.android.gms:play-services-location:21.3.0"):
        Baixa e adiciona ao projeto a biblioteca oficial de localização do Google
        (FusedLocationProviderClient).
        Sem essa linha no Gradle,
        o Android Studio não reconhece as classes usadas na ConsultActivity para ler o GPS
        (LocationRequest, LocationCallback, LocationServices) e acusa erro no código.
     */

    private static final int REQUEST_LOCATION_PERMISSION = 100;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Marker marcadorUsuario;
    private Polyline trilhaPolyline;
    private final List<LatLng> listaPontos = new ArrayList<>();

    // Configurações lidas da ConfigurationActivity
    private String tipoMapa;
    private String tipoNavegacao;

    // Variáveis de controle da trilha
    private boolean gravando = false;
    private long tempoInicioMs = 0L;
    private String dataHoraInicio = "";
    private Location ultimaLocalizacao = null;
    private double distanciaTotalKm = 0.0;
    private float velocidadeMaximaKmh = 0f;

    // Elementos da tela
    private TextView tvRegDuracao, tvRegDistancia, tvRegVelocidade;
    private Button btnIniciarTrilha, btnFinalizarTrilha;

    // Cronômetro que atualiza a cada 1 segundo
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (gravando) {
                long millis = SystemClock.elapsedRealtime() - tempoInicioMs;
                tvRegDuracao.setText(getString(R.string.reg_duration_format, formatarTempo(millis)));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tvRegDuracao = findViewById(R.id.tvRegDuracao);
        tvRegDistancia = findViewById(R.id.tvRegDistancia);
        tvRegVelocidade = findViewById(R.id.tvRegVelocidade);
        btnIniciarTrilha = findViewById(R.id.btnIniciarTrilha);
        btnFinalizarTrilha = findViewById(R.id.btnFinalizarTrilha);

        // Lê as preferências salvas na ConfigurationActivity
        SharedPreferences prefs = getSharedPreferences(ConfigurationActivity.PREFS_NAME, MODE_PRIVATE);
        tipoMapa = prefs.getString(ConfigurationActivity.KEY_MAP_TYPE, ConfigurationActivity.MAP_VETORIAL);
        tipoNavegacao = prefs.getString(ConfigurationActivity.KEY_NAV_TYPE, ConfigurationActivity.NAV_NORTH_UP);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        configurarLocationCallback();

        btnIniciarTrilha.setOnClickListener(v -> iniciarTrilha());
        btnFinalizarTrilha.setOnClickListener(v -> finalizarTrilha());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 1. Aplica o tipo de mapa escolhido
        if (tipoMapa.equals(ConfigurationActivity.MAP_SATELITE)) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        // 2. Habilita controles de zoom e bússola na tela
        UiSettings mapUI = mMap.getUiSettings();
        mapUI.setZoomControlsEnabled(true);
        mapUI.setCompassEnabled(true);

        // 3. Prepara a linha (Polyline) que vai desenhar a trilha
        trilhaPolyline = mMap.addPolyline(new PolylineOptions()
                .width(10f)
                .color(Color.parseColor("#2E7D32")));

        verificarPermissaoECentralizar();
    }

    private void verificarPermissaoECentralizar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        // Pega a última localização conhecida só para abrir o mapa onde o usuário já está
        /*fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && mMap != null) {
                LatLng posInicial = new LatLng(location.getLatitude(), location.getLongitude());
                atualizarMarcadorECamera(posInicial, location);
            }
        });
    }*/

    private void iniciarTrilha() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            verificarPermissaoECentralizar();
            return;
        }

        // Zera os dados para uma nova gravação
        gravando = true;
        listaPontos.clear();
        trilhaPolyline.setPoints(listaPontos);
        distanciaTotalKm = 0.0;
        velocidadeMaximaKmh = 0f;
        ultimaLocalizacao = null;

        dataHoraInicio = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        tempoInicioMs = SystemClock.elapsedRealtime();
        timerHandler.post(timerRunnable);

        btnIniciarTrilha.setEnabled(false);
        btnFinalizarTrilha.setEnabled(true);
        Toast.makeText(this, getString(R.string.reg_hike_started), Toast.LENGTH_SHORT).show();

        // Pede atualizações de GPS a cada 3 segundos ou 2 metros deslocados
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateDistanceMeters(2f)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void configurarLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location == null) continue;

                    LatLng pontoAtual = new LatLng(location.getLatitude(), location.getLongitude());

                    // Calcula distância percorrida desde o ponto anterior
                    if (ultimaLocalizacao != null) {
                        float metros = ultimaLocalizacao.distanceTo(location);
                        distanciaTotalKm += (metros / 1000.0);
                    }
                    ultimaLocalizacao = location;

                    // Calcula velocidade atual (m/s -> km/h) e velocidade máxima
                    float velAtualKmh = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f;
                    if (velAtualKmh > velocidadeMaximaKmh) {
                        velocidadeMaximaKmh = velAtualKmh;
                    }

                    // Atualiza os textos da tela
                    tvRegDistancia.setText(getString(R.string.reg_distance_format, distanciaTotalKm));
                    tvRegVelocidade.setText(getString(R.string.reg_speed_format, velAtualKmh, velocidadeMaximaKmh));

                    // Adiciona o ponto na lista e redesenha a linha no mapa
                    listaPontos.add(pontoAtual);
                    trilhaPolyline.setPoints(listaPontos);

                    // Move o cursor e ajusta a câmera
                    atualizarMarcadorECamera(pontoAtual, location);
                }
            }
        };
    }

    private void atualizarMarcadorECamera(LatLng pontoAtual, Location location) {
        if (mMap == null) return;

        // 1. Cria o cursor na primeira vez; nas próximas, apenas move e rotaciona
        if (marcadorUsuario == null) {
            MarkerOptions options = new MarkerOptions()
                    .position(pontoAtual)
                    .title(getString(R.string.reg_you_marker))
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marcadorUsuario = mMap.addMarker(options);
        } else {
            marcadorUsuario.setPosition(pontoAtual);
            if (location.hasBearing()) {
                marcadorUsuario.setRotation(location.getBearing());
            }
        }

        // 2. Ajusta a câmera conforme o modo escolhido (Course Up vs North Up)
        if (tipoNavegacao.equals(ConfigurationActivity.NAV_COURSE_UP) && location.hasBearing()) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(pontoAtual)
                    .zoom(17f)
                    .bearing(location.getBearing())
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pontoAtual, 17f));
        }
    }

    private void finalizarTrilha() {
        gravando = false;
        timerHandler.removeCallbacks(timerRunnable);
        fusedLocationClient.removeLocationUpdates(locationCallback);

        long duracaoTotalMs = SystemClock.elapsedRealtime() - tempoInicioMs;
        String duracaoFormatada = formatarTempo(duracaoTotalMs);

        // Calcula velocidade média (km / horas)
        double horas = duracaoTotalMs / 3600000.0;
        double velocidadeMediaKmh = (horas > 0) ? (distanciaTotalKm / horas) : 0.0;

        // TODO: Salvar no banco SQLite aqui usando:
        // dataHoraInicio, duracaoFormatada, distanciaTotalKm, velocidadeMediaKmh, velocidadeMaximaKmh e listaPontos

        btnIniciarTrilha.setEnabled(true);
        btnFinalizarTrilha.setEnabled(false);

        Toast.makeText(this, getString(R.string.reg_hike_saved, distanciaTotalKm), Toast.LENGTH_LONG).show();
        finish(); // Volta para a ConfigurationActivity
    }

    private String formatarTempo(long millis) {
        long segundosTotais = millis / 1000;
        long horas = segundosTotais / 3600;
        long minutos = (segundosTotais % 3600) / 60;
        long segundos = segundosTotais % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", horas, minutos, segundos);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verificarPermissaoECentralizar();
            } else {
                Toast.makeText(this, getString(R.string.reg_permission_denied), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}