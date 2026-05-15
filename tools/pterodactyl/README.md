# MythicPvP Pterodactyl

This folder contains Pterodactyl artifacts for running MythicPvP test shards.

## Folia Image

Start a local Docker registry on the VPS:

```sh
docker run -d --restart=unless-stopped --name local-registry -p 127.0.0.1:5000:5000 registry:2
```

Build and publish the custom Folia image from the repository root:

```sh
docker build -f tools/docker/Dockerfile.pterodactyl-folia -t 127.0.0.1:5000/mythicpvp/pterodactyl-folia:latest .
docker push 127.0.0.1:5000/mythicpvp/pterodactyl-folia:latest
```

The image includes:

- Folia 1.21.1
- MythicPvP suite jars
- Mythic Core
- Simple Voice Chat and SimpleVoice-Geyser
- Default Folia/Paper/Bukkit/voice configs

The image is Pterodactyl-friendly: it does not force an entrypoint. The egg startup command runs `/opt/folia/entrypoint.sh`, which seeds `/home/container` and starts Folia.

## Egg

Import `egg-mythicpvp-folia.json` into a Minecraft nest, then create a server using the `MythicPvP Folia Java 21` image. The egg points at the local registry image `127.0.0.1:5000/mythicpvp/pterodactyl-folia:latest` so Wings can pull it without a public image registry.

Suggested first test server:

- Name: `MythicPvP Hub Test`
- Allocation: `51.222.136.6:25567`
- Memory: `4096 MiB`
- Disk: `20000 MiB`
- `SERVER_TYPE`: `hub`
- `SHARD_ID`: `hub-ptero-1`
- `STDB_URI`: `http://host.docker.internal:3000` if SpacetimeDB is running on the host, or the reachable STDB URL for your test stack
- `STDB_MODULE`: `mythicpvp`
- `ONLINE_MODE`: `false`

If SpacetimeDB runs in another Docker container, make sure the Pterodactyl server container can reach it. For early plugin boot testing, start with the host-published STDB port and tighten networking later.
