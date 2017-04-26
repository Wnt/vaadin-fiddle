vaadinfiddleprototype
==============

## Development environment setup instructions
assuming Ubuntu 16.04 based system

### Install Docker CE
Full installation instructions at https://docs.docker.com/engine/installation/linux/ubuntu/

e.g.
```
sudo apt install apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt update
sudo apt install docker-ce
```
add your user to the docker group
```
sudo usermod --append --groups docker <your unix user name>
```
and re-login.

### Install project prerequisites:
```
sudo apt install maven git nginx-light
```
Change docker directory permissions so that your user has access to the volumes:
```
sudo chown root:docker /var/lib/docker
sudo chown root:docker /var/lib/docker/volumes
sudo chmod 750 /var/lib/docker/volumes
```
### Build stub project Docker image
Checkout and build the stub project:
```
git clone https://github.com/Wnt/vaadin-fiddle-stub-project.git
cd vaadin-fiddle-stub-project
docker build --tag vaadin-stub .
```
### Configure nginx
To allow nginx configuration reloading following line to `/etc/sudoers`:
```
%docker ALL=NOPASSWD: /bin/systemctl reload nginx.service
```
Create a directory to host the nginx VaadinFiddle configuration
```
sudo mkdir /etc/nginx/fiddle-config
sudo chown root:docker /etc/nginx/fiddle-config
sudo chmod 775 /etc/nginx/fiddle-config
```
and checkout the stub configuration in to there:
```
git clone https://github.com/Wnt/nginx-fiddle-config.git /etc/nginx/fiddle-config
```
and include the VaadinFiddle configuration in the nginx configuration
```
include fiddle-config/fiddle-host.conf;
```
e.g. in to `/etc/nginx/sites-available/default` after this block:
```
location / {
        # First attempt to serve request as file, then
        # as directory, then fall back to displaying a 404.
        try_files $uri $uri/ =404;
}

include fiddle-config/fiddle-host.conf;
```
### Build and run
then finally check out and run this project:
```
git clone https://github.com/Wnt/vaadin-fiddle.git
cd vaadin-fiddle
mvn jetty:run
```
then you can access the VaadinFiddle ui on your localhost at:
http://localhost/editor/
## Common problems:
- Empty file list in container view

  re-set the directory permissions:

```
sudo chown root:docker /var/lib/docker
sudo chown root:docker /var/lib/docker/volumes
sudo chmod 750 /var/lib/docker/volumes
```
## Optional: automatically set directory permissions
Create directory to host local docker service customisation
```
mkdir /etc/systemd/system/docker.service.d/
```
and add the following content into `/etc/systemd/system/docker.service.d/directory_permissions.conf`:
```
[Service]
ExecStartPost=/usr/local/bin/docker_directory_permissions.sh
```
then create `/usr/local/bin/docker_directory_permissions.sh` and put following content into there:
```
#!/usr/bin/env bash
chown root:docker /var/lib/docker
chown root:docker /var/lib/docker/volumes
chmod 750 /var/lib/docker/volumes
```
and set it executable: `sudo chmod +x /usr/local/bin/docker_directory_permissions.sh`
then run:
```
sudo systemctl daemon-reload
sudo systemctl restart docker.service
```
to restart the service
## Optional: restrict container networking
```
sudo apt instal ufw
```
Change `ENABLED` to `yes` in `/etc/ufw/ufw.conf`
Allow inbound ssh, http & https:
```
sudo ufw allow ssh
sudo ufw allow http
sudo ufw allow https
```
create directory to host local docker service customisation
```
mkdir /etc/systemd/system/docker.service.d/
```
and add the following content into `/etc/systemd/system/docker.service.d/network_whitelist.conf`:
```
[Service]
ExecStartPost=/usr/local/bin/docker_network_whitelist.sh
```
then create `/usr/local/bin/docker_network_whitelist.sh` and put following content into there:
```
#!/usr/bin/env bash
iptables --append DOCKER --protocol udp --destination <dns server> --destination-port 53 --jump ACCEPT
iptables --append DOCKER --protocol udp --source <dns server> --source-port 53 --jump ACCEPT
iptables --append DOCKER --protocol tcp --destination <allowed http server> --destination-port 80 --jump ACCEPT
iptables --append DOCKER --protocol tcp --source <allowed http server> --source-port 80 --jump ACCEPT
iptables --append DOCKER --protocol tcp --destination <allowed https server> --destination-port 443 --jump ACCEPT
iptables --append DOCKER --protocol tcp --source <allowed http server> --source-port 443 --jump ACCEPT
iptables --append DOCKER --jump DROP
```
adapt the server addresses to suit your use-case.
and set it executable: `sudo chmod +x /usr/local/bin/docker_network_whitelist.sh`
then run:
```
sudo systemctl daemon-reload
sudo systemctl restart docker.service
```
to restart the service
