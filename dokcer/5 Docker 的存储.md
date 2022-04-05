# 5 Docker 的存储

默认情况下，在运行中的容器里创建的文件，被保存在一个可写的容器层：

- 如果容器被删除了，则数据也没有了
- 这个可写的容器层是和特定的容器绑定的，也就是这些数据无法方便的和其它容器共享

Docker 主要提供了两种方式做数据的持久化

- Data Volume, 由 Docker 管理，(/var/lib/docker/volumes/ Linux), 持久化数据的最好方式
- Bind Mount，由用户指定存储的数据具体mount在系统什么位置

![docker-volume](https://dockertips.readthedocs.io/en/latest/_images/types-of-mounts.png)

## Data Volume

### 环境准备

准备一个 Dockerfile 和一个 my-cron 的文件

```bash
$ ls
Dockerfile  my-cron
$ more Dockerfile
FROM alpine:latest
RUN apk update
RUN apk --no-cache add curl
ENV SUPERCRONIC_URL=https://github.com/aptible/supercronic/releases/download/v0.1.12/supercronic-linux-amd64 \
    SUPERCRONIC=supercronic-linux-amd64 \
    SUPERCRONIC_SHA1SUM=048b95b48b708983effb2e5c935a1ef8483d9e3e
RUN curl -fsSLO "$SUPERCRONIC_URL" \
    && echo "${SUPERCRONIC_SHA1SUM}  ${SUPERCRONIC}" | sha1sum -c - \
    && chmod +x "$SUPERCRONIC" \
    && mv "$SUPERCRONIC" "/usr/local/bin/${SUPERCRONIC}" \
    && ln -s "/usr/local/bin/${SUPERCRONIC}" /usr/local/bin/supercronic
COPY my-cron /app/my-cron
WORKDIR /app

VOLUME ["/app"]

# RUN cron job
CMD ["/usr/local/bin/supercronic", "/app/my-cron"]
$
$ more my-cron
*/1 * * * * date >> /app/test.txt
```



### 构建镜像

```bash
$ docker image build -t my-cron .
$ docker image ls
REPOSITORY   TAG       IMAGE ID       CREATED         SIZE
my-cron      latest    e9fbd9a562c9   4 seconds ago   24.7MB
```



### 创建容器(不指定 -v 参数)

此时 Docker 会自动创建一个随机名字的 volume，去存储在 Dockerfile 定义的 volume

`VOLUME ["/app"]`

```bash
$ docker run -d my-cron
9a8fa93f03c42427a498b21ac520660752122e20bcdbf939661646f71d277f8f
$ docker volume ls
DRIVER    VOLUME NAME
local     043a196c21202c484c69f2098b6b9ec22b9a9e4e4bb8d4f55a4c3dce13c15264
$ docker volume inspect 043a196c21202c484c69f2098b6b9ec22b9a9e4e4bb8d4f55a4c3dce13c15264
[
    {
        "CreatedAt": "2021-06-22T23:06:13+02:00",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/043a196c21202c484c69f2098b6b9ec22b9a9e4e4bb8d4f55a4c3dce13c15264/_data",
        "Name": "043a196c21202c484c69f2098b6b9ec22b9a9e4e4bb8d4f55a4c3dce13c15264",
        "Options": null,
        "Scope": "local"
    }
]
```

在这个 Volume 的 mountpoint 可以发现容器创建的文件。

### 创建容器(指定 -v 参数)

在创建容器的时候通过 `-v` 参数我们可以手动的指定需要创建 Volume 的名字，以及对应于容器内的路径，这个路径是可以任意的，不必需要在 Dockerfile 里通过 VOLUME 定义。

比如把上面的 Dockerfile 里的 VOLUME 删除。

```dockerfile
FROM alpine:latest
RUN apk update
RUN apk --no-cache add curl
ENV SUPERCRONIC_URL=https://github.com/aptible/supercronic/releases/download/v0.1.12/supercronic-linux-amd64 \
    SUPERCRONIC=supercronic-linux-amd64 \
    SUPERCRONIC_SHA1SUM=048b95b48b708983effb2e5c935a1ef8483d9e3e
RUN curl -fsSLO "$SUPERCRONIC_URL" \
    && echo "${SUPERCRONIC_SHA1SUM}  ${SUPERCRONIC}" | sha1sum -c - \
    && chmod +x "$SUPERCRONIC" \
    && mv "$SUPERCRONIC" "/usr/local/bin/${SUPERCRONIC}" \
    && ln -s "/usr/local/bin/${SUPERCRONIC}" /usr/local/bin/supercronic
COPY my-cron /app/my-cron
WORKDIR /app

# RUN cron job
CMD ["/usr/local/bin/supercronic", "/app/my-cron"]
```

重新 build 镜像，然后创建容器，加 -v 参数

```bash
$ docker image build -t my-cron .
$ docker container run -d -v cron-data:/app my-cron
43c6d0357b0893861092a752c61ab01bdfa62ea766d01d2fcb8b3ecb6c88b3de
$ docker volume ls
DRIVER    VOLUME NAME
local     cron-data
$ docker volume inspect cron-data
[
    {
        "CreatedAt": "2021-06-22T23:25:02+02:00",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/cron-data/_data",
        "Name": "cron-data",
        "Options": null,
        "Scope": "local"
    }
]
$ ls /var/lib/docker/volumes/cron-data/_data
my-cron
$ ls /var/lib/docker/volumes/cron-data/_data
my-cron  test.txt
```

Volume也创建了。

### 环境清理

强制删除所有容器，系统清理和volume清理。

```bash
$ docker rm -f $(docker container ps -aq)
$ docker system prune -f
$ docker volume prune -f
```



## Data Volume 练习之 MySQL

使用 MySQL 官方镜像，tag 版本为 5.7

Dockerfile 参考这里：https://github.com/docker-library/mysql/tree/master/5.7

### 准备镜像

```bash
$ docker pull mysql:5.7
$ docker image ls
REPOSITORY   TAG       IMAGE ID       CREATED        SIZE
mysql        5.7       2c9028880e58   5 weeks ago    447MB
```

### 创建容器

关于 MySQL 的镜像使用，可以参考 [dockerhub](https://hub.docker.com/_/mysql?tab=description&page=1&ordering=last_updated)

关于 Dockerfile Volume 的定义，参考 https://github.com/docker-library/mysql/tree/master/5.7

```bash
$ docker container run --name some-mysql -e MYSQL_ROOT_PASSWORD=my-secret-pw -d -v mysql-data:/var/lib/mysql mysql:5.7
02206eb369be08f660bf86b9d5be480e24bb6684c8a938627ebfbcfc0fd9e48e
$ docker volume ls
DRIVER    VOLUME NAME
local     mysql-data
$ docker volume inspect mysql-data
[
    {
        "CreatedAt": "2021-06-21T23:55:23+02:00",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/mysql-data/_data",
        "Name": "mysql-data",
        "Options": null,
        "Scope": "local"
    }
]
```

### 数据库写入数据

进入 MySQL 的 shell ，密码是 123456

```bash
$ docker container exec -it 022 sh
# mysql -u root -p
Enter password:
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 2
Server version: 5.7.34 MySQL Community Server (GPL)

Copyright (c) 2000, 2021, Oracle and/or its affiliates.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
4 rows in set (0.00 sec)

mysql> create database demo;
Query OK, 1 row affected (0.00 sec)

mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| demo               |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
5 rows in set (0.00 sec)

mysql> exit
Bye
```

创建了一个叫 demo的数据库

查看 data volume

```bash
$ docker volume inspect mysql-data
[
    {
        "CreatedAt": "2021-06-22T00:01:34+02:00",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/mysql-data/_data",
        "Name": "mysql-data",
        "Options": null,
        "Scope": "local"
    }
]
$ ls  /var/lib/docker/volumes/mysql-data/_data
auto.cnf    client-cert.pem  ib_buffer_pool  ibdata1  performance_schema  server-cert.pem
ca-key.pem  client-key.pem   ib_logfile0     ibtmp1   private_key.pem     server-key.pem
ca.pem      demo             ib_logfile1     mysql    public_key.pem      sys
```

### 其它数据库

- [MongoDB](https://hub.docker.com/_/mongo)

## Bind Mount

## Bind Mount 练习之 Dokcer 开发环境

## 多个机器之间容器共享数据

![multi-host-volume](https://dockertips.readthedocs.io/en/latest/_images/volumes-shared-storage.png)

官方参考链接 https://docs.docker.com/storage/volumes/#share-data-among-machines

Docker 的 volume 支持多种 driver。默认创建的 volume driver 都是 local.

```bash
$ docker volume inspect vscode
[
    {
        "CreatedAt": "2021-06-23T21:33:57Z",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/vscode/_data",
        "Name": "vscode",
        "Options": null,
        "Scope": "local"
    }
]
```

使用 sshfs 的 driver，让 docker 使用不在同一台机器上的文件系统做 volume。

### 环境准备

准备三台 Linux 机器，之间可以通过 SSH 相互通信。

| hostname      | ip             | ssh username | ssh password |
| ------------- | -------------- | ------------ | ------------ |
| docker-host-1 | 192.168.37.101 | vagrant      | 123456       |
| docker-host-2 | 192.168.37.102 | vagrant      | 123456       |
| docker-host-3 | 192.168.37.103 | vagrant      | 123456       |

### 安装 plugin

在其中两台机器上安装一个 plugin vieux/sshfs

```bash
[vagrant@docker-host-1 ~]$ docker plugin install --grant-all-permissions vieux/sshfs
latest: Pulling from vieux/sshfs
Digest: sha256:1d3c3e42c12138da5ef7873b97f7f32cf99fb6edde75fa4f0bcf9ed277855811
52d435ada6a4: Complete
Installed plugin vieux/sshfs
```

```bash
[vagrant@docker-host-2 ~]$ docker plugin install --grant-all-permissions vieux/sshfs
latest: Pulling from vieux/sshfs
Digest: sha256:1d3c3e42c12138da5ef7873b97f7f32cf99fb6edde75fa4f0bcf9ed277855811
52d435ada6a4: Complete
Installed plugin vieux/sshfs
```

### 创建 volume

```bash
[vagrant@docker-host-1 ~]$ docker volume create --driver vieux/sshfs \
                          -o sshcmd=vagrant@192.168.200.12:/home/vagrant \
                          -o password=vagrant \
                          sshvolume
```

查看

```bash
[vagrant@docker-host-1 ~]$ docker volume ls
DRIVER               VOLUME NAME
vieux/sshfs:latest   sshvolume
[vagrant@docker-host-1 ~]$ docker volume inspect sshvolume
[
    {
        "CreatedAt": "0001-01-01T00:00:00Z",
        "Driver": "vieux/sshfs:latest",
        "Labels": {},
        "Mountpoint": "/mnt/volumes/f59e848643f73d73a21b881486d55b33",
        "Name": "sshvolume",
        "Options": {
            "password": "vagrant",
            "sshcmd": "vagrant@192.168.200.12:/home/vagrant"
        },
        "Scope": "local"
    }
]
```

### 创建容器挂载 Volume

创建容器，挂载 sshvolume 到 /app目录，然后进入容器的 shell，在 /app 目录创建一个 test.txt 文件。

```bash
[vagrant@docker-host-1 ~]$ docker run -it -v sshvolume:/app busybox sh
Unable to find image 'busybox:latest' locally
latest: Pulling from library/busybox
b71f96345d44: Pull complete
Digest: sha256:930490f97e5b921535c153e0e7110d251134cc4b72bbb8133c6a5065cc68580d
Status: Downloaded newer image for busybox:latest
/ #
/ # ls
app   bin   dev   etc   home  proc  root  sys   tmp   usr   var
/ # cd /app
/app # ls
/app # echo "this is ssh volume"> test.txt
/app # ls
test.txt
/app # more test.txt
this is ssh volume
/app #
/app #
```

这个文件可以在 docker-host-3 上看到

```bash
[vagrant@docker-host-3 ~]$ pwd
/home/vagrant
[vagrant@docker-host-3 ~]$ ls
test.txt
[vagrant@docker-host-3 ~]$ more test.txt
this is ssh volume
```

