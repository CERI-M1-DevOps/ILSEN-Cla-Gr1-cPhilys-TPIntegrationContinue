# TP 2 : Docker - Conteneurs et images

**Auteur :** Corentin Philys  
**Formation :** Master 1 ILSEN (Groupe 1)  
**Date :** 14 octobre 2026  

---

## Partie 2 – Installation et démarrage de Docker

### Installation à partir du dépôt officiel apt

Conformément à la documentation officielle (étapes 1 et 2) :
1. Configuration du trousseau de clés GPG officiel de Docker :
```bash
sudo apt-get update
sudo apt-get install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

2. Ajout du dépôt Docker dans les sources APT :
```bash
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
```

3. Installation des paquets du moteur Docker :
```bash
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```
