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

### Affichage de la version (client et serveur)

Commande exécutée :
```bash
sudo docker version
```

**Explication :**  
La commande simple `docker --version` ne retourne qu'une seule ligne d'information sur la CLI locale.  
En revanche, `docker version` interroge le socket UNIX et fournit deux blocs distincts :
- **Client :** version de la CLI, version de Go utilisée pour la compilation, commit Git et architecture (`linux/amd64`).
- **Server (Engine) :** version du moteur Docker, version minimale de l'API supportée, containerd, runc et docker-init.
Cela permet de vérifier à la fois le client et le bon fonctionnement du daemon.

### Composants du daemon Docker en cours d'exécution

Commande utilisée :
```bash
ps aux | grep -E 'dockerd|containerd'
```

**Explication :**  
Docker s'appuie sur une architecture découplée :
- `dockerd` : le daemon Docker principal. Il expose l'API Docker, gère les images, les volumes, le réseau et orchestre les requêtes de haut niveau.
- `containerd` : le runtime de conteneurs de niveau inférieur. Il supervise le cycle de vie complet des conteneurs sur l'hôte (création, démarrage, arrêt, supervision des processus) en déléguant l'isolation bas niveau à `runc`.

### Services et sockets utilisés par Docker

Commandes utilisées :
```bash
systemctl list-units --type=service | grep docker
systemctl list-units --type=socket | grep docker
ls -l /var/run/docker.sock
```

**Explication :**  
- **Service :** `docker.service` (ainsi que `containerd.service`).
- **Socket :** `docker.socket`, qui gère l'écoute sur le socket UNIX local `/var/run/docker.sock`.
- **Utilisateur :** Ces services sont démarrés par l'utilisateur `root` (UID 0). Ces privilèges administrateur sont indispensables pour configurer les namespaces Linux, les groupes de contrôle (cgroups) et les règles iptables/nftables du noyau.

### Arrêt de Docker et statut du socket

Commande pour arrêter le service :
```bash
sudo systemctl stop docker
systemctl status docker.socket
```

**Constat :**  
Le service `docker.service` est bien arrêté (`inactive/dead`), mais le socket `docker.socket` reste à l'état `active (listening)`.

**Pourquoi :**  
Systemd implémente le principe d'activation par socket (*socket activation*). Même quand le service Docker est stoppé, systemd conserve l'écoute sur `/var/run/docker.sock`. Si un utilisateur ou un script envoie une requête Docker sur ce socket, systemd réveille et relance automatiquement `docker.service`. Pour arrêter complètement Docker, il faut stopper le socket en premier : `sudo systemctl stop docker.socket`.

### Désactiver et réactiver Docker au démarrage

- **Pour désactiver le démarrage automatique au boot :**
```bash
sudo systemctl disable docker
sudo systemctl disable docker.socket
```

- **Pour réactiver le démarrage automatique au boot :**
```bash
sudo systemctl enable docker
sudo systemctl enable docker.socket
```

- **Pour démarrer ou redémarrer le service manuellement :**
```bash
sudo systemctl start docker
```
