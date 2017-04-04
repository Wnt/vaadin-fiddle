vaadinfiddleprototype
==============

## Development environment setup instructions
assuming Ubuntu 16.04 based system

Install Docker CE. Installation instructions at https://docs.docker.com/engine/installation/linux/ubuntu/

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

Install project prerequisites:
```
sudo apt install maven git nginx-light
```
Change docker directory permissions so that your user has access to the volumes:
```
sudo chown root:docker /var/lib/docker
sudo chown root:docker /var/lib/docker/volumes
sudo chmod 750 /var/lib/docker/volumes
```
Checkout and build the stub project:
```
git clone https://github.com/Wnt/vaadin-fiddle-stub-project.git
cd vaadin-fiddle-stub-project
docker build --tag vaadin-stub .
```
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
