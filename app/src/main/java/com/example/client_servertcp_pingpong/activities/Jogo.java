package com.example.client_servertcp_pingpong.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.client_servertcp_pingpong.R;
import com.example.client_servertcp_pingpong.model.Jogador;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Jogo extends AppCompatActivity {
    Jogador jogador;
    EditText edtCepInicio;
    TextView tvCepFim, tvLogradouro, tvCidade, tvStatus, tvPontuacao, tvTentativas, tvAvisoNum;
    Button btOk;
    String cepInicio, cepFim;
    int numInserido, numReal;
    int pts = 1000;
    int tents = 0;
    int tempo = 0;
    ServerSocket welcomeSocket;
    Socket clientSocket;
    DataInputStream socketInput;
    DataOutputStream socketOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jogo);
        tvCepFim = (TextView) findViewById(R.id.tvCepFim);
        tvAvisoNum = (TextView) findViewById(R.id.tvAvisoNum);
        tvLogradouro = (TextView) findViewById(R.id.tvLogradouro);
        tvCidade = (TextView) findViewById(R.id.tvCidade);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvPontuacao = (TextView) findViewById(R.id.tvPontuacao);
        tvTentativas = (TextView) findViewById(R.id.tvTentativas);
        edtCepInicio = (EditText) findViewById(R.id.edtCepInicio);
        btOk = (Button) findViewById(R.id.btOk);

        //Dados da Intent anterior
        Intent quemChamou = this.getIntent();
        if (quemChamou != null) {
            Bundle params = quemChamou.getExtras();
            if (params != null) {
                //Recuperando instância do jogador
                jogador = (Jogador) params.getSerializable("jogador");
                if (jogador != null) {
                    jogador.setnPontos(pts);
                    jogador.setnPontosOponente(0);
                    jogador.setnTentativas(tents);
                    jogador.setTempoDoJogador(tempo);
                    jogador.setFinished(false);
                    jogador.setOpponentFinished(false);
                    cepInicio = jogador.getCEPOponente().substring(0,3);
                    numReal = Integer.parseInt(cepInicio);
                    cepFim = jogador.getCEPOponente().substring(3);
                    tvCepFim.setText(cepFim);
                    Log.v("PDM", "É Server? "+ jogador.isServer());
                    Log.v("PDM", "É Morlock? "+ jogador.isMorlock());//não zerar
                    Log.v("PDM", "CEP Jogador: "+ jogador.getCEP());//não zerar
                    Log.v("PDM", "CEP Oponente: "+ jogador.getCEPOponente());//não zerar
                    Log.v("PDM", "IP Server: "+ jogador.getIP());//não zerar
                    Log.v("PDM", "Porta: "+ jogador.getPorta());//não zerar
                    Log.v("PDM", "Nº de Pontos Jogador: "+ jogador.getnPontos());//gravar só no final
                    Log.v("PDM", "Nº de Pontos Oponente: "+ jogador.getnPontosOponente());//receber só no final
                    Log.v("PDM", "Nº Tentativas: "+ jogador.getnTentativas());//gravar só no final
                    Log.v("PDM", "Tempo: "+ jogador.getTempoDoJogador());//gravar só no final
                    Log.v("PDM", "Jogador Terminou? "+ jogador.isFinished());//enviar só no final
                    Log.v("PDM", "Oponente Terminou? "+ jogador.isOpponentFinished());//receber só no final
                }
            }
        }



    }

    public void onClickOk(View v){
        //https://viacep.com.br/ws/60115222/json/
        String num = edtCepInicio.getText().toString();//número inserido em forma de String
        numInserido = Integer.parseInt(num);//número inserido em forma de int
        if (numReal == numInserido){
            tvStatus.setText("ACERTOU! EM ESPERA PARA COMPARAÇÃO DE PONTOS");
            btOk.setEnabled(false);
            btOk.setVisibility(View.INVISIBLE);
            tents++;
            tvTentativas.setText(tents);
            jogador.setFinished(true);
        } else {
            pts = pts - (tents*5);
            tents++;
            tvPontuacao.setText(pts);
            tvTentativas.setText(tents);
            if (numReal > numInserido) {
                tvStatus.setText("MAIOR");
            } else {
                tvStatus.setText("MENOR");
            }
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final String CEP = edtCepInicio.getText().toString() + cepFim;
                try {
                    URL url = new URL("https://viacep.com.br/ws/" + CEP + "/json/");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();//abertura da conexão TCP
                    conn.setReadTimeout(10000);//timeout da conexão
                    conn.setConnectTimeout(15000);//para ficar esperando
                    conn.setRequestMethod("GET");//serviço esperando uma conexão do tipo "GET"
                    //RECEPÇÃO
                    String resultRest[] = new String[1];
                    int responseCode = conn.getResponseCode();//vai receber a resposta dessa conexão
                    //nesse momento vai ficar bloqueado esperando o servidor mandar as respostas
                    if (responseCode == HttpsURLConnection.HTTP_OK) {//só entra aqui se o código retornado for 200
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        resultRest[0] = response.toString();
                        if (resultRest[0].compareTo("{\"erro\": true}") == 0) {
                            tvAvisoNum.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvAvisoNum.setVisibility(View.INVISIBLE);
                                    tvLogradouro.setText("Não existe");
                                    tvCidade.setText("Não existe");
                                }
                            });
                        } else {
                            JSONObject respostaJSON = new JSONObject(resultRest[0]);
                            final String logradouro = respostaJSON.getString("logradouro");
                            final String cidade = respostaJSON.getString("localidade");
                            tvAvisoNum.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvAvisoNum.setVisibility(View.INVISIBLE);
                                    tvLogradouro.setText(logradouro);
                                    tvCidade.setText(cidade);
                                }
                            });
                        }
                    } else {
                        tvAvisoNum.post(new Runnable() {
                            @Override
                            public void run() {
                                tvAvisoNum.setVisibility(View.VISIBLE);
                                tvLogradouro.setText("Não existe");
                                tvCidade.setText("Não existe");
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

    }

    public void comunicacaoSocket(){
        if (jogador.isServer()){
            serverCodigo();
        } else {
            clientCodigo();
        }
    }

    public void serverCodigo(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    welcomeSocket = new ServerSocket(9090);
                    Socket connectionSocket = welcomeSocket.accept();//<-----em espera
                    socketInput = new DataInputStream(connectionSocket.getInputStream());
                    socketOutput = new DataOutputStream(connectionSocket.getOutputStream());
                    boolean isClientFinished = socketInput.readBoolean();
                    jogador.setOpponentFinished(isClientFinished);

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public void clientCodigo(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket = new Socket(jogador.getIP(), jogador.getPorta());
                    socketOutput = new DataOutputStream(clientSocket.getOutputStream());
                    socketInput = new DataInputStream(clientSocket.getInputStream());
                    boolean isServerFinished = socketInput.readBoolean();
                    jogador.setOpponentFinished(isServerFinished);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public void enviarFinished(){
        try {
            if (socketOutput != null){
                Log.v("PDM", "enviando vitória");
                socketOutput.writeBoolean(jogador.isFinished());
                socketOutput.flush();
                if ((jogador.isFinished() == true) && (jogador.isOpponentFinished() == true)) {
                    Log.v("PDM", "Abrindo Tela Vitória");
                    telaVitoria();
                }
            } else {
                Log.v("PDM", "else");
                btOk.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Jogo.this, "Nenhum jogador conectado", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.v("PDM", "catch");
            e.printStackTrace();
        }
    }

    public void telaVitoria(){
        Intent intent = new Intent(Jogo.this, TelaVitoria.class);
        intent.putExtra("jogador", jogador);
        startActivity(intent);
        try {
            Log.v("PDM", "try close");
            socketOutput.close();
            socketInput.close();
        } catch (IOException e) {
            Log.v("PDM", "catch close");
            e.printStackTrace();
        }
        Log.v("PDM", "Fechando Activity Jogo");
        finish();
    }






}