# Khawchin Domain Admin Panel Setup

This sets up a small password-protected admin panel at:

```text
https://khawchin.me/admin/
```

Use it to:

- publish or disable in-app announcement banners
- publish app/server status messages
- send one-off FCM topic push messages
- check whether forecast/current JSON files exist
- check WRF 9km/3km age and archive counts
- inspect cron log summaries and tail logs without SSH
- toggle maintenance mode quickly
- clean old WRF archive JSON files with confirmation

## 1. Upload Files

Upload these local files to EC2 `/opt/khawchin/`:

```text
C:\Users\Mapuia\Desktop\App developement\Khawchin app\backend\backend_v86.py
C:\Users\Mapuia\Desktop\App developement\Khawchin app\backend\khawchin_admin_panel.py
```

`backend_v86.py` includes the status merge fix, so admin-set maintenance/status messages are not overwritten by the next forecast run.

## 2. Create Folders

Run on EC2:

```bash
sudo mkdir -p /opt/khawchin/cache/app /opt/khawchin/cache/forecast
sudo chown -R ubuntu:ubuntu /opt/khawchin/cache/app /opt/khawchin/cache/forecast
```

## 3. Create Admin Password File

Choose a strong password, then run on EC2:

```bash
cat > /opt/khawchin/admin.env <<'EOF'
ADMIN_PANEL_ENABLE=1
ADMIN_USER=mapuia
ADMIN_PASSWORD=CHANGE_THIS_TO_A_STRONG_PASSWORD
ADMIN_HOST=127.0.0.1
ADMIN_PORT=8091
APP_CONTROL_DIR=/opt/khawchin/cache/app
FORECAST_CACHE_DIR=/opt/khawchin/cache/forecast
SERVICE_ACCOUNT_PATH=/opt/khawchin/serviceAccountKey.json
GOOGLE_APPLICATION_CREDENTIALS=/opt/khawchin/serviceAccountKey.json
EOF

chmod 600 /opt/khawchin/admin.env
```

Do not use a weak password. The admin page can send FCM pushes.

## 4. Add Systemd Service

Run on EC2:

```bash
sudo tee /etc/systemd/system/khawchin-admin.service <<'EOF'
[Unit]
Description=Khawchin domain admin panel
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/khawchin
EnvironmentFile=/opt/khawchin/admin.env
ExecStart=/opt/khawchin/venv/bin/python /opt/khawchin/khawchin_admin_panel.py
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now khawchin-admin
sudo systemctl status khawchin-admin --no-pager
```

Expected local check:

```bash
curl -I http://127.0.0.1:8091/admin/
```

Expected result is `401 Unauthorized` before username/password.

## 5. Add Nginx Proxy

Add this block inside the existing HTTPS `server { ... }` block for `khawchin.me`:

```nginx
    location = /admin {
        return 301 /admin/;
    }

    location /admin/ {
        proxy_pass http://127.0.0.1:8091/admin/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Authorization $http_authorization;
        proxy_read_timeout 60s;
        add_header Cache-Control "no-store" always;
    }
```

Then run:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Check:

```bash
curl -I https://khawchin.me/admin/
```

Expected result is `401 Unauthorized` before login.

## 6. Open Admin Panel

Open:

```text
https://khawchin.me/admin/
```

Login:

```text
Username: mapuia
Password: the password from /opt/khawchin/admin.env
```

Useful admin APIs behind the same login:

```text
GET  /admin/api/state
GET  /admin/api/system?level=all&log=full
GET  /admin/api/system?level=warnings&log=current
GET  /admin/api/system?level=errors&log=weekly_compare
POST /admin/api/quick/maintenance-on
POST /admin/api/quick/maintenance-off
POST /admin/api/quick/clear-announcement
POST /admin/api/archive/cleanup
```

## 7. Security Group

Do not open port `8091` to the internet.

Required public ports:

- `80`: HTTP, for redirect/Let's Encrypt
- `443`: HTTPS, app JSON and admin page
- `22`: SSH, preferably restricted to your IP if possible

Port `8091` must stay localhost-only.
