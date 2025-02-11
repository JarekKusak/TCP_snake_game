# Uživatelská dokumentace - TCP Snake Game

## 1. Obecný popis hry
TCP Snake Game je multiplayerová variace klasické hry **Had**, kde hráči ovládají své hady na mřížkovém poli a soupeří mezi sebou v reálném čase. Hra běží přes TCP/IP protokol, kde jeden hráč spustí server a ostatní se k němu připojí jako klienti. Hra podporuje až čtyři hráče současně.

Cílem hry je **přežít co nejdéle**, sbírat **body** za jablka a využívat **speciální power-upy**. Hráči se musí vyhýbat nárazům do překážek, vlastního těla i soupeřů. Poslední přeživší hráč získává bonusové body.

## 2. Pravidla hry
- Hráč ovládá hada a musí se pohybovat po herním poli.
- Při srážce s vlastním tělem, tělem soupeře nebo s hranicí mapy hráč **umírá** a vypadává z kola.
- Pokud se dva hráči **srazí hlavami**, oba **zemřou** a obdrží **-20 bodů**.
- Pokud hráč **sebere jablko**, získá **+10 bodů**.
- Pokud hráč **sebere zlaté jablko**, získá **+20 bodů**.
- Pokud hráč **sebere power-up**, oslepí soupeře na **3 sekundy** – soupeři uvidí pouze **3×3 pole** kolem své hlavy.
- Poslední přeživší hráč v kole získá **+10 bodů**.
- Celkové skóre se sčítá napříč všemi koly.
- Na konci hry se vyhlásí **vítěz** s nejvyšším skóre.

## 3. Ovládání
Hráč se pohybuje pomocí kláves:

- **W** - pohyb nahoru
- **A** - pohyb doleva
- **S** - pohyb dolů
- **D** - pohyb doprava
- **ESC** - ukončení hry

## 4. Spuštění hry

Nejprve je nutné sestavit projekt pomocí **Mavenu**:
```bash
mvn clean package
```

### 4.1 Spuštění serveru
Pro spuštění serveru použijte následující příkaz:
```bash
java -cp target/tcp-snake-game-1.0-SNAPSHOT.jar \
    tcpsnake.Server <pocet_hracu> <port> <pocet_kol>
```
Příklad spuštění serveru pro 2 hráče na portu 12345 s 5 koly:
```bash
java -cp target/tcp-snake-game-1.0-SNAPSHOT.jar \
    tcpsnake.Server 2 12345 5
```
### 4.2 Spuštění klientů
Každý hráč se připojuje ke stejnému serveru. Pro spuštění klienta použijte:
```bash
mvn compile exec:java -Dexec.mainClass=tcpsnake.Client \
    -Dexec.args="<jmeno_hrace> <IP_adresa_serveru> <port>"
```
Příklad spuštění dvou hráčů připojených k serveru na localhost:
```bash
mvn compile exec:java -Dexec.mainClass=tcpsnake.Client \
    -Dexec.args="player1 localhost 12345"

mvn compile exec:java -Dexec.mainClass=tcpsnake.Client \
    -Dexec.args="player2 localhost 12345"
```
## 5. Herní prvky
- Jablko (O) - Přidává hráči 10 bodů.
- Zlaté jablko (*) - Přidává hráči 20 bodů.
- Power-up (&) - Na 3 sekundy oslepí všechny ostatní hráče.
- Hlava hráče (X, Y, Z, W) - Značí aktuální pozici hráčovy hlavy.
- Tělo hráče (x, y, z, w) - Značí části těla jednotlivých hráčů.
- Hranice mapy (#) - Okraje herního pole, do kterých nelze narazit.

## 6. Ukázka hry
```
╔═══════════════════════╗
║ . . . . x . . . . . . ║
║ . . . . x . O . . . . ║
║ . . . . x . . . . & . ║
║ . Y . . . . . . . . . ║
║ . y . . . . . . . . . ║
║ y y . . . . . . . y y ║
║ . . . . x x X . . . . ║
║ . . . . x . . . . . . ║
╚═══════════════════════╝
```

## 7. Známé nedostatky
- Chybí podpora pro ukládání skóre - Po ukončení hry se skóre nezaznamenává nikam mimo aktuální běh programu.
- Absence vizuálních efektů - Hra se zobrazuje v textovém režimu, což omezuje možnosti grafického zobrazení.
- Lokalizace pouze v češtině - V současnosti není podpora pro více jazyků.
## 8. Závěr
   Tento projekt představuje multiplayerovou hru Had, která běží na síťové architektuře TCP. Hra byla vyvinuta v Javě s využitím Maven pro správu závislostí a JUnit pro testování. Kód je plně dokumentován
