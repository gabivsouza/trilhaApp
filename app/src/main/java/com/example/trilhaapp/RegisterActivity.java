package com.example.trilhaapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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

    private static final int REQUEST_LOCATION_PERMISSION = 100;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Marker marcadorUsuario;
    private Circle circuloAcuracia;
    private Polyline trilhaPolyline;
    private final List<LatLng> listaPontos = new ArrayList<>();

    // Configurações lidas da ConfigurationActivity
    private String tipoMapa;
    private String tipoNavegacao;

    // Variáveis de controle da trilha
    private boolean gravando = false;
    private long tempoInicioMs = 0L;
    private long timestampInicioEpoch = 0L;
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

        // 1. Aplica o tipo de mapa escolhido (Vetorial ou Satélite)
        if (tipoMapa.equals(ConfigurationActivity.MAP_SATELITE)) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        // 2. Habilita controles de zoom e bússola na tela
        UiSettings mapUI = mMap.getUiSettings();
        mapUI.setZoomControlsEnabled(true);
        mapUI.setCompassEnabled(true);

        // 3. Prepara a linha (Polyline) que vai desenhar o trajeto da trilha
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
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        // Centraliza o mapa na última localização conhecida ao abrir a tela
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && mMap != null) {
                LatLng posInicial = new LatLng(location.getLatitude(), location.getLongitude());
                atualizarMarcadorECamera(posInicial, location);
            }
        });
    }

    private void iniciarTrilha() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            verificarPermissaoECentralizar();
            return;
        }

        // Zera os dados para iniciar uma nova gravação
        gravando = true;
        listaPontos.clear();
        trilhaPolyline.setPoints(listaPontos);
        distanciaTotalKm = 0.0;
        velocidadeMaximaKmh = 0f;
        ultimaLocalizacao = null;

        timestampInicioEpoch = System.currentTimeMillis();
        dataHoraInicio = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                .format(new Date(timestampInicioEpoch));
        tempoInicioMs = SystemClock.elapsedRealtime();
        timerHandler.post(timerRunnable);

        btnIniciarTrilha.setEnabled(false);
        btnFinalizarTrilha.setEnabled(true);
        Toast.makeText(this, getString(R.string.reg_hike_started), Toast.LENGTH_SHORT).show();

        // Solicita atualizações de GPS a cada 3 segundos ou 2 metros deslocados
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

                    // Calcula velocidade instantânea (m/s -> km/h) e velocidade máxima
                    float velAtualKmh = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f;
                    if (velAtualKmh > velocidadeMaximaKmh) {
                        velocidadeMaximaKmh = velAtualKmh;
                    }

                    // Atualiza os textos sobrepostos ao mapa
                    tvRegDistancia.setText(getString(R.string.reg_distance_format, distanciaTotalKm));
                    tvRegVelocidade.setText(getString(R.string.reg_speed_format, velAtualKmh, velocidadeMaximaKmh));

                    // Adiciona o ponto na lista e redesenha a linha no mapa
                    listaPontos.add(pontoAtual);
                    trilhaPolyline.setPoints(listaPontos);

                    // Atualiza marcador personalizado, círculo de acurácia e câmera
                    atualizarMarcadorECamera(pontoAtual, location);
                }
            }
        };
    }

    private BitmapDescriptor obterIconePersonalizado(int drawableId, int larguraDp, int alturaDp) {
        // Converte dp para pixels de acordo com a tela do celular
        float densidade = getResources().getDisplayMetrics().density;
        int larguraPx = Math.round(larguraDp * densidade);
        int alturaPx = Math.round(alturaDp * densidade);

        // Carrega o PNG da pasta drawable e redimensiona
        android.graphics.Bitmap imagemOriginal = android.graphics.BitmapFactory.decodeResource(getResources(), drawableId);
        android.graphics.Bitmap imagemRedimensionada = android.graphics.Bitmap.createScaledBitmap(
                imagemOriginal, larguraPx, alturaPx, false
        );

        return BitmapDescriptorFactory.fromBitmap(imagemRedimensionada);
    }

    private void atualizarMarcadorECamera(LatLng pontoAtual, Location location) {
        if (mMap == null) return;

        // 1. Marcador personalizado indicando a posição do usuário
        if (marcadorUsuario == null) {
            MarkerOptions options = new MarkerOptions()
                    .position(pontoAtual)
                    .title(getString(R.string.reg_you_marker))
                    .anchor(0.5f, 0.5f)
                    .icon(obterIconePersonalizado(R.drawable.ic_marcador_jojo, 52, 52));
            marcadorUsuario = mMap.addMarker(options);
        } else {
            marcadorUsuario.setPosition(pontoAtual);
        }

        // 2. Círculo semitransparente indicando a acurácia do GPS
        float acuraciaMetros = location.hasAccuracy() ? location.getAccuracy() : 10f;
        if (circuloAcuracia == null) {
            circuloAcuracia = mMap.addCircle(new CircleOptions()
                    .center(pontoAtual)
                    .radius(acuraciaMetros)
                    .strokeWidth(2f)
                    .strokeColor(Color.parseColor("#1565C0"))
                    .fillColor(Color.argb(55, 33, 150, 243)));
        } else {
            circuloAcuracia.setCenter(pontoAtual);
            circuloAcuracia.setRadius(acuraciaMetros);
        }

        // 3. Orientação do mapa: Course Up (rumo) vs North Up (Norte)
        if (tipoNavegacao.equals(ConfigurationActivity.NAV_COURSE_UP) && location.hasBearing()) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(pontoAtual)
                    .zoom(17f)
                    .bearing(location.getBearing())
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(pontoAtual)
                    .zoom(17f)
                    .bearing(0f) // Garante orientação Norte para cima
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private void finalizarTrilha() {
        gravando = false;
        timerHandler.removeCallbacks(timerRunnable);
        fusedLocationClient.removeLocationUpdates(locationCallback);

        long duracaoTotalMs = SystemClock.elapsedRealtime() - tempoInicioMs;
        String duracaoFormatada = formatarTempo(duracaoTotalMs);
        String dataHoraFim = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Calcula velocidade média (km / horas)
        double horas = duracaoTotalMs / 3600000.0;
        double velocidadeMediaKmh = (horas > 0) ? (distanciaTotalKm / horas) : 0.0;

        // Solicita o nome da trilha ao usuário e salva nas 2 tabelas do SQLite
        EditText inputNome = new EditText(this);
        inputNome.setHint("Ex: Trilha da Serra");

        new AlertDialog.Builder(this)
                .setTitle("Salvar Trilha")
                .setMessage("Informe um nome para a trilha registrada:")
                .setView(inputNome)
                .setCancelable(false)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String nomeTrilha = inputNome.getText().toString().trim();
                    if (nomeTrilha.isEmpty()) {
                        nomeTrilha = "Trilha " + dataHoraInicio;
                    }

                    HikeHelp dbHelper = new HikeHelp(this);
                    dbHelper.salvarTrilhaCompleta(
                            nomeTrilha,
                            dataHoraInicio,
                            dataHoraFim,
                            timestampInicioEpoch,
                            duracaoFormatada,
                            distanciaTotalKm,
                            velocidadeMediaKmh,
                            velocidadeMaximaKmh,
                            listaPontos
                    );

                    Toast.makeText(this, getString(R.string.reg_hike_saved, distanciaTotalKm), Toast.LENGTH_LONG).show();
                    finish();
                })
                .show();
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