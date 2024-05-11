#include <SoftwareSerial.h>

SoftwareSerial BTSerial(2, 3);
byte r = 9, g = 10, b = 11;
int shouldBlink = 0;
byte rgb[3] = { 0 };
String input;

void setup() {
  pinMode(r, OUTPUT);  // red pin
  pinMode(g, OUTPUT);  // green pin
  pinMode(b, OUTPUT);  // blue pin
  Serial.begin(9600);
  BTSerial.begin(9600);
}

void loop() {

  if (BTSerial.available()) {

    input = BTSerial.readString();

    if(input.startsWith("b")) {
      if(shouldBlink) {
        shouldBlink = 0;
      } else {
        shouldBlink = 1;
      }
    } else {
      int previousIndex = 0;
      int nextIndex = 0;

      for (int i = 0; i < 3; i++) {
        nextIndex = input.indexOf(",", previousIndex);
        rgb[i] = input.substring(previousIndex, nextIndex).toInt();
        previousIndex = nextIndex + 1;
      }

      Serial.println("\n*** NEW INPUT ***\n");+

      Serial.print("Unformatted input = ");
      Serial.println(input);

      Serial.print("Red = ");
      Serial.println(rgb[0]);

      Serial.print("Green = ");
      Serial.println(rgb[1]);

      Serial.print("Blue = ");
      Serial.println(rgb[2]);

      Serial.println("\n*****************\n");

      analogWrite(r, rgb[0]);
      analogWrite(g, rgb[1]);
      analogWrite(b, rgb[2]);
    }

  }
  if(shouldBlink) {
    blink();
  }
}

void blink() {
    analogWrite(r, 0);
    analogWrite(g, 0);
    analogWrite(b, 0);
    delay(100);
    analogWrite(r, rgb[0]);
    analogWrite(g, rgb[1]);
    analogWrite(b, rgb[2]);
    delay(100);
}
