package com.example.trilhaapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ConsultActivity extends AppCompatActivity {

    private HikeHelp dbHelper;
    private ListView lvTrilhas;
    private TextView tvListaVazia;
    private List<Hike> listaTrilhas = new ArrayList<>();
    private TrilhaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consult);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dbHelper = new HikeHelp(this);
        lvTrilhas = findViewById(R.id.lvTrilhas);
        tvListaVazia = findViewById(R.id.tvListaVazia);
        Button btnApagarIntervalo = findViewById(R.id.btnApagarIntervalo);
        Button btnApagarTodas = findViewById(R.id.btnApagarTodas);

        adapter = new TrilhaAdapter();
        lvTrilhas.setAdapter(adapter);

        btnApagarIntervalo.setOnClickListener(v -> iniciarExclusaoPorIntervalo());
        btnApagarTodas.setOnClickListener(v -> confirmarApagarTodas());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarListaTrilhas();
    }

    private void carregarListaTrilhas() {
        listaTrilhas = dbHelper.listarTodasTrilhas();
        adapter.notifyDataSetChanged();
        tvListaVazia.setVisibility(listaTrilhas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Edita o nome da trilha selecionada
    private void exibirDialogoEditarNome(Hike trilha) {
        EditText inputNome = new EditText(this);
        inputNome.setText(trilha.getNome());
        inputNome.setSelection(trilha.getNome().length());

        new AlertDialog.Builder(this)
                .setTitle("Editar Nome da Trilha")
                .setView(inputNome)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String novoNome = inputNome.getText().toString().trim();
                    if (!novoNome.isEmpty()) {
                        dbHelper.atualizarNomeTrilha(trilha.getId(), novoNome);
                        carregarListaTrilhas();
                        Toast.makeText(this, "Nome atualizado!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Apaga uma trilha específica
    private void confirmarApagarTrilha(Hike trilha) {
        new AlertDialog.Builder(this)
                .setTitle("Apagar Trilha")
                .setMessage("Deseja apagar a trilha \"" + trilha.getNome() + "\"?")
                .setPositiveButton("Apagar", (dialog, which) -> {
                    dbHelper.apagarTrilha(trilha.getId());
                    carregarListaTrilhas();
                    Toast.makeText(this, "Trilha apagada.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Apaga trilhas em um determinado intervalo de datas (Data Inicial -> Data Final)
    private void iniciarExclusaoPorIntervalo() {
        Calendar hoje = Calendar.getInstance();
        Toast.makeText(this, "Selecione a DATA INICIAL", Toast.LENGTH_SHORT).show();

        new DatePickerDialog(this, (view, anoIni, mesIni, diaIni) -> {
            Calendar calInicio = Calendar.getInstance();
            calInicio.set(anoIni, mesIni, diaIni, 0, 0, 0);
            calInicio.set(Calendar.MILLISECOND, 0);

            Toast.makeText(this, "Selecione a DATA FINAL", Toast.LENGTH_SHORT).show();
            new DatePickerDialog(this, (view2, anoFim, mesFim, diaFim) -> {
                Calendar calFim = Calendar.getInstance();
                calFim.set(anoFim, mesFim, diaFim, 23, 59, 59);
                calFim.set(Calendar.MILLISECOND, 999);

                if (calFim.getTimeInMillis() < calInicio.getTimeInMillis()) {
                    Toast.makeText(this, "A data final deve ser maior ou igual à inicial.", Toast.LENGTH_LONG).show();
                    return;
                }

                int removidas = dbHelper.apagarTrilhasPorIntervalo(
                        calInicio.getTimeInMillis(),
                        calFim.getTimeInMillis()
                );
                carregarListaTrilhas();
                Toast.makeText(this, removidas + " trilha(s) apagada(s) no período.", Toast.LENGTH_LONG).show();

            }, hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), hoje.get(Calendar.DAY_OF_MONTH)).show();

        }, hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), hoje.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Apaga todas as trilhas registradas
    private void confirmarApagarTodas() {
        if (listaTrilhas.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Apagar Todas as Trilhas")
                .setMessage("Tem certeza que deseja apagar TODAS as trilhas registradas?")
                .setPositiveButton("Apagar Todas", (dialog, which) -> {
                    dbHelper.apagarTodasTrilhas();
                    carregarListaTrilhas();
                    Toast.makeText(this, "Todas as trilhas foram apagadas.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private class TrilhaAdapter extends BaseAdapter {
        @Override
        public int getCount() { return listaTrilhas.size(); }

        @Override
        public Object getItem(int position) { return listaTrilhas.get(position); }

        @Override
        public long getItemId(int position) { return listaTrilhas.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ConsultActivity.this)
                        .inflate(R.layout.item_hike, parent, false);
            }

            Hike trilha = listaTrilhas.get(position);

            TextView tvNome = convertView.findViewById(R.id.tvItemNome);
            TextView tvInfo = convertView.findViewById(R.id.tvItemInfo);
            Button btnConsultar = convertView.findViewById(R.id.btnItemConsultar);
            Button btnEditar = convertView.findViewById(R.id.btnItemEditar);
            Button btnApagar = convertView.findViewById(R.id.btnItemApagar);

            tvNome.setText(trilha.getNome());
            tvInfo.setText(String.format(Locale.getDefault(),
                    "Início: %s | Distância: %.2f km | Duração: %s",
                    trilha.getDataInicio(), trilha.getDistanciaKm(), trilha.getDuracao()));

            // Abre o mapa com o trajeto e dados da trilha
            btnConsultar.setOnClickListener(v -> {
                Intent intent = new Intent(ConsultActivity.this, ViewHikeActivity.class);
                intent.putExtra("TRILHA_ID", trilha.getId());
                startActivity(intent);
            });

            btnEditar.setOnClickListener(v -> exibirDialogoEditarNome(trilha));
            btnApagar.setOnClickListener(v -> confirmarApagarTrilha(trilha));

            return convertView;
        }
    }
}