package com.example.client_servertcp_pingpong.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.client_servertcp_pingpong.R;
import com.example.client_servertcp_pingpong.model.Jogador;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ClienteTCP extends AppCompatActivity {
    TextView tvStatus, tvInfoClient, tvEsperaClient;
    Socket clientSocket;
    DataOutputStream socketOutput;
    DataInputStream fromServer;
    Button btConectar, btCepClient;
    EditText edtIp, edtPort, edtCepClient;
    Jogador jogadorClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente);
        tvStatus = (TextView) findViewById(R.id.tvStatusClient);
        tvInfoClient = (TextView) findViewById(R.id.tvInfoClient);
        btConectar = (Button) findViewById(R.id.btConectar);
        edtIp = (EditText) findViewById(R.id.edtIP);
        edtPort = (EditText) findViewById(R.id.edtPort);
        edtCepClient = (EditText) findViewById(R.id.edtCepClient);
        btCepClient = (Button) findViewById(R.id.btCepClient);
        tvEsperaClient = (TextView) findViewById(R.id.tvEsperaClient);
        jogadorClient = new Jogador();
        jogadorClient.setServer(false);
        Log.v("PDM", "isServer? " + jogadorClient.isServer());
    }

    public void onClickConectar(View v) {
        ConnectivityManager connManager;
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connManager.getAllNetworks();
        for (Network minhaRede : networks) {
            NetworkInfo netInfo = connManager.getNetworkInfo(minhaRede);
            if (netInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                NetworkCapabilities propDaRede = connManager.getNetworkCapabilities(minhaRede);
                if (propDaRede.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            conectarCodigo();
                        }
                    });
                    t.start();
                }
            }
        }
    }

    public void conectarCodigo() {
        final String ip = edtIp.getText().toString();
        final int porta = Integer.parseInt(edtPort.getText().toString());
        tvStatus.post(new Runnable() {
            @Override
            public void run() {
                tvStatus.setText("Conectando em " + ip + ":9090");
            }
        });
        String CEPOponente = "";
        String result = "";
        try {
            clientSocket = new Socket(ip, porta);//<---------------------------conecta Cliente por Socket
            tvStatus.post(new Runnable() {
                @Override
                public void run() {
                    tvStatus.setText("Conectado com " + ip + ":" + porta);
                    btConectar.setEnabled(false);
                    btConectar.setVisibility(View.INVISIBLE);
                }
            });
            jogadorClient.setIP(ip);
            jogadorClient.setPorta(porta);
            socketOutput =
                    new DataOutputStream(clientSocket.getOutputStream());//<------------socketOutput - envio de dados
            fromServer =
                    new DataInputStream(clientSocket.getInputStream());//<-------------fromServer - recebimento de dados
            //------------------------------------------------------------------------------------------------------------------//
            //while (fromServer != null) {
            Log.v("PDM", "socketOutput1: " + socketOutput);
            boolean isMorlock = fromServer.readBoolean();//leitura
            receberRaca(isMorlock);
            Log.v("PDM", "1. CEP Cliente: " + jogadorClient.getCEP() + ", CEP Oponente: " + jogadorClient.getCEPOponente());
            Log.v("PDM", "2. CEP Cliente: " + jogadorClient.getCEP() + ", CEP Oponente: " + jogadorClient.getCEPOponente());
            if (jogadorClient.getCEPOponente() == null) {
                result = fromServer.readUTF();
                Log.v("PDM", "Result = " + result);
                if (result.compareTo("") != 0) {
                    CEPOponente = result;
                }
                if (CEPOponente != null) {
                    jogadorClient.setCEPOponente(CEPOponente);
                }
            }
            Log.v("PDM", "CEPOponente = " + CEPOponente);
            Log.v("PDM", "3. CEP Cliente: " + jogadorClient.getCEP() + ", CEP Oponente: " + jogadorClient.getCEPOponente());
            if (jogadorClient.getCEP() != null && jogadorClient.getCEPOponente() != null) {
                Log.v("PDM", "Abrindo jogo");
                gameStart();
            }

            // }
        } catch (Exception e) {
            tvStatus.post(new Runnable() {
                @Override
                public void run() {
                    tvStatus.setText("Erro na conexão com " + ip + ":" + porta);
                }
            });
            e.printStackTrace();
        }
    }


    public void receberRaca(boolean isMorlock) {
        if (isMorlock) {//vê se oponente é Morlock
            jogadorClient.setMorlock(false);//cliente é Eloi
        } else {//caso contrário
            jogadorClient.setMorlock(true);//cliente é Morlock
        }
        tvInfoClient.post(new Runnable() {
            @Override
            public void run() {
                if (jogadorClient.isMorlock()) {
                    tvInfoClient.setText("Você é um Morlock.\nSeu objetivo é encontrar o Eloi. Você sabe onde está a máquina do tempo, mas ela não lhe interessa.\nDigite o CEP da máquina do tempo:");
                } else {
                    tvInfoClient.setText("Você é um Eloi.\nSeu objetivo é encontrar a máquina do tempo para escapar do Morlock.\nDigite o CEP da sua localização atual:");
                }
                tvInfoClient.setVisibility(View.VISIBLE);
                edtCepClient.setVisibility(View.VISIBLE);
                btCepClient.setVisibility(View.VISIBLE);
            }
        });
    }

    public void onClickConfirmarCepClient(View v) {
        //https://viacep.com.br/ws/60115222/json/
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final String CEP = edtCepClient.getText().toString();
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
                            tvEsperaClient.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvEsperaClient.setText("CEP inexistente");
                                    tvEsperaClient.setVisibility(View.VISIBLE);
                                }
                            });
                        } else {
                            tvEsperaClient.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvEsperaClient.setText("Em espera");//à espera do outro jogador
                                    tvEsperaClient.setVisibility(View.VISIBLE);
                                    btCepClient.setEnabled(false);
                                    btCepClient.setVisibility(View.INVISIBLE);
                                }
                            });
                            Log.v("PDM", "CEP: " + CEP);
                            atualizarCEP(CEP);
                        }
                    } else {
                        tvEsperaClient.post(new Runnable() {
                            @Override
                            public void run() {
                                tvEsperaClient.setText("Digite um CEP válido");
                                tvEsperaClient.setVisibility(View.VISIBLE);
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


    public void gameStart() {
        Intent intent = new Intent(ClienteTCP.this, Jogo.class);
        intent.putExtra("jogador", jogadorClient);
        startActivity(intent);
        try {
            Log.v("PDM", "try close");
            socketOutput.close();
            fromServer.close();
        } catch (IOException e) {
            Log.v("PDM", "catch close");
            e.printStackTrace();
        }
        Log.v("PDM", "Fechando Activity ClienteTCP");
        finish();
    }

    public void atualizarCEP(String cep) {
        jogadorClient.setCEP(cep);
        //Thread t = new Thread(new Runnable() {
        // @Override
        //public void run() {
        try {
            if (socketOutput != null) {
                Log.v("PDM", "4. CEP Cliente: " + jogadorClient.getCEP() + ", CEP Oponente: " + jogadorClient.getCEPOponente());
                Log.v("PDM", "enviando cep");
                socketOutput.writeUTF(jogadorClient.getCEP());
                socketOutput.flush();
                if ((jogadorClient.getCEP() != null) && (jogadorClient.getCEPOponente() != null)) {
                    Log.v("PDM", "Abrindo jogo");
                    gameStart();
                }
            } else {
                Log.v("PDM", "else");
                btCepClient.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ClienteTCP.this, "Nenhum jogador conectado", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.v("PDM", "catch");
            e.printStackTrace();
        }
    }
    // });
    // t.start();
    // }

}