VaadinFiddle
==============
VaadinFiddle is an online platform for developing Vaadin applications. Use it to share examples, test out new features and just to fiddle with Vaadin code. All from the comfort of your web browser - no installation needed!

https://vaadinfiddle.com/editor/
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
Because the Docker service resets the directory permissions to `root:root` startup, we need to set our special permissions each time the service is started. Create `/usr/local/bin/docker_directory_permissions.sh` with the following content:
```
#!/usr/bin/env bash
chown root:docker /var/lib/docker
chown root:docker /var/lib/docker/volumes
chmod 750 /var/lib/docker/volumes
```
and set it executable:
```
sudo chmod +x /usr/local/bin/docker_directory_permissions.sh
```
Next we need to make the system run that script whenever the Docker service is started. Create directory to host local docker service customization:
```
sudo mkdir /etc/systemd/system/docker.service.d/
```
add the following content into `/etc/systemd/system/docker.service.d/directory_permissions.conf`:
```
[Service]
ExecStartPost=/usr/local/bin/docker_directory_permissions.sh
```
and reload the systemd manager configuration and restart the service e.g. by running:
```
sudo systemctl daemon-reload
sudo systemctl restart docker.service
```
Now you should get the more relaxed directory permissions in `/var/lib/docker/volumes`:
```
ls -ld /var/lib/docker/volumes
drwxr-x--- 4 root docker 4096 loka  27 13:59 /var/lib/docker/volumes
```

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
to restart the service.
## Optional: install PhantomJS to enable share previews
```
sudo apt install phantomjs
```
