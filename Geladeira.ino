//"Esse código controla dois LEDs (Branco Quente e Branco Frio) e um sensor de temperatura DHT11 com um ESP32. 
//Os LEDs são controlados por PWM e o estado deles pode ser definido por um aplicativo conectado via Bluetooth ou por um potenciômetro. 
//O estado dos LEDs é armazenado na NVSRAM para que possa ser restaurado na próxima inicialização. 
//O sensor de temperatura DHT11 é lido a cada 5 segundos e o valor é enviado ao aplicativo via Bluetooth."

// Incluindo as bibliotecas necessárias
#include <BluetoothSerial.h>         // Biblioteca para conexão bluetooth
#include <DHT.h>                    // Biblioteca para o sensor DHT11 de temperatura e humidade
#include <Preferences.h>           // Biblioteca para salvar as ultimas definições na memória NVS 
                                  //(Non-Volatile Storage, usa uma parte da memória flash principal por meio de uma API interna.) do ESP
#include <PWMOutESP32.h>         // Biblioteca para controle PWM em ESP32

// Definindo os pinos
#define DHTPIN 5                  // Pino onde está o sensor DHT
#define DHTTYPE DHT11            // Tipo do sensor DHT

#define LED_WHITE_HOT_PIN 23       // Pino onde está o LED Branco Quente
#define LED_WHITE_COLD_PIN 22     // Pino onde está o LED Branco Frio
#define POT_PIN 36               // Pino onde está o potenciômetro

// Definindo os canais para o PWM dos LEDs
#define LEDC_CHANNEL_0 0
#define LEDC_CHANNEL_1 1

// Inicializando as classes necessárias
DHT dht(DHTPIN, DHTTYPE);           // Classe para lidar com o sensor DHT
BluetoothSerial SerialBT;          // Classe para lidar com o Bluetooth
Preferences preferences;          // Classe para lidar com as preferências (usado para armazenar estados de LED)
PWMOutESP32 pwm(10, 5000);       // Inicialização do PWM

// Variáveis globais
bool appConnected = false;                   // Variável para acompanhar se o aplicativo está conectado
bool advancedMode = false;                  // Variável para acompanhar se está no modo avançado
bool neutralState = false;                 // Variável para acompanhar se está no estado neutro
bool appMoved = false;                    // Variável para acompanhar se o aplicativo foi movido
bool potMovedAfterDisconnect = false;    // Variável para acompanhar se o potenciômetro foi movido depois que o aplicativo desconectou
int lastPotValue = 0;                   // Variável para armazenar o último valor do potenciômetro
int ledWhiteHotState = 0;              // Variável para armazenar o estado do LED Branco Quente
int ledWhiteColdState = 0;            // Variável para armazenar o estado do LED Branco Frio
unsigned long lastTempCheckTime = 0; // Variável para armazenar a última vez que a temperatura foi verificada

// Função para ler o potenciômetro
int readPot() {
  const int numReadings = 10;     // Número de leituras para fazer a média
  int total = 0;                 // Total das leituras
  for (int i = 0; i < numReadings; i++) {
    total += analogRead(POT_PIN);
    delay(1); // Curto atraso para o ADC estabilizar
  }
  return total / numReadings;    // Retorna a média
}

void setup() {
  Serial.begin(115200);            // Inicia a comunicação serial
  SerialBT.begin("ESP32test");    // Inicia o Bluetooth com o nome "ESP32test"
  dht.begin();                   // Inicia o sensor DHT

  // Ciclo de fade in e fade out para o LED quente
  for (int fadeValue = 0; fadeValue <= pwm.getMaxPWMValue(); fadeValue++) {
    pwm.analogWrite(LED_WHITE_HOT_PIN, fadeValue);
    delay(2);
  }
  
  // Ciclo de fade out e fade in para o LED quente
  for (int fadeValue = pwm.getMaxPWMValue(); fadeValue >= 0; fadeValue--) {
    pwm.analogWrite(LED_WHITE_HOT_PIN, fadeValue);
    delay(2);
  }
  
  // Mesmo ciclo de fade para o LED frio
  for (int fadeValue = 0; fadeValue <= pwm.getMaxPWMValue(); fadeValue++) {
    pwm.analogWrite(LED_WHITE_COLD_PIN, fadeValue);
    delay(2);
  }

  for (int fadeValue = pwm.getMaxPWMValue(); fadeValue >= 0; fadeValue--) {
    pwm.analogWrite(LED_WHITE_COLD_PIN, fadeValue);
    delay(2);
  }


  // Configuração do PWM para os LEDs
  ledcSetup(LEDC_CHANNEL_0, 5000, 10);  // Configura o PWM para o canal 0, frequência de 5 kHz e resolução de 10 bits
  ledcSetup(LEDC_CHANNEL_1, 5000, 10); // Configura o PWM para o canal 1, frequência de 5 kHz e resolução de 10 bits
  ledcAttachPin(LED_WHITE_HOT_PIN, LEDC_CHANNEL_0);   // Atribui o LED Branco Quente ao canal 0
  ledcAttachPin(LED_WHITE_COLD_PIN, LEDC_CHANNEL_1); // Atribui o LED Branco Frio ao canal 1

  // Carregar os últimos estados dos LEDs da NVSRAM
  preferences.begin("LEDstate", false); // Inicia as preferências
  ledWhiteHotState = preferences.getInt("hot", 0);    // Carrega o último estado do LED Branco Quente
  ledWhiteColdState = preferences.getInt("cold", 0); // Carrega o último estado do LED Branco Frio
  ledcWrite(LEDC_CHANNEL_0, ledWhiteHotState);   // Aplica o último estado ao LED Branco Quente
  ledcWrite(LEDC_CHANNEL_1, ledWhiteColdState); // Aplica o último estado ao LED Branco Frio

  // Lê o valor inicial do potenciômetro
  lastPotValue = analogRead(POT_PIN);
}

void loop() {
  // Verifica a conexão Bluetooth
  bool clientConnected = SerialBT.hasClient();
  if (clientConnected && !appConnected) {
    // O aplicativo acabou de se conectar
    appConnected = true;
    potMovedAfterDisconnect = false; // Reinicia a flag quando o aplicativo se conecta
  } else if (!clientConnected && appConnected) {
    // O aplicativo acabou de desconectar
    appConnected = false;
  }

  // Trata o comando Bluetooth
  if (appConnected && SerialBT.available()) {
    String command = SerialBT.readString();
    if (command == "10") {
      ledWhiteHotState = 1024;
      ledWhiteColdState = 0;
      neutralState = false;
    } else if (command == "11") {
      ledWhiteHotState = 512;
      ledWhiteColdState = 512;
      neutralState = true;
    } else if (command == "01") {
      ledWhiteHotState = 0;
      ledWhiteColdState = 1024;
      neutralState = false;
    } else if (command == "00") {
      ledWhiteHotState = 0;
      ledWhiteColdState = 0;
      neutralState = false;
    } else if (command == "avancado") {
      advancedMode = true;
    } else if (command == "desligarAvancado") {
      advancedMode = false;
    } else if (command.toInt() >= 0 && command.toInt() <= 1024 && advancedMode) {
      int val = command.toInt();
      ledWhiteHotState = 1024 - val;
      ledWhiteColdState = val;
      neutralState = false;   
    }

    ledcWrite(LEDC_CHANNEL_0, ledWhiteHotState);   // Aplica o estado ao LED Branco Quente
    ledcWrite(LEDC_CHANNEL_1, ledWhiteColdState); // Aplica o estado ao LED Branco Frio
    // Salva os atuais estados dos LEDs na NVSRAM
    preferences.putInt("hot", ledWhiteHotState);
    preferences.putInt("cold", ledWhiteColdState);
    appMoved = true; // Seta a flag quando o aplicativo se move
  }

  // Trata o controle do potenciômetro
  int potValue = readPot(); // Usa a nova função para ler o potenciômetro
  if (abs(potValue - lastPotValue) > 200) { // Se o potenciômetro se moveu significativamente
    lastPotValue = potValue; // Atualiza o último valor do potenciômetro
    if (!appConnected) {
      potMovedAfterDisconnect = true; // Seta a flag
      neutralState = false; // Se o potenciômetro se moveu, não estamos mais no estado neutro
      if (appMoved) {
        appMoved = false; // Reinicia a flag quando o potenciômetro se move depois que o aplicativo desconecta
      }
    }
  }
  if (!appConnected && potMovedAfterDisconnect && !neutralState) {
    potValue = map(potValue, 0, 4095, 0, 1024); // Mapeia o valor do potenciômetro para a faixa de PWM do LED
    ledWhiteHotState = 1024 - potValue;
    ledWhiteColdState = potValue;
    ledcWrite(LEDC_CHANNEL_0, ledWhiteHotState);   // Aplica o estado ao LED Branco Quente
    ledcWrite(LEDC_CHANNEL_1, ledWhiteColdState); // Aplica o estado ao LED Branco Frio
    // Salva os atuais estados dos LEDs na NVSRAM
    preferences.putInt("hot", ledWhiteHotState);
    preferences.putInt("cold", ledWhiteColdState);
  }

  // Trata a leitura da temperatura do DHT11
  if (millis() - lastTempCheckTime >= 5000) { // Verifica se passaram 5 segundos
    int temp = dht.readTemperature();
    if (isnan(temp)) {
      Serial.println("Failed to read from DHT sensor!");
    } else {
      char buffer[10];
      snprintf(buffer, sizeof(buffer), "%dº", temp);
      SerialBT.print(buffer);
      Serial.print("Temperatura: ");
      Serial.println(temp);
    }
    lastTempCheckTime = millis(); // Atualiza a última vez que a temperatura foi verificada
  }
}
