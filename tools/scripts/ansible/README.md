# Ansible for Survey Tool

These are ansible scripts for setup and maintenance of the Survey Tool.

## Scope

Right now, the test setup mostly controls OpenLiberty, but not the nginx proxy
due to public port issues (https).

## Setup

### Setup: Control system

This is your local system, where you control the others from.

- Install Ansible <https://ansible.com>
- Install some prereqs:

```shell
ansible-galaxy install -r requirements.yml
```

- Make sure you can `ssh` into all of the needed systems. For example,
`ssh cldr-ref.unicode.org` should succeed without needing a password.

- You should be able to run `ansible all -m ping` and get something back
like the following:

```shell
cldr-ref.unicode.org | SUCCESS => {
    "ansible_facts": {
        "discovered_interpreter_python": "/usr/bin/python"
    },
    "changed": false,
    "ping": "pong"
}
```

### Setup: Managed systems

- Install python3. Make sure `python --version`
or `python3 --version` returns "Python 3…"

- TODO: these shouldn't be needed, but they are. Here's the entire
install command:

```shell
sudo apt-get update && sudo apt-get install python3 python-apt python3-pymysql
```

### Setup: surveytool keypair

Create a RSA keypair with no password for the buildbot:

```shell
mkdir -p ./local-vars
ssh-keygen -t rsa -b 4096 -f ./local-vars/surveytool -P '' -C 'surveytool deploy'
```

The contents of the `local-vars/surveytool.pub` file is used for the
`key:` parameter below in `local.yml`. The `local-vars/surveytool`
private key is used in the secret `RSA_KEY_SURVEYTOOL`.

Then setup github secrets as shown:

- `SMOKETEST_HOST` -
  hostname of smoketest
- `SMOKETEST_PORT` -
  port of smoketest
- `RSA_KEY_SURVEYTOOL` -
  contents of `local-vars/surveytool` (the secret key)
- `SMOKETEST_KNOWNHOSTS` -
  run `ssh-keyscan smoketest.example.com` where _smoketest.example.com_
  is the name of the smoketest server.  Put the results into this
  secret. One of these lines should match `~/.ssh/known_hosts` on your
  own system when you ssh into smoketest.
  Try `grep -i smoke ~/.ssh/known_hosts`

Create a folder "cldrbackup" inside local-vars
```shell
mkdir -p ./local-vars/cldrbackup
```

Add three files inside local-vars/cldrbackup-vars: id_rsa, id_rsa.pub, and known_hosts. These must correspond to the public key for cldrbackup on corp.unicode.org. Copy existing versions if you have them. Otherwise, create new ones with `ssh-keygen -t rsa` and copy the public key to corp.unicode.org with `ssh-copy-id -i ~/.ssh/id_rsa cldrbackup@corp.unicode.org`

### Setup: Config file

- Create a file `local-vars/local.yml` matching the example values in [test-local-vars/local.yml](test-local-vars/local.yml) but with secure passwords instead of `hunter42`, ...!

```yaml
cldradmin_pw: hunter46 # needs to match cldradmin pw below
mysql_users:
  # this is the account used by the survey tool itself
  # password will match /var/lib/openliberty/usr/servers/cldr/server.env
  - name: surveytool
    host: localhost
    password: hunter42
    priv: 'cldrdb.*:ALL'
  # this is the account used for administrative tasks
  # password will match /home/cldradmin/.my.sql
  - name: cldradmin
    password: hunter46
    priv: 'cldrdb.*:ALL/*.*:PROCESS'
    append_privs: yes
# this is the account used for deployment
surveytooldeploy:
  # TODO: surveytooldeploy.password appears to be unused?
  password: hunter43
  # vap will match CLDR_VAP in /srv/st/config/cldr.properties
  vap: hunter44
  # testpw will match CLDR_TESTPW in /srv/st/config/cldr.properties
  testpw: hunter45
  oldversion: 39
  newversion: 40
  key: ssh-rsa …  ( SSH key goes here)
  certbot_admin_email: surveytool@unicode.org
  certbot_certs:
    - domains:
      - cldr-ref.unicode.org
```

## Setup: cldrcc

```shell
mkdir -p local-vars/cldrcc
ssh-keygen -t rsa -b 2048 -C 'CLDR Commit Checker' -f local-vars/cldrcc/id_rsa
```


## Configure

Run the setup playbook.

```shell
ansible-playbook --check setup-playbook.yml
```

This is in dry run mode. When it looks good to you, take the
`--check` out and run it again.

You can also use the `-l cldr-smoke.unicode.org` option to limit
the operation to a single host.

## Local Test

Here’s how to deploy the SurveyTool locally and try it out.

### Build

You need a server zipfile to deploy.  This is a file such as `cldr-apps.zip`. When expanded, it contains a directory tree beginning with `wlp/`.

#### Option A: Local Build

- Prerequisites: See <https://cldr.unicode.org/development/maven> and follow instructions to be able to run `mvn package` as shown on that page.

- You can then create a server zipfile locally with maven using these command (from the top `cldr/` directory).  The first command does a full build of CLDR, but skips running tests.

```shell
mvn --file=tools/pom.xml install -DskipTests=true
mvn --file=tools/pom.xml -pl cldr-apps liberty:package
```

- The output file will be in `tools/cldr-apps/target/cldr-apps.zip`


#### Option B: Download a Build

- Server Builds are actually attached to each action run in <https://github.com/unicode-org/cldr/actions/workflows/maven.yml>, look for an artifact entitled `cldr-apps-server` at the bottom of a run.

- *Warning*: Clicking on this artifact will download a zipfile named `cldr-apps-server.zip` which _contains_ `cldr-apps.zip`.  Double clicking or automatic downloading will often extract one too many levels of zipfiles. If you see a folder named `wlp` then you have extracted too much. From the command line you can unpack with `unzip cldr-apps-server.zip` which will extract `cldr-apps.zip`.

### Deploy

- install [vagrant](https://www.vagrantup.com) and some provider such as virtualbox or libvirt, see vagrant docs.

- vagrant up!

```shell
# (this directory)
cd tools/scripts/ansible
vagrant up
```

- To log into the new host, run `vagrant ssh`

- To iterate, trying to reapply ansible, run `vagrant provision --provision-with=ansible`

- to deploy your built server to this, use the following:

```shell
# Note 1: $(git rev-parse HEAD) just turns into a full git hash such as 72dda8d7386087bf6087de200b5edc002feca2f2, you can use an explicit hash instead.
# Note 2: change ../../cldr-apps/target/cldr-apps.zip to point to your cldr-apps.zip file if moved
vagrant ssh -- sudo -u surveytool /usr/local/bin/deploy-to-openliberty.sh $(git rev-parse HEAD) < ../../cldr-apps/target/cldr-apps.zip
```

- Now you should be able to login at <http://127.0.0.1:9081/cldr-apps/>

- Use the user `admin@` and the password set in `surveytooldeploy.vap` above.

- *Note*: <http://127.0.0.1:8880> will go to the nginx proxy, but it has login problems, see <https://unicode-org.atlassian.net/browse/CLDR-14321>

### Operation

- the mvn build and `deploy-to-openliberty.sh` steps above can be repeated to redeploy a new version of the server code
- `vagrant ssh` to login and poke around at the server
- `sudo nano /srv/st/config/cldr.properties` to edit the configuration file (will be created automatically at first ST boot, restart server to pickup changes).
- `sudo journalctl -f` to watch server logs
- `sudo systemctl restart openliberty@cldr` to restart the server
- Logs are in `/var/log/openliberty/cldr`
- `sudo -u cldradmin mysql cldrdb` will give you the raw SQL prompt
