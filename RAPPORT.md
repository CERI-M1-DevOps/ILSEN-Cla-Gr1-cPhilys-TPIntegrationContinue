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

### Configuration de l'utilisateur dockeruser sans sudo

Étapes effectuées :
1. Création du groupe `docker` :
```bash
sudo groupadd docker
```
2. Ajout de l'utilisateur `dockeruser` au groupe `docker` :
```bash
sudo usermod -aG docker dockeruser
```
*(L'option `-aG` permet d'ajouter le groupe secondaire sans perdre les appartenances existantes).*

3. Redémarrage de la machine virtuelle pour recharger les groupes de session :
```bash
sudo reboot
```

4. Test de la commande sans passer par `sudo` :
```bash
docker version
```

**Constat :**  
La commande retourne désormais les informations complètes Client et Server sans erreur de permission.  
**Pourquoi :**  
Le socket UNIX `/var/run/docker.sock` appartient au groupe `docker` avec des droits `rw-rw----`. En devenant membre de ce groupe, `dockeruser` peut lire et écrire sur le socket sans devoir élever ses privilèges avec `sudo`.

---

## Partie 3 – Premières manipulations de conteneurs et d’images

### Répertoire de stockage des objets Docker et catégories

Docker stocke l'ensemble de ses données internes dans le répertoire :
```bash
/var/lib/docker/
```

Les différentes catégories d'objets stockés dans cette arborescence sont :
- `containers/` : métadonnées, configurations JSON et logs des conteneurs.
- `image/` : métadonnées des images, descripteurs et base de données des layers (`imagedb`).
- `overlay2/` : couches de stockage effectives du système de fichiers unionfs.
- `volumes/` : données des volumes persistants créés par Docker.
- `network/` : base de données locale des configurations réseau (bridges, endpoints).
- `buildkit/` : cache de compilation pour Docker BuildKit.

### Contenu initial du répertoire des conteneurs

Commande :
```bash
sudo ls -la /var/lib/docker/containers/
```

**Constat :**  
Le répertoire ne contient aucun sous-dossier de conteneur. Il y a actuellement **0 conteneur** stocké sur la machine.

### Recherche d'images sur Docker Hub

Commande :
```bash
docker search hello-world
```

**Explication :**  
La commande consulte le registre public Docker Hub et retourne la liste des dépôts correspondants. L'image officielle `hello-world` est classée en tête de liste : elle totalise le plus grand nombre d'étoiles (`STARS`) et dispose de la mention `[OK]` sous la colonne `OFFICIAL`.

### Exécution de l'image hello-world

Commande :
```bash
docker run hello-world
```

**Étapes constatées lors de l'exécution :**
1. `Unable to find image 'hello-world:latest' locally` : Docker vérifie d'abord si l'image est déjà disponible dans son cache local.
2. `latest: Pulling from library/hello-world` : Comme l'image est introuvable localement, Docker contacte Docker Hub pour la télécharger.
3. `Digest: sha256:...` et `Status: Downloaded newer image for hello-world:latest` : Les couches de l'image sont téléchargées, vérifiées par leur empreinte SHA256 et enregistrées.
4. Docker crée un conteneur à partir de cette image et démarre son binaire principal.
5. Le programme s'exécute et affiche dans le terminal le message de bienvenue "Hello from Docker!".
6. Le binaire termine son exécution (`exit 0`), provoquant l'arrêt immédiat du conteneur.

### Inspection du stockage et SHA256 de l'image

1. **Vérification du répertoire des conteneurs :**
```bash
sudo ls -l /var/lib/docker/containers/
```
**Constat :** Il y a désormais **1 conteneur** (un sous-dossier portant l'identifiant long de 64 caractères du conteneur). Même si le conteneur a fini son exécution, Docker le conserve sur disque avec ses métadonnées et ses logs.

2. **Vérification du répertoire des images :**
```bash
sudo ls -l /var/lib/docker/image/overlay2/imagedb/content/sha256/
```
**Constat :** Un fichier JSON est présent, correspondant à l'image `hello-world` enregistrée.

3. **SHA256 de l'image :**
```bash
docker inspect hello-world --format '{{.Id}}'
```
L'identifiant complet SHA256 de l'image est :  
`sha256:d2c94e258dcb3c5ac4040d3016186b7f36e6438547510f81da040dd2112f5e16`

### Liste des conteneurs en cours d'exécution

Commande :
```bash
docker ps
```

- **Nombre de conteneurs en exécution :** **0 conteneur**.
- **Pourquoi :** Le binaire de `hello-world` affiche son message puis se termine immédiatement. Un conteneur Docker ne reste actif que tant que son processus principal (PID 1) tourne.
- **Alias utilisé :** `docker ps` est l'alias historique de la commande `docker container ls`. Par défaut, quand aucun objet n'est spécifié, Docker applique les commandes aux conteneurs.

### Liste des images locales

Commande :
```bash
docker images
```

**Explication :**  
La commande affiche les images présentes sur l'hôte. Pour l'image `hello-world` avec le tag `latest`, l'identifiant court (`IMAGE ID`) est composé des 12 premiers caractères du SHA256 (par exemple `d2c94e258dcb`).

### Seconde exécution du conteneur

Commande :
```bash
docker run hello-world
```

**Comparaison et explication :**  
- Contrairement au premier lancement, la ligne "Unable to find image... locally" n'apparaît plus. Le démarrage est instantané car l'image est déjà disponible dans le cache local.
- Toutefois, `docker run` instancie un **nouveau conteneur distinct** du précédent (avec un nouvel ID et un nouveau nom généré).

### Nombre de conteneurs actifs vs stockés

- **Conteneurs en cours d'exécution :** **0** (vérifiable avec `docker ps`).
- **Conteneurs stockés localement :** **2** (vérifiable avec `docker ps -a` ou en comptant les dossiers dans `/var/lib/docker/containers/`).  
Chaque commande `docker run` a créé une instance distincte qui est restée stockée à l'état arrêté.

### Tentative de suppression de l'image

Commande :
```bash
docker rmi hello-world
```

**Constat :**  
Docker refuse de supprimer l'image et lève une erreur de conflit :
`Error response from daemon: conflict: unable to remove repository reference "hello-world" (must force) - container <ID> is using its referenced image <ID>`

**Pourquoi :**  
Docker garantit l'intégrité de ses conteneurs : même arrêté, un conteneur dépend de la couche en lecture seule de l'image qui a servi à le créer. Sans l'option de force (`-f`), il est impératif de supprimer d'abord les conteneurs dépendants avant de pouvoir supprimer l'image.

### Liste complète des conteneurs et statut

Commande :
```bash
docker ps -a
```

**Observations :**  
- **Statut :** La colonne `STATUS` indique `Exited (0) X minutes ago`. Le code de retour `(0)` signale une terminaison avec succès.
- **Noms des conteneurs :** En l'absence de nom explicite lors du `run`, Docker attribue un nom aléatoire unique composé d'un adjectif et du nom d'un scientifique ou pionnier de l'informatique (par exemple `brave_curie`, `nifty_hopper`). Les deux conteneurs ont des noms différents.

### Affichage non tronqué

Commande :
```bash
docker ps -a --no-trunc
```

**Explication :**  
L'option `--no-trunc` force Docker à afficher les valeurs complètes sans les abréger :
- Le `CONTAINER ID` et l'`IMAGE` sont affichés avec leurs 64 caractères SHA256 intégraux.
- La commande exécutée dans le conteneur est également affichée sans troncature.

### Suppression ciblée d'un conteneur par son nom

1. Exécution d'un troisième conteneur :
```bash
docker run hello-world
```
2. Identification du nom généré avec `docker ps -a` (par exemple `kind_archimedes`).
3. Suppression du conteneur en spécifiant ce nom :
```bash
docker rm kind_archimedes
```

**Constat :**  
La commande affiche le nom du conteneur supprimé pour confirmation. Le conteneur n'apparaît plus dans `docker ps -a`.

### Lancement par SHA256 avec nommage explicite

Commande :
```bash
docker run --name test_sha d2c94e258dcb
```

**Constat :**  
Le conteneur démarre et s'exécute de façon identique. Dans `docker ps -a`, le conteneur porte bien le nom défini `test_sha` et la colonne `IMAGE` référence l'identifiant court spécifié.  
L'option `--name` permet d'attribuer un nom déterministe au lieu d'un nom aléatoire, ce qui facilite les scripts et la maintenance.
