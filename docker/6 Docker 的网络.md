# 6 Docker 的网络

## 网络基础知识回顾

[IP、子网掩码、网关、DNS、端口号](https://zhuanlan.zhihu.com/p/65226634)

[Internet 工作方式](https://www.hp.com/us-en/shop/tech-takes/how-does-the-internet-work)

[从数据包的角度详细解析](https://www.homenethowto.com/advanced-topics/traffic-example-the-full-picture/)

## 网络常用命令

### IP 地址的查看

Windows：`ipconfig`

Linux：`ifconfig` 或者 `ip addr`

### 网络连通性测试

**ping 命令**

![image-20220404234927006](C:\Users\superhsc\AppData\Roaming\Typora\typora-user-images\image-20220404234927006.png)

**telnet 命令：测试端口的连通性**

![image-20220404234743408](C:\Users\superhsc\AppData\Roaming\Typora\typora-user-images\image-20220404234743408.png)

**traceroute 命令：路径探测跟踪**

Linux 下使用 tracepath

![image-20220404233237370](C:\Users\superhsc\AppData\Roaming\Typora\typora-user-images\image-20220404233237370.png)

Windows 下使用 `TRACRT.EXT` 

![image-20220404233025258](C:\Users\superhsc\AppData\Roaming\Typora\typora-user-images\image-20220404233025258.png)

curl 命令：请求 web 服务，参考：http://www.ruanyifeng.com/blog/2019/09/curl-reference.html

## Docker Bridge 网络

![docker-volume](https://dockertips.readthedocs.io/en/latest/_images/two-container-network.png)

### 创建两个容器

```bash
$ docker container run -d --rm --name box1 busybox /bin/sh -c "while true; do sleep 3600; done"
$ docker container run -d --rm --name box2 busybox /bin/sh -c "while true; do sleep 3600; done"
$ docker container ls
CONTAINER ID   IMAGE     COMMAND                  CREATED          STATUS          PORTS     NAMES
4f3303c84e53   busybox   "/bin/sh -c 'while t…"   49 minutes ago   Up 49 minutes             box2
03494b034694   busybox   "/bin/sh -c 'while t…"   49 minutes ago   Up 49 minutes             box1
```

### 容器间通信

两个容器都连接到了一个叫 docker0 的 Linux bridge 上。

```bash
$ docker network ls
NETWORK ID     NAME      DRIVER    SCOPE
1847e179a316   bridge    bridge    local
a647a4ad0b4f   host      host      local
fbd81b56c009   none      null      local
$ docker network inspect bridge
[
    {
        "Name": "bridge",
        "Id": "1847e179a316ee5219c951c2c21cf2c787d431d1ffb3ef621b8f0d1edd197b24",
        "Created": "2021-07-01T15:28:09.265408946Z",
        "Scope": "local",
        "Driver": "bridge",
        "EnableIPv6": false,
        "IPAM": {
            "Driver": "default",
            "Options": null,
            "Config": [
                {
                    "Subnet": "172.17.0.0/16",
                    "Gateway": "172.17.0.1"
                }
            ]
        },
        "Internal": false,
        "Attachable": false,
        "Ingress": false,
        "ConfigFrom": {
            "Network": ""
        },
        "ConfigOnly": false,
        "Containers": {
            "03494b034694982fa085cc4052b6c7b8b9c046f9d5f85f30e3a9e716fad20741": {
                "Name": "box1",
                "EndpointID": "072160448becebb7c9c333dce9bbdf7601a92b1d3e7a5820b8b35976cf4fd6ff",
                "MacAddress": "02:42:ac:11:00:02",
                "IPv4Address": "172.17.0.2/16",
                "IPv6Address": ""
            },
            "4f3303c84e5391ea37db664fd08683b01decdadae636aaa1bfd7bb9669cbd8de": {
                "Name": "box2",
                "EndpointID": "4cf0f635d4273066acd3075ec775e6fa405034f94b88c1bcacdaae847612f2c5",
                "MacAddress": "02:42:ac:11:00:03",
                "IPv4Address": "172.17.0.3/16",
                "IPv6Address": ""
            }
        },
        "Options": {
            "com.docker.network.bridge.default_bridge": "true",
            "com.docker.network.bridge.enable_icc": "true",
            "com.docker.network.bridge.enable_ip_masquerade": "true",
            "com.docker.network.bridge.host_binding_ipv4": "0.0.0.0",
            "com.docker.network.bridge.name": "docker0",
            "com.docker.network.driver.mtu": "1500"
        },
        "Labels": {}
    }
]
```

> brctl 使用前需要安装, 对于 CentOS， 可以通过 `sudo yum install -y bridge-utils` 安装. 对于Ubuntu, 可以通过 `sudo apt-get install -y bridge-utils`

```bash
$ brctl show
bridge name     bridge id               STP enabled     interfaces
docker0         8000.0242759468cf       no              veth8c9bb82
                                                        vethd8f9afb
```



### 容器对外通信

查看路由

```bash
$ ip route
default via 10.0.2.2 dev eth0 proto dhcp metric 100
10.0.2.0/24 dev eth0 proto kernel scope link src 10.0.2.15 metric 100
172.17.0.0/16 dev docker0 proto kernel scope link src 172.17.0.1
192.168.200.0/24 dev eth1 proto kernel scope link src 192.168.200.10 metric 101
```

[iptable](https://access.redhat.com/documentation/en-US/Red_Hat_Enterprise_Linux/4/html/Security_Guide/s1-firewall-ipt-fwd.html) 转发规则

```bash
$ sudo iptables --list -t nat
Chain PREROUTING (policy ACCEPT)
target     prot opt source               destination
DOCKER     all  --  anywhere             anywhere             ADDRTYPE match dst-type LOCAL

Chain INPUT (policy ACCEPT)
target     prot opt source               destination

Chain OUTPUT (policy ACCEPT)
target     prot opt source               destination
DOCKER     all  --  anywhere            !loopback/8           ADDRTYPE match dst-type LOCAL

Chain POSTROUTING (policy ACCEPT)
target     prot opt source               destination
MASQUERADE  all  --  172.17.0.0/16        anywhere

Chain DOCKER (2 references)
target     prot opt source               destination
RETURN     all  --  anywhere             anywhere
```



### 端口转发

第一步：创建容器

```bash
$ docker container run -d --rm --name web -p 8080:80 nginx
$ docker container inspect --format '{{.NetworkSettings.IPAddress}}' web
$ docker container run -d --rm --name client busybox /bin/sh -c "while true; do sleep 3600; done"
$ docker container inspect --format '{{.NetworkSettings.IPAddress}}' client
$ docker container exec -it client wget http://172.17.0.2
```



第二步：查看 iptables 的端口转发规则

```bash
[vagrant@docker-host1 ~]$ sudo iptables -t nat -nvxL
Chain PREROUTING (policy ACCEPT 10 packets, 1961 bytes)
    pkts      bytes target     prot opt in     out     source               destination
    1       52 DOCKER     all  --  *      *       0.0.0.0/0            0.0.0.0/0            ADDRTYPE match dst-type LOCAL

Chain INPUT (policy ACCEPT 9 packets, 1901 bytes)
    pkts      bytes target     prot opt in     out     source               destination

Chain OUTPUT (policy ACCEPT 2 packets, 120 bytes)
    pkts      bytes target     prot opt in     out     source               destination
    0        0 DOCKER     all  --  *      *       0.0.0.0/0           !127.0.0.0/8          ADDRTYPE match dst-type LOCAL

Chain POSTROUTING (policy ACCEPT 4 packets, 232 bytes)
    pkts      bytes target     prot opt in     out     source               destination
    3      202 MASQUERADE  all  --  *      !docker0  172.17.0.0/16        0.0.0.0/0
    0        0 MASQUERADE  tcp  --  *      *       172.17.0.2           172.17.0.2           tcp dpt:80

Chain DOCKER (2 references)
    pkts      bytes target     prot opt in     out     source               destination
    0        0 RETURN     all  --  docker0 *       0.0.0.0/0            0.0.0.0/0
    1       52 DNAT       tcp  --  !docker0 *       0.0.0.0/0            0.0.0.0/0            tcp dpt:8080 to:172.17.0.2:80
```



## 创建和使用 Bridge

看视频！

## Docker Host 网络

看视频！

## 网络命名空间

Linux的Namespace（命名空间）技术是一种隔离技术，常用的Namespace有 user namespace, process namespace, network namespace等

在Docker容器中，不同的容器通过Network namespace进行了隔离，也就是不同的容器有各自的IP地址，路由表等，互不影响。

> 准备一台Linux机器，这一节会用到一个叫 `brtcl` 的命令，这个命令需要安装，如果是Ubuntu的系统，可以通过 `apt-get install bridge-utils` 安装；如果是Centos系统，可以通过 `sudo yum install bridge-utils` 来安装

![docker-volume](https://dockertips.readthedocs.io/en/latest/_images/network-namespace.png)



### 创建 bridge

```bash
[vagrant@docker-host1 ~]$ sudo brctl addbr mydocker0
[vagrant@docker-host1 ~]$ brctl show
bridge name     bridge id               STP enabled     interfaces
mydocker0               8000.000000000000       no
[vagrant@docker-host1 ~]$
```



### 准备一个 shell 脚本

脚本名字叫 `add-ns-to-br.sh`

```shell
#!/bin/bash

bridge=$1
namespace=$2
addr=$3

vethA=veth-$namespace
vethB=eth00

sudo ip netns add $namespace
sudo ip link add $vethA type veth peer name $vethB

sudo ip link set $vethB netns $namespace
sudo ip netns exec $namespace ip addr add $addr dev $vethB
sudo ip netns exec $namespace ip link set $vethB up

sudo ip link set $vethA up

sudo brctl addif $bridge $vethA
```

### 脚本执行

```bash
[vagrant@docker-host1 ~]$ sh add-ns-to-br.sh mydocker0 ns1 172.16.1.1/16
[vagrant@docker-host1 ~]$ sh add-ns-to-br.sh mydocker0 ns2 172.16.1.2/16
```

把 mydocker0 这个 bridge up 起来。

```bash
[vagrant@docker-host1 ~]$ sudo ip link set dev mydocker0 up
```

### 验证

```bash
[vagrant@docker-host1 ~]$ sudo ip netns exec ns1 bash
[root@docker-host1 vagrant]# ip a
1: lo: <LOOPBACK> mtu 65536 qdisc noop state DOWN group default qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
5: eth00@if6: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default qlen 1000
    link/ether f2:59:19:34:73:70 brd ff:ff:ff:ff:ff:ff link-netnsid 0
    inet 172.16.1.1/16 scope global eth00
    valid_lft forever preferred_lft forever
    inet6 fe80::f059:19ff:fe34:7370/64 scope link
    valid_lft forever preferred_lft forever
[root@docker-host1 vagrant]# ping 172.16.1.2
PING 172.16.1.2 (172.16.1.2) 56(84) bytes of data.
64 bytes from 172.16.1.2: icmp_seq=1 ttl=64 time=0.029 ms
64 bytes from 172.16.1.2: icmp_seq=2 ttl=64 time=0.080 ms
^C
--- 172.16.1.2 ping statistics ---
2 packets transmitted, 2 received, 0% packet loss, time 1000ms
rtt min/avg/max/mdev = 0.029/0.054/0.080/0.026 ms
[root@docker-host1 vagrant]#
```

### 对外通信

https://www.karlrupp.net/en/computer/nat_tutorial

## Python Flask + Reids 练习



### 程序准备

准备一个Python文件，名字为 `app.py` 内容如下：

```python
from flask import Flask
from redis import Redis
import os
import socket

app = Flask(__name__)
redis = Redis(host=os.environ.get('REDIS_HOST', '127.0.0.1'), port=6379)


@app.route('/')
def hello():
    redis.incr('hits')
    return f"Hello Container World! I have been seen {redis.get('hits').decode('utf-8')} times and my hostname is {socket.gethostname()}.\n"
```

准备一个Dockerfile：

```dockerfile
FROM python:3.9.5-slim

RUN pip install flask redis && \
    groupadd -r flask && useradd -r -g flask flask && \
    mkdir /src && \
    chown -R flask:flask /src

USER flask

COPY app.py /src/app.py

WORKDIR /src

ENV FLASK_APP=app.py REDIS_HOST=redis

EXPOSE 5000

CMD ["flask", "run", "-h", "0.0.0.0"]
```

### 镜像准备

构建flask镜像，准备一个redis镜像。

```bash
$ docker image pull redis
$ docker image build -t flask-demo .
$ docker image ls
REPOSITORY   TAG          IMAGE ID       CREATED              SIZE
flask-demo   latest       4778411a24c5   About a minute ago   126MB
python       3.9.5-slim   c71955050276   8 days ago           115MB
redis        latest       08502081bff6   2 weeks ago          105MB
```



### 创建一个 docker bridge

```bash
$ docker network create -d bridge demo-network
8005f4348c44ffe3cdcbbda165beea2b0cb520179d3745b24e8f9e05a3e6456d
$ docker network ls
NETWORK ID     NAME           DRIVER    SCOPE
2a464c0b8ec7   bridge         bridge    local
8005f4348c44   demo-network   bridge    local
80b63f711a37   host           host      local
fae746a75be1   none           null      local
$
```



### 创建 redis container

创建一个叫 `redis-server` 的 container，连到 demo-network 上

```bash
$ docker container run -d --name redis-server --network demo-network redis
002800c265020310231d689e6fd35bc084a0fa015e8b0a3174aa2c5e29824c0e
$ docker container ls
CONTAINER ID   IMAGE     COMMAND                  CREATED         STATUS         PORTS      NAMES
002800c26502   redis     "docker-entrypoint.s…"   4 seconds ago   Up 3 seconds   6379/tcp   redis-server
```



### 创建 flask container

```bash
$ docker container run -d --network demo-network --name flask-demo --env REDIS_HOST=redis-server -p 5000:5000 flask-demo
```

打开浏览器访问 [http://127.0.0.1:5000](http://127.0.0.1:5000/)

应该能看到类似下面的内容，每次刷新页面，计数加 1

Hello Container World! I have been seen 36 times and my hostname is 925ecb8d111a.

### 总结

如果把上面的步骤合并到一起，成为一个部署脚本

```shell
# prepare image
docker image pull redis
docker image build -t flask-demo .

# create network
docker network create -d bridge demo-network

# create container
docker container run -d --name redis-server --network demo-network redis
docker container run -d --network demo-network --name flask-demo --env REDIS_HOST=redis-server -p 5000:5000 flask-demo
```

