# Trixie Slim Selfref
A Debian Trixie slim image that can build itself.

# Intro
A self referencing image? Why is this important, you say?

For me much of the FLOSS movement is not so much a movement but more focused on consuming what is FREE... Linux distros,
Docker images, games, other operating systems... For me, proving that the product I am using is also
OPEN is important.

For me the hold point is to be able to patch bugs, look under the hood to tinker about or simply learn
a little bit from looking at code I use every day.

Many FLOSS repos I check today simply do not care about the OPEN part and traceability is tricky or impossible or 
the code in the repos does not work...

This little repo aims to fix a little bit of this chafing.

# Since you are all antsy to get started...
Build by running...
```bash
./trixie-slim-selfref.sh
```

# The setup
* A cronjob that pulls all different architecture branches from GitHub and checks for new [checksums](https://raw.githubusercontent.com/debuerreotype/docker-debian-artifacts/39095b9bf8cbb2635be1e2dfed3d152f0b3d72bf/trixie/slim/rootfs.tar.xz.sha256) 
    * If no changes has been made the cronjob exits
* If a new checksum is discovered, the copy of [this](https://github.com/jdw/trixie-slim-selfref) repo is also checked for new releases 
* The Docker image is built 
    * This repository's latest release is copied to the new image

# Project
## Dependencies
* [Kotlin 2.0](https://kotlinlang.org/docs/whatsnew20.html)
* [Clikt](https://ajalt.github.io/clikt/)
* [Fuel](https://github.com/kittinunf/fuel)
* [KtSh](https://github.com/jaredrummler/KtSh)

## Branches
* [main](https://github.com/jdw/trixie-slim-selfref): Live code, what is copied to the image, should be worthy of production 
* [test](https://github.com/jdw/trixie-slim-selfref/tree/test): For testing purposes
* [dev](https://github.com/jdw/trixie-slim-selfref/tree/dev): For development
* [checksums](https://github.com/jdw/trixie-slim-selfref/tree/checksums): Used to store the checksums found in Debian docker images repo

# Grab bag
## Misc data
View this file by running...
tjo
```bash
./trixie-slim-selfref.sh general --peruse-readme
```

## Misc links
* [this repo](https://github.com/jdw/trixie-slim-selfref)
* [My personal homepage Paronomasia](https://parono.asia)

### Upstream
* [Debian](https://www.debian.org/)
    * [Debian Trixie](https://wiki.debian.org/DebianTrixie)
    * [Debian Trixie release notes](https://www.debian.org/releases/trixie/release-notes/index.en.html)
    * [Debian Wiki Kotlin page](https://wiki.debian.org/Kotlin)
    * [Debian docker images repo](https://github.com/debuerreotype/docker-debian-artifacts)
* [Docker Hub](https://hub.docker.com/layers/library/debian/trixie-slim/images/sha256-5ab342ea0ec9552312f13ec81945dcf51afe9a920db367e2b2c6efb75ce5b6c8?context=explore)