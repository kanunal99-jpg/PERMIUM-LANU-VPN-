# Lanu VPN Server Setup Guide

This guide provides instructions on how to set up a real WireGuard VPN server to work with the Lanu VPN Android application.

## Prerequisites
- A Linux VPS (Ubuntu 22.04+ recommended) with a public IP address.
- Root or sudo access.

## 1. Install WireGuard
On your server, run:
```bash
sudo apt update
sudo apt install wireguard -y
```

## 2. Enable IP Forwarding
Edit `/etc/sysctl.conf` and uncomment or add:
```properties
net.ipv4.ip_forward=1
net.ipv6.conf.all.forwarding=1
```
Apply the changes:
```bash
sudo sysctl -p
```

## 3. Generate Keys
Generate server and client keys:
```bash
# Server keys
wg genkey | tee server_private.key | wg pubkey > server_public.key

# Client keys
wg genkey | tee client_private.key | wg pubkey > client_public.key
```

## 4. Configure WireGuard (wg0.conf)
Create `/etc/wireguard/wg0.conf`:
```ini
[Interface]
Address = 10.8.0.1/24
ListenPort = 51820
PrivateKey = <PASTE_SERVER_PRIVATE_KEY>

# NAT Rules (adjust 'eth0' to your public interface)
PostUp = iptables -A FORWARD -i %i -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i %i -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

# Client Peer
[Peer]
PublicKey = <PASTE_CLIENT_PUBLIC_KEY>
AllowedIPs = 10.8.0.2/32
```

## 5. Start WireGuard
```bash
sudo wg-quick up wg0
# Enable on boot
sudo systemctl enable wg-quick@wg0
```

## 6. Configure the Android App
Use the following values in the app's server configuration:
- **Endpoint**: Your Server Public IP
- **Port**: 51820
- **Server Public Key**: Contents of `server_public.key`
- **Client Private Key**: Contents of `client_private.key`
- **Client Address**: 10.8.0.2/32

## 7. Firewall (UFW)
Ensure UDP port 51820 is open:
```bash
sudo ufw allow 51820/udp
```
