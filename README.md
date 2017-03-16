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
sudo apt install maven git
```
Change docker directory permissions so that your user has access to the volumes:
```
sudo chown root:docker /var/lib/docker
sudo chmod 750 /var/lib/docker/volumes
```
Checkout and build the stub project:
```
git clone https://github.com/Wnt/vaadin-fiddle-stub-project.git
cd vaadin-fiddle-stub-project
docker build --tag vaadin-stub .
```
then finally check out and run this project:
```
git clone https://github.com/Wnt/vaadin-fiddle.git
cd vaadin-fiddle
mvn jetty:run
```

## Common problems:
- Empty file list in container view

  re-set the directory permissions:

```
sudo chown root:docker /var/lib/docker
sudo chmod 750 /var/lib/docker/volumes
```
