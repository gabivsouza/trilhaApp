package com.example.trilhaapp;

public class Hike {
    private long id;
    private String nome;
    private String dataInicio;
    private String dataFim;
    private long timestampInicio;
    private String duracao;
    private double distanciaKm;
    private double velMedia;
    private double velMaxima;

    public Hike (long id, String nome, String dataInicio, String dataFim,
                  long timestampInicio, String duracao, double distanciaKm,
                  double velMedia, double velMaxima) {
        this.id = id;
        this.nome = nome;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.timestampInicio = timestampInicio;
        this.duracao = duracao;
        this.distanciaKm = distanciaKm;
        this.velMedia = velMedia;
        this.velMaxima = velMaxima;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDataInicio() { return dataInicio; }
    public String getDataFim() { return dataFim; }
    public long getTimestampInicio() { return timestampInicio; }
    public String getDuracao() { return duracao; }
    public double getDistanciaKm() { return distanciaKm; }
    public double getVelMedia() { return velMedia; }
    public double getVelMaxima() { return velMaxima; }
}