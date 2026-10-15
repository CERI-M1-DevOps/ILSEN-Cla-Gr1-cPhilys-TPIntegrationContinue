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

### Informations globales sur le système

Commande :
```bash
docker info
```

**Constat :**  
La sortie synthétise l'état global du daemon :
- `Containers: 3` (`Running: 0`, `Paused: 0`, `Stopped: 3`).
- `Images: 1`.
- Des détails d'infrastructure sont également listés : le pilote de stockage (`Storage Driver: overlay2`), le chemin racine `/var/lib/docker`, l'architecture et la version du noyau de l'hôte.

### Suppression globale des conteneurs en bash

1. **Extraction exclusive des identifiants :**
```bash
docker ps -a -q
```
L'option `-q` (`--quiet`) n'affiche que la colonne des IDs des conteneurs.

2. **Suppression de tous les conteneurs en une seule ligne :**
```bash
docker rm $(docker ps -a -q)
```

**Explication :**  
La substitution de commande bash `$(...)` injecte la liste de tous les IDs produits par `docker ps -a -q` en arguments de `docker rm`. Tous les conteneurs arrêtés sont ainsi détruits d'un seul coup.

### Suppression de l'image hello-world

Commande :
```bash
docker rmi hello-world
```

**Constat :**  
La suppression s'effectue sans aucune erreur :
```text
Untagged: hello-world:latest
Untagged: hello-world@sha256:d2c94e258dcb3c5ac4040d3016186b7f36e6438547510f81da040dd2112f5e16
Deleted: sha256:d2c94e258dcb3c5ac4040d3016186b7f36e6438547510f81da040dd2112f5e16
```

**Pourquoi :**  
Comme l'ensemble des conteneurs a été purgé à l'étape précédente, aucune dépendance ne bloque plus la libération de l'image. Docker supprime le tag et efface les couches physiques du disque.

### Exécution de hello1 et vérification de l'identifiant

Commande :
```bash
docker run --name hello1 hello-world
```

**Constat :**  
- L'image n'étant plus présente dans le cache local, Docker affiche de nouveau `Unable to find image 'hello-world:latest' locally` et la retélécharge depuis Docker Hub.
- Le nouveau conteneur est créé sous le nom `hello1`.

Vérification de l'identifiant de l'image retéléchargée :
```bash
docker images
```

**Constat :**  
L'identifiant (`IMAGE ID` : `d2c94e258dcb`) est rigoureusement identique au tout premier téléchargement.  
**Pourquoi :**  
Docker repose sur un adressage par le contenu (*content-addressable storage*). L'identifiant est le hash SHA256 de la configuration et des couches. Le contenu distant n'ayant pas changé, le hash calculé localement reste invariant.

### Création sans démarrage (hello2) et démarrage simple

1. **Création d'un conteneur sans le lancer :**
```bash
docker create --name hello2 hello-world
docker ps -a
```
**Constat :** Le conteneur apparaît dans la liste avec le statut `Created`. Docker a réservé son nom, instancié sa couche d'écriture et enregistré sa configuration, mais n'a pas encore démarré son processus.

2. **Démarrage du conteneur :**
```bash
docker start hello2
```
**Affichage :** Seul le nom `hello2` est affiché dans le terminal. Le texte "Hello from Docker!" n'apparaît pas à l'écran.  
**Statut constaté :** Dans `docker ps -a`, le statut est passé à `Exited (0)`.  
**Pourquoi :** La commande `docker start` lance par défaut le conteneur en arrière-plan (mode détaché) sans connecter ses flux stdout/stderr au terminal courant.

### Démarrage avec attachement des flux (start -a)

Commandes :
```bash
docker start -a hello2
docker start -a hello1
```

**Constat :**  
L'option `-a` (`--attach`) connecte la sortie standard du conteneur à notre terminal. Les messages "Hello from Docker!" s'affichent alors directement dans la console lors du redémarrage de `hello2` puis de `hello1`.

### Exécution en arrière-plan (-d), conteneur éphémère (--rm) et nettoyage

1. **Exécution en tâche de fond (détaché) :**
```bash
docker run -d --name hello3 hello-world
```
**Constat :** Docker renvoie uniquement l'identifiant long du conteneur. Dans `docker ps -a`, `hello3` apparaît avec le statut `Exited (0)` car son binaire s'est arrêté aussitôt l'exécution finie.

2. **Exécution avec auto-suppression :**
```bash
docker run --rm --name hello4 hello-world
```
**Constat :** Le message s'affiche sur la console, mais lorsqu'on vérifie avec `docker ps -a`, `hello4` n'apparaît nulle part. L'option `--rm` supprime automatiquement le conteneur et sa couche d'écriture dès qu'il passe à l'état arrêté.

3. **Nettoyage final avant la suite :**
```bash
docker rm hello1 hello2 hello3
docker rmi hello-world
```
Les conteneurs créés et l'image d'exercice `hello-world` sont supprimés proprement.

---

## Partie 4 – DockerHub

### Types d'images et classification sur le registre public

Sur Docker Hub (https://hub.docker.com/), on trouve plusieurs catégories d'images :
- **Docker Official Images :** images de référence maintenues et validées par les équipes Docker et les éditeurs amont (ex. `ubuntu`, `python`, `nginx`). Elles offrent des garanties de sécurité et des Dockerfiles optimisés.
- **Verified Publisher :** images publiées par des éditeurs tiers officiellement reconnus par Docker (ex. Canonical, Red Hat).
- **Docker Sponsored Open Source :** projets open source vérifiés et sponsorisés par Docker.
- **Community Images :** images publiques déposées par n'importe quel utilisateur ou organisation (sous la forme `utilisateur/depot`).

La classification s'effectue via des filtres par catégorie (systèmes d'exploitation, bases de données, outils devops), par architecture matérielle (`amd64`, `arm64`), par niveau de confiance (badges officiel / vérifié) et par tags de version.

### Image officielle Ubuntu et gestion des versions

L'image officielle est accessible sous le nom `ubuntu` (ou `library/ubuntu`).

Les différentes versions proposées sont identifiées par des **tags** :
- Numéros de version : `24.04`, `22.04`, `20.04`.
- Noms de code de distribution : `noble`, `jammy`, `focal`.
- Révisions datées précises pour la reproductibilité : `noble-20240904`.
- Variantes spécifiques : `rolling` (dernière version intermédiaire publiée) ou `devel` (version en cours de développement).

### Version la plus récente vs version latest

- **Version la plus récente :** Ubuntu publie des versions tous les 6 mois (ex. `ubuntu:rolling` qui correspond à la dernière version semestrielle non-LTS avec 9 mois de support).
- **Version `latest` :** Docker fait pointer le tag `latest` vers la dernière version **LTS** (Long Term Support) stabilisée, actuellement Ubuntu 24.04 LTS (`noble`), recommandée en production.
- **Identifiants respectifs :** `ubuntu:latest` et `ubuntu:noble` partagent exactement le même digest SHA256 et le même Image ID, tandis que `ubuntu:rolling` pointe vers une image distincte avec son propre condensat SHA256.

### Analyse et classification des vulnérabilités

Docker Hub intègre l'analyseur de sécurité Docker Scout pour inspecter les images :
- **Constat :** Les versions de type `rolling` ou `devel` intègrent des paquets très récents pouvant comporter des vulnérabilités nouvellement signalées non encore corrigées. À l'inverse, les images basées sur les versions LTS (`latest`/`noble`) bénéficient de correctifs de sécurité réguliers et d'une base logicielle plus stable.
- **Classement des vulnérabilités :** Les vulnérabilités (CVE) sont hiérarchisées selon leur score de gravité CVSS :
  - **Critical (Critique) :** exploitation à distance triviale avec impact majeur.
  - **High (Élevée) :** risque sérieux nécessitant un correctif rapide.
  - **Medium (Moyenne) :** impact modéré ou conditions d'exploitation spécifiques.
  - **Low (Faible) :** impact minime ou théorique.

### Comparaison des tags latest et noble

- **Constat :** Les tags `latest` et `noble` pointent sur le même condensat d'image.
- **Intérêt :** En pratique DevOps, il est déconseillé d'utiliser le tag mutable `latest` dans les Dockerfiles de production. Référencer explicitement `ubuntu:noble` (ou `ubuntu:24.04`) garantit que les déploiements futurs resteront sur la version 24.04, évitant les régressions le jour où `latest` basculera vers la version LTS suivante (26.04).

### Consultation des couches (layers) sur Docker Hub

En consultant le détail des tags sur Docker Hub :
- Pour `latest` et `noble` : l'historique et les hash des couches sont rigoureusement identiques à 100%.
- En comparant `noble` avec `rolling` : les deux images divergent dès les premières couches du rootfs (fichiers de paquets et configuration initiale de la distribution), générant des identifiants de couches entièrement distincts.

### Inspection du répertoire layerdb

Commande :
```bash
sudo ls -l /var/lib/docker/image/overlay2/layerdb/sha256
```

**Explication :**  
Ce répertoire constitue la base de données locale des métadonnées des couches Docker. Chaque sous-répertoire correspond au SHA256 d'une couche et contient des fichiers de suivi :
- `cache-id` : référence vers le système de fichiers réel dans `/var/lib/docker/overlay2/`.
- `diff` : identifiant du delta de la couche.
- `size` : taille de la couche en octets.
- `tar-split` : métadonnées pour reconstruire l'archive tar d'origine.

### Téléchargement de l'image ubuntu par défaut

Commande :
```bash
docker pull ubuntu
```

**Constat :**  
Docker applique automatiquement le tag par défaut `:latest`. La commande télécharge donc `ubuntu:latest` (qui correspond à la version LTS Ubuntu 24.04).

### Téléchargement de la version la plus récente et filtrage

1. **Téléchargement de la version rolling :**
```bash
docker pull ubuntu:rolling
```

2. **Affichage des images locales avec filtre de référence :**
```bash
docker images --filter "reference=ubuntu*"
```

**Constat :**  
La commande ne liste que les images dont le nom de dépôt correspond au motif `ubuntu*` (ici `ubuntu:latest` et `ubuntu:rolling`), masquant les autres images présentes sur la machine.

### Commande par défaut de l'image ubuntu

Commande d'inspection :
```bash
docker inspect ubuntu --format '{{.Config.Cmd}}'
```

**Résultat :** `[bash]` (ou `[/bin/bash]`).  
L'image Ubuntu est configurée pour exécuter le shell bash par défaut lors du démarrage d'un conteneur si aucune commande n'est passée en argument.

---

## Partie 5 – Interagir avec un conteneur

### Préparation et rappel sur Ctrl-D

1. **Suppression de l'image `ubuntu:rolling` :**
```bash
docker rmi ubuntu:rolling
```

2. **Test du raccourci `Ctrl-D` sur le terminal hôte :**
La frappe de `Ctrl-D` transmet le caractère spécial EOF (End Of File) sur l'entrée standard du terminal. Le shell Unix détecte la fin des entrées utilisateur et met fin à la session en se déconnectant.

### Lancement sans option d'un conteneur ubuntu

1. **Lancement du conteneur :**
```bash
docker run ubuntu
docker ps -a
```

**Constat :**  
Le conteneur passe directement à l'état `Exited (0)` sans rien afficher à l'écran.  
**Pourquoi :**  
Comme vu précédemment, la commande par défaut est `bash`. Sans les options interactives (`-i` et `-t`), `bash` ne dispose d'aucun terminal attaché ni d'entrée standard ouverte : il reçoit un signal EOF dès son lancement et s'arrête immédiatement.

2. **Tentative de redémarrage en mode interactif :**
```bash
docker start -i <id_du_conteneur>
```
Le shell s'ouvre. En tapant `exit`, le conteneur s'arrête de nouveau.

3. **Suppression du conteneur :**
```bash
docker rm <id_du_conteneur>
```

### Lancement d'un conteneur interactif nommé os_ubuntu

Commande :
```bash
docker run -it --name os_ubuntu ubuntu
```

**Explication des options :**  
- `-i` (`--interactive`) : maintient le flux d'entrée standard (stdin) du conteneur ouvert même en l'absence de saisie immédiate.
- `-t` (`--tty`) : alloue un pseudo-terminal (TTY), ce qui permet d'obtenir une invite de commande interactive (prompt bash avec coloration et gestion des raccourcis clavier).

### Commandes de base dans le conteneur os_ubuntu

Une fois dans le conteneur :
- **Processus de PID 1 :** `/bin/bash`.
- **Exécution des commandes :**
  - `whoami` : renvoie `root` (utilisateur par défaut du conteneur).
  - `pwd` : renvoie `/` (répertoire racine initial).
  - `ls` : liste l'arborescence racine Linux standard (`bin`, `etc`, `home`, `proc`, `sys`, `usr`...).
  - `hostname` : renvoie un nom d'hôte de 12 caractères hexadécimaux (par ex. `a1b2c3d4e5f6`), qui correspond aux 12 premiers caractères du `CONTAINER ID`.

### Observation du conteneur depuis un second terminal

Depuis un second terminal connecté à la machine virtuelle :
```bash
docker ps
```

**Constat :**  
- **Statut :** `Up X minutes`.
- **Nom :** `os_ubuntu`.
- **Commande :** `"bash"`.
- **Identifiant :** Les 12 premiers caractères de l'ID affiché correspondent exactement au nom d'hôte affiché par la commande `hostname` à l'intérieur du conteneur.

### Déplacement et arrêt classique du conteneur

1. Dans le premier terminal (dans le conteneur) :
```bash
cd /home
exit
```

2. Observation du statut depuis l'hôte :
```bash
docker ps -a
```

**Constat :**  
Le conteneur est désormais à l'état `Exited (0)`.  
**Pourquoi :**  
Le processus PID 1 était le shell `bash`. Lorsqu'on le quitte avec `exit`, le processus racine se termine, ce qui provoque immédiatement l'arrêt complet du conteneur.

### Redémarrage en interactif et répertoire de travail

Commande :
```bash
docker start -i os_ubuntu
pwd
```

**Constat :**  
Le répertoire courant est `/` (la racine) et non `/home`.  
**Pourquoi :**  
Chaque démarrage du conteneur instancie un nouveau processus `bash` à partir de la configuration de l'image, qui définit le répertoire de travail initial (`WORKDIR /`). L'état volatile du shell précédent (`cd /home`) n'est pas sauvegardé dans le processus.

### Inspection du conteneur et extraction du PID

Dans le second terminal :
1. **Inspection complète :**
```bash
docker inspect os_ubuntu
```
La section `State` indique `"Running": true`.

2. **Extraction directe du PID avec formatage Go template :**
```bash
docker inspect os_ubuntu --format '{{.State.Pid}}'
```

3. **Extraction avec jq :**
```bash
docker inspect os_ubuntu | jq '.[0].State.Pid'
```

La commande renvoie le numéro de PID du processus sur la machine hôte (par exemple `45123`).

### Identification du processus sur la machine hôte

Commande sur l'hôte avec le PID obtenu :
```bash
ps u -p 45123
```

**Constat :**  
On retrouve le processus `/bin/bash` exécuté sur la machine hôte.  
**Pourquoi :**  
Un conteneur n'est pas une machine virtuelle : il ne virtualise aucun matériel et ne possède pas son propre noyau. Il s'agit d'un simple processus Linux standard de l'hôte, isolé dans ses propres namespaces (PID, réseau, montage) et restreint par les cgroups. Vu depuis l'intérieur du conteneur, ce bash porte le PID 1, mais vu depuis la machine hôte, il porte un PID ordinaire.

### Sortie du conteneur sans arrêt (Ctrl-p Ctrl-q)

1. Dans le conteneur (terminal 1) :
```bash
cd /home
```
Puis saisie de la séquence d'échappement : `Ctrl-p` suivi de `Ctrl-q`.

2. Vérification du statut depuis le terminal hôte :
```bash
docker ps
```

**Constat :**  
Le statut reste `Up`. Le conteneur continue de tourner.  
**Pourquoi :**  
La séquence `Ctrl-p Ctrl-q` détache le terminal du conteneur sans envoyer de signal d'arrêt (SIGHUP ou SIGTERM) et sans clore le flux d'entrée. Le processus `bash` (PID 1) reste donc actif en tâche de fond.

### Réattachement et modification du conteneur

1. **Rattacher le terminal au conteneur :**
```bash
docker attach os_ubuntu
pwd
```

**Constat :**  
Le répertoire courant est toujours `/home`. Comme le processus ne s'est jamais arrêté, nous retrouvons exactement la même session shell intacte.

2. **Création d'un fichier de test :**
```bash
echo "TP DevOps Docker" > monfichier.txt
ls -l
```

### Détection des modifications avec docker diff

Après détachement (`Ctrl-p Ctrl-q`), vérification depuis l'hôte :
```bash
docker diff os_ubuntu
```

**Constat :**  
Docker liste les deltas appliqués sur la couche d'écriture (Copy-on-Write) :
- `C /home` : le répertoire a été modifié (*Changed*).
- `A /home/monfichier.txt` : le fichier a été ajouté (*Added*).
- `C /root` et `A /root/.bash_history` : trace de l'historique bash.

### Exécution de commandes avec docker exec

1. **Exécution d'une commande sans passer en avant-plan :**
```bash
docker exec os_ubuntu hostname
```
La commande retourne le nom d'hôte du conteneur directement dans le terminal de l'hôte, sans interférer avec la session principale.

2. **Exécution d'un second shell interactif :**
```bash
docker exec -it os_ubuntu bash
```

3. **Vérification du nombre de processus bash :**
```bash
ps -ef | grep bash
```
**Constat :** Il y a **2 processus bash** qui tournent simultanément dans le conteneur : le premier PID 1 initial, et le nouveau lancé par `docker exec`.

### Affichage des processus du conteneur avec docker top

Depuis l'hôte :
```bash
docker top os_ubuntu
```

**Constat :**  
La commande affiche l'ensemble des processus s'exécutant dans le conteneur, en indiquant leurs PIDs vus de la machine hôte, leurs utilisateurs et leurs commandes respectives.

Sortie et arrêt du conteneur :
On retourne dans le premier terminal et on quitte le shell principal avec `exit`.

### Exécution de commandes personnalisées

1. **Conteneur `ll` avec la commande `ls -l` :**
```bash
docker run --name ll ubuntu ls -l
```
Le conteneur liste la racine et s'arrête. Pour réobtenir le même affichage ultérieurement :
```bash
docker start -a ll
```

2. **Conteneur `salut` avec `echo Bonjour` :**
```bash
docker run --name salut ubuntu echo Bonjour
```
Affiche "Bonjour" puis s'arrête. Pour le relancer :
```bash
docker start -a salut
```

### Conteneur éphémère et commande infinie

1. **Conteneur éphémère avec `ps aux` :**
```bash
docker run --rm --name ps ubuntu ps aux
```
**Constat :**
- Le binaire `ps aux` a le PID 1 dans le conteneur.
- Dès l'affichage terminé, le conteneur est automatiquement détruit (`--rm`) et n'apparaît plus dans `docker ps -a`.

2. **Conteneur avec boucle infinie :**
```bash
docker run -d --name conteneur_infini ubuntu sh -c "while true ; sleep 3600 ; done"
```
Pour le supprimer, comme il tourne en continu, `docker rm conteneur_infini` échoue. On force la suppression avec :
```bash
docker rm -f conteneur_infini
```
L'option `-f` envoie un signal SIGKILL au processus avant d'effacer le conteneur.

### Partage des flux d'E/S et code de sortie personnalisé

1. **Boucle d'affichage dans le premier terminal :**
```bash
docker start -i os_ubuntu
while true ; do echo "salut" ; sleep 3 ; done
```

2. **Attachement depuis le second terminal :**
```bash
docker attach os_ubuntu
```
**Constat :**  
Le second terminal s'attache exactement au même flux de sortie : les "salut" défilent sur les deux terminaux simultanément.  
Si l'on envoie un `Ctrl-C` dans l'un des terminaux, le signal interrompt la boucle dans les deux fenêtres.

3. **Sortie avec un code retour personnalisé :**
```bash
exit 42
```
Vérification du statut :
```bash
docker ps -a
```
**Constat :**  
La colonne `STATUS` indique `Exited (42)`. Docker stocke et préserve fidèlement le code d'erreur retourné par le processus.

### Consultation des logs du conteneur

1. **Via la commande Docker :**
```bash
docker logs os_ubuntu
```
Affiche tout l'historique des sorties stdout et stderr du conteneur.

2. **Localisation du fichier journal sur l'hôte :**
```bash
docker inspect os_ubuntu --format '{{.LogPath}}'
```
Le fichier se trouve sous :  
`/var/lib/docker/containers/<id_complet>/<id_complet>-json.log`

3. **Consultation du fichier avec jq :**
```bash
sudo cat $(docker inspect os_ubuntu --format '{{.LogPath}}') | jq .
```
**Constat :**  
Chaque ligne est un objet JSON structuré comprenant :
- `"log"` : le contenu du message émis.
- `"stream"` : le canal de sortie (`stdout` ou `stderr`).
- `"time"` : l'horodatage UTC précis de l'émission du log.

### Nettoyage des conteneurs

1. **Suppression de tous les conteneurs arrêtés :**
```bash
docker container prune -f
```

2. **Arrêter et supprimer tous les conteneurs actifs en une seule commande :**
```bash
docker rm -f $(docker ps -aq)
```

---

## Partie 6 – Inspection et manipulation d’images

### Téléchargement de python:3.9 et historique des couches

1. **Téléchargement de l'image :**
```bash
docker pull python:3.9
```
Le terminal affiche le téléchargement de 8 à 10 couches successives ("Pull complete").

2. **Observation des couches de l'image :**
```bash
docker history python:3.9
```
*(On peut utiliser `docker history --no-trunc python:3.9` pour voir l'intégralité des commandes de build sans troncature).*

3. **Dernière couche de l'image :**  
La première ligne retournée par `docker history` (le sommet de la pile de couches) correspond à la directive finale :
`CMD ["python3"]`

### Téléchargement de python:3.14.0a1 et réutilisation du cache

Commande :
```bash
docker pull python:3.14.0a1
```

**Constat :**  
Pour plusieurs couches, Docker n'effectue aucun téléchargement et indique `Already exists`. Seules les couches supérieures sont téléchargées.  
**Pourquoi :**  
Les deux images `python:3.9` et `python:3.14.0a1` sont basées sur le même système d'exploitation sous-jacent (Debian Bookworm). Docker mutualise les couches communes déjà présentes dans son stockage local, ce qui réduit le temps de transfert et l'espace disque consommé.

### Comparaison des couches avec jq

Commandes :
```bash
docker inspect python:3.9 | jq '.[0].RootFS.Layers'
docker inspect python:3.14.0a1 | jq '.[0].RootFS.Layers'
```

**Constat :**  
Les premiers hash SHA256 des listes `RootFS.Layers` sont strictement identiques entre les deux images. Cela confirme techniquement le partage effectif des couches de base Debian sur le système de fichiers hôte.
