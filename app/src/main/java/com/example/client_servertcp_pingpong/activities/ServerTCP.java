package com.example.client_servertcp_pingpong.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.client_servertcp_pingpong.R;
import com.example.client_servertcp_pingpong.model.Jogador;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ServerTCP extends AppCompatActivity {

    TextView tvServerStatus, tvEscolha, tvAviso, tvInfoServer, tvEsperaServer, tvNumConectados;
    EditText edtCepServer;
    ServerSocket welcomeSocket;
    DataOutputStream socketOutput;
    DataInputStream fromClient;
    Button btLigarServer, btCepServer;
    RadioGroup radioGroup;
    RadioButton radioButton;
    Jogador jogadorServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        tvServerStatus = (TextView) findViewById(R.id.tvServerStatus);
        tvEscolha = (TextView) findViewById(R.id.tvEscolha);
        tvAviso = (TextView) findViewById(R.id.tvAviso);
        tvInfoServer = (TextView) findViewById(R.id.tvInfoServer);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        btLigarServer = (Button) findViewById(R.id.btLigarServer);
        tvNumConectados = (TextView) findViewById(R.id.tvNumConectados);
        edtCepServer = (EditText) findViewById(R.id.edtCepServer);
        btCepServer = (Button) findViewById(R.id.btCepServer);
        tvEsperaServer = (TextView) findViewById(R.id.tvEsperaServer);
        jogadorServer = new Jogador();
        jogadorServer.setServer(true);
        Log.v("PDM", "isServer? " + jogadorServer.isServer());
    }

    public void onClickLigarServer(View v) {
        int selectedId = radioGroup.getCheckedRadioButtonId();//pega o id do Radio Button selecionado
        radioButton = (RadioButton) findViewById(selectedId);
        if (radioButton != null) {
            if (radioButton.getText().toString().compareTo("Morlock") == 0) {
                jogadorServer.setMorlock(true);
                ConnectivityManager connManager;
                connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                Network[] networks = connManager.getAllNetworks();
                for (Network minhaRede : networks) {
                    NetworkInfo netInfo = connManager.getNetworkInfo(minhaRede);
                    if (netInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                        NetworkCapabilities propDaRede = connManager.getNetworkCapabilities(minhaRede);
                        if (propDaRede.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                            String macAddress = wifiManager.getConnectionInfo().getMacAddress();
                            Log.v("PDM", "Wi-Fi - MAC:" + macAddress);
                            int ip = wifiManager.getConnectionInfo().getIpAddress();
                            String ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
                            Log.v("PDM", "Wi-Fi - IP:" + ipAddress);
                            tvServerStatus.setText("Ativo em: " + ipAddress + ":9090");
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    ligarServerCodigo();
                                }
                            });
                            t.start();
                        }
                    }
                }
            } else {
                jogadorServer.setMorlock(false);
                ConnectivityManager connManager;
                connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                Network[] networks = connManager.getAllNetworks();
                for (Network minhaRede : networks) {
                    NetworkInfo netInfo = connManager.getNetworkInfo(minhaRede);
                    if (netInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                        NetworkCapabilities propDaRede = connManager.getNetworkCapabilities(minhaRede);
                        if (propDaRede.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                            String macAddress = wifiManager.getConnectionInfo().getMacAddress();
                            Log.v("PDM", "Wi-Fi - MAC:" + macAddress);
                            int ip = wifiManager.getConnectionInfo().getIpAddress();
                            String ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
                            Log.v("PDM", "Wi-Fi - IP:" + ipAddress);
                            tvServerStatus.setText("Ativo em: " + ipAddress);
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    ligarServerCodigo();
                                }
                            });
                            t.start();
                        }
                    }
                }
            }
        } else {
            tvAviso.setText("Selecione uma raça");
            tvAviso.setVisibility(View.VISIBLE);
        }
    }

    public void ligarServerCodigo() {
        //Desabilitar o Botão de Ligar
        btLigarServer.post(new Runnable() {
            @Override
            public void run() {
                btLigarServer.setVisibility(View.INVISIBLE);
                btLigarServer.setEnabled(false);
                tvNumConectados.setVisibility(View.VISIBLE);
                tvEscolha.setVisibility(View.INVISIBLE);
                tvAviso.setVisibility(View.INVISIBLE);
                radioGroup.setVisibility(View.INVISIBLE);
                if (jogadorServer.isMorlock()) {
                    tvInfoServer.setText("Você é um Morlock.\nSeu objetivo é encontrar o Eloi. Você sabe onde está a máquina do tempo, mas ela não lhe interessa.\nDigite o CEP da máquina do tempo:");
                } else {
                    tvInfoServer.setText("Você é um Eloi.\nSeu objetivo é encontrar a máquina do tempo para escapar do Morlock.\nDigite o CEP da sua localização atual:");
                }
                tvInfoServer.setVisibility(View.VISIBLE);
                edtCepServer.setVisibility(View.VISIBLE);
                btCepServer.setVisibility(View.VISIBLE);
            }
        });
        String CEPOponente = "";
        String result = "";
        try {
            Log.v("PDM", "Ligando o Server");
            welcomeSocket = new ServerSocket(9090);//<----------------------------------------ServerSocket
            Socket connectionSocket = welcomeSocket.accept();//<-----em espera
            Log.v("PDM", "Nova conexão");
            atualizarStatus();
            //Instanciando os canais de stream
            fromClient = new DataInputStream(connectionSocket.getInputStream());//<------------fromClient - recebimento de dados
            socketOutput = new DataOutputStream(connectionSocket.getOutputStream());//<--------socketOutput - envio de dados
            //----------------------------------------------------------------------------------------------------------------//
            // while (continuarRodando) {
            informarRaca();//envio
            Log.v("PDM", "1. CEP Cliente: " + jogadorServer.getCEP() + ", CEP Oponente: " + jogadorServer.getCEPOponente());
            Log.v("PDM", "2. CEP Cliente: " + jogadorServer.getCEP() + ", CEP Oponente: " + jogadorServer.getCEPOponente());
            if (jogadorServer.getCEPOponente() == null) {
                result = fromClient.readUTF();
                Log.v("PDM", "Result = " + result);
                if (result.compareTo("") != 0) {
                    CEPOponente = result;
                }
                if (CEPOponente != null) {
                    jogadorServer.setCEPOponente(CEPOponente);
                }
            }
            Log.v("PDM", "CEPOponente = " + CEPOponente);
            Log.v("PDM", "3. CEP Cliente: " + jogadorServer.getCEP() + ", CEP Oponente: " + jogadorServer.getCEPOponente());
            if (jogadorServer.getCEP() != null && jogadorServer.getCEPOponente() != null) {
                Log.v("PDM", "Abrindo jogo");
                gameStart();
            }
            //}
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void informarRaca() {//informa raça do Server
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socketOutput != null) {
                        socketOutput.writeBoolean(jogadorServer.isMorlock());/////////////////////////////////////////////
                        socketOutput.flush();
                    } else {
                        tvServerStatus.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ServerTCP.this, "Cliente ou Servidor desconectado", Toast.LENGTH_SHORT).show();
                            }
                        });
                        btLigarServer.post(new Runnable() {
                            @Override
                            public void run() {
                                btLigarServer.setEnabled(true);
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

    public void onClickConfirmarCepServer(View v) {
        //https://viacep.com.br/ws/60115222/json/
        if (tvNumConectados.getText().toString().compareTo("1 jogador conectado") == 0) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    final String CEP = edtCepServer.getText().toString();
                    try {
                        URL url = new URL("https://viacep.com.br/ws/" + CEP + "/json/");
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();//abertura da conexão TCP
                        conn.setReadTimeout(10000);//timeout da conexão
                        conn.setConnectTimeout(15000);//para ficar esperando
                        conn.setRequestMethod("GET");//serviço esperando uma conexão do tipo "GET"
                        Log.v("PDM", "CEP: " + CEP);
                        //RECEPÇÃO
                        String[] resultRest = new String[1];
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
                            Log.v("PDM", resultRest[0]);
                            if (resultRest[0].compareTo("{\"erro\": true}") == 0) {
                                tvEsperaServer.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvEsperaServer.setText("CEP inexistente");
                                        tvEsperaServer.setVisibility(View.VISIBLE);
                                    }
                                });
                            } else {
                                tvEsperaServer.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvEsperaServer.setText("Em espera");//à espera do outro jogador
                                        tvEsperaServer.setVisibility(View.VISIBLE);
                                        btCepServer.setEnabled(false);
                                        btCepServer.setVisibility(View.INVISIBLE);
                                    }
                                });
                                Log.v("PDM", "CEP: " + CEP);
                                atualizarCEP(CEP);
                            }
                        } else {
                            tvEsperaServer.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvEsperaServer.setText("Digite um CEP válido");
                                    tvEsperaServer.setVisibility(View.VISIBLE);
                                }
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
            t.start();
        } else {
            tvEsperaServer.post(new Runnable() {
                @Override
                public void run() {
                    tvEsperaServer.setText("Não há jogador conectado");
                    tvEsperaServer.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public void atualizarStatus() {
        tvNumConectados.post(new Runnable() {
            @Override
            public void run() {
                tvNumConectados.setText("1 jogador conectado");
            }
        });
    }

    public void atualizarCEP(String cep) {
        jogadorServer.setCEP(cep);
        //Thread t = new Thread(new Runnable() {
        //@Override
        //public void run() {
        try {
            if (socketOutput != null) {
                Log.v("PDM", "4. CEP Cliente: " + jogadorServer.getCEP() + ", CEP Oponente: " + jogadorServer.getCEPOponente());
                Log.v("PDM", "enviando cep");
                socketOutput.writeUTF(jogadorServer.getCEP());
                socketOutput.flush();
                if (jogadorServer.getCEP() != null && jogadorServer.getCEPOponente() != null) {
                    Log.v("PDM", "Abrindo jogo");
                    gameStart();
                }
            } else {
                Log.v("PDM", "else");
                btCepServer.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ServerTCP.this, "Nenhum jogador conectado", Toast.LENGTH_LONG).show();
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
    //}

    public void gameStart() {
        Intent intent = new Intent(ServerTCP.this, Jogo.class);
        intent.putExtra("jogador", jogadorServer);
        startActivity(intent);
        try {
            Log.v("PDM", "try close");
            socketOutput.close();
            fromClient.close();
        } catch (IOException e) {
            Log.v("PDM", "catch close");
            e.printStackTrace();
        }
        Log.v("PDM", "Fechando Activity ServerTCP");
        finish();
    }


}
