# 7 Docker Compose

## Docker Compose

![docker-compose-intro](https://dockertips.readthedocs.io/en/latest/_images/docker-compose-intro.PNG)

## Dokcer Compose 的安装

Windows 和 Mac 在默认安装了 docker desktop 以后，docker-compose 随之自动安装。



Linux 需要自行安装

最新版本号可以在这里查询 https://github.com/docker/compose/releases

```bash
$ sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
$ sudo chmod +x /usr/local/bin/docker-compose
$ docker-compose --version
docker-compose version 1.29.2, build 5becea4c
```

除此之外，还可以使用 pip 去安装 docker-compose，命令如下：

```bash
$ pip install docker-compose
```



## Docker Compose 文件的结构和版本

docker compose 文件的语法说明 https://docs.docker.com/compose/compose-file/

### 基本语法结构

```yaml
version: "3.8"

services: # 容器
  servicename: # 服务名字，这个名字也是内部 bridge网络可以使用的 DNS name
    image: # 镜像的名字
    command: # 可选，如果设置，则会覆盖默认镜像里的 CMD命令
    environment: # 可选，相当于 docker run里的 --env
    volumes: # 可选，相当于docker run里的 -v
    networks: # 可选，相当于 docker run里的 --network
    ports: # 可选，相当于 docker run里的 -p
  servicename2:

volumes: # 可选，相当于 docker volume create

networks: # 可选，相当于 docker network create
```

以 Python Flask + Redis 为例子，改造成一个docker-compose文件

```bash
docker image pull redis
docker image build -t flask-demo .

# create network
docker network create -d bridge demo-network

# create container
docker container run -d --name redis-server --network demo-network redis
docker container run -d --network demo-network --name flask-demo --env REDIS_HOST=redis-server -p 5000:5000 flask-demo
```

docker-compose.yml 文件如下：

```yaml
version: "3.8"

services:
  flask-demo:
    image: flask-demo:latest
    environment:
      - REDIS_HOST=redis-server
    networks:
      - demo-network
    ports:
      - 8080:5000

  redis-server:
    image: redis:latest
    networks:
     - demo-network

networks:
  demo-network:
```

### docker-compose 语法版本

向后兼容，https://docs.docker.com/compose/compose-file/ .

## Docker Compose 水平扩展

- [compose scale example](compose-scale-example-1.zip)

### 环境清理

删除所有容器和镜像

```bash
$ docker container rm -f $(docker container ps -aq)
$ docker system prune -a -f
```

### 启动

下载源码，进入源码目录

```bash
$ docker-compose pull
$ docker-compose build
$ docker-compose up -d
Creating network "compose-scale-example_default" with the default driver
Creating compose-scale-example_flask_1        ... done
Creating compose-scale-example_client_1       ... done
Creating compose-scale-example_redis-server_1 ... done
$ docker-compose ps
                Name                              Command               State    Ports
----------------------------------------------------------------------------------------
compose-scale-example_client_1         sh -c while true; do sleep ...   Up
compose-scale-example_flask_1          flask run -h 0.0.0.0             Up      5000/tcp
compose-scale-example_redis-server_1   docker-entrypoint.sh redis ...   Up      6379/tcp
```



### 水平扩展 scale

```bash
$ docker-compose up -d --scale flask=3
compose-scale-example_client_1 is up-to-date
compose-scale-example_redis-server_1 is up-to-date
Creating compose-scale-example_flask_2 ... done
Creating compose-scale-example_flask_3 ... done
$ docker-compose ps
                Name                              Command               State    Ports
----------------------------------------------------------------------------------------
compose-scale-example_client_1         sh -c while true; do sleep ...   Up
compose-scale-example_flask_1          flask run -h 0.0.0.0             Up      5000/tcp
compose-scale-example_flask_2          flask run -h 0.0.0.0             Up      5000/tcp
compose-scale-example_flask_3          flask run -h 0.0.0.0             Up      5000/tcp
compose-scale-example_redis-server_1   docker-entrypoint.sh redis ...   Up      6379/tcp
```



### 添加 nginx

- [ompose-scale-example-2](ompose-scale-example-2.zip)

## Docker Compose 环境变量

- [compose-env](compose-env.zip)

参考文档：https://docs.docker.com/compose/environment-variables/

## Docker Compose 服务依赖和健康检查

Dockerfile healthcheck https://docs.docker.com/engine/reference/builder/#healthcheck

docker compose https://docs.docker.com/compose/compose-file/compose-file-v3/#healthcheck

健康检查是容器运行状态的高级检查，主要是检查容器所运行的进程是否能正常的对外提供“服务”，比如一个数据库容器，我们不光 需要这个容器是up的状态，我们还要求这个容器的数据库进程能够正常对外提供服务，这就是所谓的健康检查。

### 容器的健康检查

容器本身有一个健康检查的功能，但是需要在Dockerfile里定义，或者在执行docker container run 的时候，通过下面的一些参数指定。

```bash
--health-cmd string              Command to run to check health
--health-interval duration       Time between running the check
                                (ms|s|m|h) (default 0s)
--health-retries int             Consecutive failures needed to
                                report unhealthy
--health-start-period duration   Start period for the container to
                                initialize before starting
                                health-retries countdown
                                (ms|s|m|h) (default 0s)
--health-timeout duration        Maximum time to allow one check to
```

#### 栗子：

以下面的这个flask容器为例，相关的代码如下：

```bash
PS C:\Users\superhsc\coding\compose-env\flask> dir


    目录: C:\Users\superhsc\coding\compose-env\flask


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
-a----         2021/7/13     15:52            448 app.py
-a----         2021/7/14      0:32            471 Dockerfile


PS C:\Users\superhsc\coding\compose-env\flask> more .\app.py
from flask import Flask
from redis import StrictRedis
import os
import socket

app = Flask(__name__)
redis = StrictRedis(host=os.environ.get('REDIS_HOST', '127.0.0.1'),
                    port=6379, password=os.environ.get('REDIS_PASS'))


@app.route('/')
def hello():
    redis.incr('hits')
    return f"Hello Container World! I have been seen {redis.get('hits').decode('utf-8')} times and my hostname is {socket.gethostname()}.\n"

PS C:\Users\superhsc\coding\compose-env\flask> more .\Dockerfile
FROM python:3.9.5-slim

RUN pip install flask redis && \
    apt-get update && \
    apt-get install -y curl && \
    groupadd -r flask && useradd -r -g flask flask && \
    mkdir /src && \
    chown -R flask:flask /src

USER flask

COPY app.py /src/app.py

WORKDIR /src

ENV FLASK_APP=app.py REDIS_HOST=redis

EXPOSE 5000

HEALTHCHECK --interval=30s --timeout=3s \
    CMD curl -f http://localhost:5000/ || exit 1

CMD ["flask", "run", "-h", "0.0.0.0"]
```

上面 Dockerfile 里的 HEALTHCHECK 就是定义了一个健康检查。 会每隔 30 秒检查一次，如果失败就会退出，退出代码是 1。

#### 构建镜像和创建容器

构建镜像，创建一个 bridge 网络，然后启动容器连到 bridge 网络。

```bash
$ docker image build -t flask-demo .
$ docker network create mybridge
$ docker container run -d --network mybridge --env REDIS_PASS=abc123 flask-demo
```

查看容器状态

```bash
$ docker container ls
CONTAINER ID   IMAGE        COMMAND                  CREATED       STATUS                            PORTS      NAMES
059c12486019   flask-demo   "flask run -h 0.0.0.0"   4 hours ago   Up 8 seconds (health: starting)   5000/tcp   dazzling_tereshkova
```

也可以通过 docker container inspect 059 查看详情， 其中有有关 health 的：

```json
"Health": {
"Status": "starting",
"FailingStreak": 1,
"Log": [
    {
        "Start": "2021-07-14T19:04:46.4054004Z",
        "End": "2021-07-14T19:04:49.4055393Z",
        "ExitCode": -1,
        "Output": "Health check exceeded timeout (3s)"
    }
]
}
```

经过3次检查，一直是不通的，然后health的状态会从starting变为 unhealthy：

```bash
docker container ls
CONTAINER ID   IMAGE        COMMAND                  CREATED       STATUS                     PORTS      NAMES
059c12486019   flask-demo   "flask run -h 0.0.0.0"   4 hours ago   Up 2 minutes (unhealthy)   5000/tcp   dazzling_tereshkova
```

### 启动redis服务器

启动redis，连到mybridge上，name=redis， 注意密码。

```bash
$ docker container run -d --network mybridge --name redis redis:latest redis-server --requirepass abc123
```

经过几秒钟，flask 变成了 healthy

```bash
$ docker container ls
CONTAINER ID   IMAGE          COMMAND                  CREATED          STATUS                   PORTS      NAMES
bc4e826ee938   redis:latest   "docker-entrypoint.s…"   18 seconds ago   Up 16 seconds            6379/tcp   redis
059c12486019   flask-demo     "flask run -h 0.0.0.0"   4 hours ago      Up 6 minutes (healthy)   5000/tcp   dazzling_tereshkova
```

### docker-compose 健康检查

- [flask healthcheck](compose-healthcheck-flask.zip)
- [flask + redis healthcheck](compose-healthcheck-redis.zip)

一个 healthcheck 不错的例子 https://gist.github.com/phuysmans/4f67a7fa1b0c6809a86f014694ac6c3a

## Docker Compose 投票 app 练习

- 源码地址： https://github.com/dockersamples/example-voting-app