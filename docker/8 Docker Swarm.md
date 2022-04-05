# 8 Docker Swarm

## Docker Swarm

不建议在生产环境中使用 docker-Compose，因为会带来如下问题：

- 多机器如何管理？
- 如果跨机器做 scale 横向扩展 ？
- 容器失败退出的时候，如何新建容器确保服务正常运行 ？
- 如何确保零宕机时间 ？
- 如何管理密码，Key 等敏感数据 ？
- 其他



容器编排 swarm

![docker-swarm-intro](https://dockertips.readthedocs.io/en/latest/_images/docker-compose_swarm.png)

Swarm 的基本架构

![docker-swarm-arch](https://dockertips.readthedocs.io/en/latest/_images/swarm_arch.png)

dokcer swarm vs kubernetes

k8s在容器编排领域处于绝对领先的地位

2021年 redhat调查：https://www.redhat.com/en/resources/kubernetes-adoption-security-market-trends-2021-overview



## Swarm 单节点

### 初始化

### docker swarm init 背后发生的事情

主要是 PKI 和安全相关的自动化。

- 创建 swarm 集群的根证书
- manager 节点的证书
- 其它节点加入集群需要的 tokens

创建 Raft 数据库用于存储证书，配置，密码等数据，



RAFT 相关资料

- http://thesecretlivesofdata.com/raft/
- https://raft.github.io/
- https://docs.docker.com/engine/swarm/raft/

Raft 算法：https://mp.weixin.qq.com/s/p8qBcIhM04REuQ-uG4gnbw



## Swarm 三节点集群搭建

创建 3 节点 swarm cluster 的方法：

-  [play with docker 网站](https://labs.play-with-docker.com/)
  - 优点：快速方便，
  - 缺点：环境不持久，4个小时后环境会被重置
- 在本地通过虚拟化软件搭建Linux虚拟机
  - 优点：稳定，方便
  - 缺点：占用系统资源，需要电脑内存最好 8G 及其以上
- 在云上使用云主机， 亚马逊，Google，微软Azure，阿里云，腾讯云等
  - 缺点是需要消耗金钱（但是有些云服务，有免费试用）

多节点环境涉及到机器之间的通信需求，所以防火墙和网络安全策略组是一定要考虑的问题，特别是在云上使用云主机。下面的端口需要打开`防火墙`以及`设置安全策略组`

- TCP port 2376
- TCP port 2377
- TCP and UDP port 7946
- UDP port 4789

### Vagrant + Virtualbox

下载安装：`VirtualBox` https://www.virtualbox.org/

下载安装：`Vagarnt` https://www.vagrantup.com/

Vagrant 系列视频： https://space.bilibili.com/364122352/channel/detail?cid=174004

Vagrant 搭建的文件：

- [Ubuntu](vagrant-setup-ubuntu.zip)
- [Centos](vagrant-setup.zip)

虚拟机的启动：vagrant up

虚拟机的停止：vagrant halt

虚拟机的删除：vagrant destroy



## Swarm 的 overlay 网络详解

对于 swarm 的网络，有两个重要的点：

- 第一个，外部如何访问部署运行在 swarm 集群内的服务，可以称之为 `入方向` 流量，一般，在 swarm 里通过 `ingress` 来解决
- 第二个，部署在 swarm 集群里的服务，如何对外进行访问，这部分又分为两块：
  - 第一：`东西向流量`，也就是不同 swarm 节点上的容器之间如何通信，一般，swarm 通过 `overlay` 网络来解决；
  - 第二：`南北向流量`，也就是 swarm 集群里的容器如何对外访问，比如互联网，这个一般是通过 `Linux bridge + iptables NAT` 来解决的

### 创建 overlay 网络

```bash
vagrant@swarm-manager:~$ docker network create -d overlay mynet
```



### 创建服务

创建一个服务连接到这个 overlay 网络，name 是 test，replicas 是 2

```bash
vagrant@swarm-manager:~$ docker service create --network mynet --name test --replicas 2 busybox ping 8.8.8.8
vagrant@swarm-manager:~$ docker service ps test
ID             NAME      IMAGE            NODE            DESIRED STATE   CURRENT STATE            ERROR     PORTS
yf5uqm1kzx6d   test.1    busybox:latest   swarm-worker1   Running         Running 18 seconds ago
3tmp4cdqfs8a   test.2    busybox:latest   swarm-worker2   Running         Running 18 seconds ago
```

可以看到这两个容器分别被创建在 worker1 和 worker2 两个节点上

### 网络查看

到 worker1 和 worker2 上分别查看容器的网络连接情况
```bash
vagrant@swarm-worker1:~$ docker container ls
CONTAINER ID   IMAGE            COMMAND          CREATED      STATUS      PORTS     NAMES
cac4be28ced7   busybox:latest   "ping 8.8.8.8"   2 days ago   Up 2 days             test.1.yf5uqm1kzx6dbt7n26e4akhsu
vagrant@swarm-worker1:~$ docker container exec -it cac sh
/ # ip a
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
    valid_lft forever preferred_lft forever
24: eth0@if25: <BROADCAST,MULTICAST,UP,LOWER_UP,M-DOWN> mtu 1450 qdisc noqueue
    link/ether 02:42:0a:00:01:08 brd ff:ff:ff:ff:ff:ff
    inet 10.0.1.8/24 brd 10.0.1.255 scope global eth0
    valid_lft forever preferred_lft forever
26: eth1@if27: <BROADCAST,MULTICAST,UP,LOWER_UP,M-DOWN> mtu 1500 qdisc noqueue
    link/ether 02:42:ac:12:00:03 brd ff:ff:ff:ff:ff:ff
    inet 172.18.0.3/16 brd 172.18.255.255 scope global eth1
    valid_lft forever preferred_lft forever
```

这个容器有两个接口 eth0 和 eth1， 其中 eth0 是连到了 mynet 这个网络，eth1是连到 docker_gwbridge 这个网络。

```bash
vagrant@swarm-worker1:~$ docker network ls
NETWORK ID     NAME              DRIVER    SCOPE
a631a4e0b63c   bridge            bridge    local
56945463a582   docker_gwbridge   bridge    local
9bdfcae84f94   host              host      local
14fy2l7a4mci   ingress           overlay   swarm
lpirdge00y3j   mynet             overlay   swarm
c1837f1284f8   none              null      local
```

在这个容器里是可以直接 ping 通 worker2 上容器的 IP 10.0.1.9 的

![docker-swarm-overlay](https://dockertips.readthedocs.io/en/latest/_images/swarm-overlay.PNG)



## Swarm 的 Ingress 网络

docker swarm 的 ingress网络又叫 `Ingress Routing Mesh`

主要是为了实现把 service 的服务端口对外发布出去，让其能够被外部网络访问到。

ingress routing mesh 是 docker swarm 网络里最复杂的一部分内容，包括多方面的内容：

- iptables 的 Destination NAT 流量转发
- Linux bridge, network namespace
- 使用IPVS技术做负载均衡
- 包括容器间的通信（overlay）和入方向流量的端口转发

### service 创建

创建一个 service，指定网络是 overlay 的 mynet， 通过-p把端口映射出来

我们使用的镜像 `containous/whoami` 是一个简单的web服务，能返回服务器的 hostname，和基本的网络信息，比如 IP 地址

```
vagrant@swarm-manager:~$ docker service create --name web --network mynet -p 8080:80 --replicas 2 containous/whoami
a9cn3p0ovg5jcz30rzz89lyfz
overall progress: 2 out of 2 tasks
1/2: running   [==================================================>]
2/2: running   [==================================================>]
verify: Service converged
vagrant@swarm-manager:~$ docker service ls
ID             NAME      MODE         REPLICAS   IMAGE                      PORTS
a9cn3p0ovg5j   web       replicated   2/2        containous/whoami:latest   *:8080->80/tcp
vagrant@swarm-manager:~$ docker service ps web
ID             NAME      IMAGE                      NODE            DESIRED STATE   CURRENT STATE            ERROR     PORTS
udlzvsraha1x   web.1     containous/whoami:latest   swarm-worker1   Running         Running 16 seconds ago
mms2c65e5ygt   web.2     containous/whoami:latest   swarm-manager   Running         Running 16 seconds ago
vagrant@swarm-manager:~$
```

## service 访问

8080 这个端口到底映射到哪里了？尝试三个 swarm 节点的IP加端口 8080。

可以看到三个节点IP都可以访问，并且回应的容器是不同的（hostname），也就是有负载均衡的效果。

```
vagrant@swarm-manager:~$ curl 192.168.200.10:8080
Hostname: fdf7c1354507
IP: 127.0.0.1
IP: 10.0.0.7
IP: 172.18.0.3
IP: 10.0.1.14
RemoteAddr: 10.0.0.2:36828
GET / HTTP/1.1
Host: 192.168.200.10:8080
User-Agent: curl/7.68.0
Accept: */*

vagrant@swarm-manager:~$ curl 192.168.200.11:8080
Hostname: fdf7c1354507
IP: 127.0.0.1
IP: 10.0.0.7
IP: 172.18.0.3
IP: 10.0.1.14
RemoteAddr: 10.0.0.3:54212
GET / HTTP/1.1
Host: 192.168.200.11:8080
User-Agent: curl/7.68.0
Accept: */*

vagrant@swarm-manager:~$ curl 192.168.200.12:8080
Hostname: c83ee052787a
IP: 127.0.0.1
IP: 10.0.0.6
IP: 172.18.0.3
IP: 10.0.1.13
RemoteAddr: 10.0.0.4:49820
GET / HTTP/1.1
Host: 192.168.200.12:8080
User-Agent: curl/7.68.0
Accept: */*
```

![docker-swarm-ingress-logic](https://dockertips.readthedocs.io/en/latest/_images/swarm-ingress-logic.PNG)

### ingress 数据包的走向

以 manager 节点为例，数据到底是如何达到 service 的 container 的。

```bash
vagrant@swarm-manager:~$ sudo iptables -nvL -t nat
Chain PREROUTING (policy ACCEPT 388 packets, 35780 bytes)
pkts bytes target     prot opt in     out     source               destination
296 17960 DOCKER-INGRESS  all  --  *      *       0.0.0.0/0            0.0.0.0/0            ADDRTYPE match dst-type LOCAL
21365 1282K DOCKER     all  --  *      *       0.0.0.0/0            0.0.0.0/0            ADDRTYPE match dst-type LOCAL

Chain INPUT (policy ACCEPT 388 packets, 35780 bytes)
pkts bytes target     prot opt in     out     source               destination

Chain OUTPUT (policy ACCEPT 340 packets, 20930 bytes)
pkts bytes target     prot opt in     out     source               destination
    8   590 DOCKER-INGRESS  all  --  *      *       0.0.0.0/0            0.0.0.0/0            ADDRTYPE match dst-type LOCAL
    1    60 DOCKER     all  --  *      *       0.0.0.0/0           !127.0.0.0/8          ADDRTYPE match dst-type LOCAL

Chain POSTROUTING (policy ACCEPT 340 packets, 20930 bytes)
pkts bytes target     prot opt in     out     source               destination
    2   120 MASQUERADE  all  --  *      docker_gwbridge  0.0.0.0/0            0.0.0.0/0            ADDRTYPE match src-type LOCAL
    3   252 MASQUERADE  all  --  *      !docker0  172.17.0.0/16        0.0.0.0/0
    0     0 MASQUERADE  all  --  *      !docker_gwbridge  172.18.0.0/16        0.0.0.0/0

Chain DOCKER (2 references)
pkts bytes target     prot opt in     out     source               destination
    0     0 RETURN     all  --  docker0 *       0.0.0.0/0            0.0.0.0/0
    0     0 RETURN     all  --  docker_gwbridge *       0.0.0.0/0            0.0.0.0/0

Chain DOCKER-INGRESS (2 references)
pkts bytes target     prot opt in     out     source               destination
    2   120 DNAT       tcp  --  *      *       0.0.0.0/0            0.0.0.0/0            tcp dpt:8080 to:172.18.0.2:8080
302 18430 RETURN     all  --  *      *       0.0.0.0/0            0.0.0.0/0
```

通过 iptables，可以看到一条 DNAT 的规则，所有访问本地 8080 端口的流量都被转发到 172.18.0.2:8080

那这个172.18.0.2 是什么？

首先　172.18.0.0/16　这个网段是 `docker_gwbridge` 的，所以这个地址肯定是连在了 `docker_gwbridge` 上。

`docker network inspect docker_gwbridge` 可以看到这个网络连接了一个叫　`ingress-sbox`　的容器。它的地址就是　172.18.0.2/16

这个　`ingress-sbox`　其实并不是一个容器，而是一个网络的命名空间　network namespace,　我们可以通过下面的方式进入到这个命名空间

```bash
vagrant@swarm-manager:~$　docker run -it --rm -v /var/run/docker/netns:/netns --privileged=true nicolaka/netshoot nsenter --net=/netns/ingress_sbox sh
~ # ip a
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
8: eth0@if9: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP group default
    link/ether 02:42:0a:00:00:02 brd ff:ff:ff:ff:ff:ff link-netnsid 0
    inet 10.0.0.2/24 brd 10.0.0.255 scope global eth0
       valid_lft forever preferred_lft forever
    inet 10.0.0.5/32 scope global eth0
       valid_lft forever preferred_lft forever
10: eth1@if11: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default
    link/ether 02:42:ac:12:00:02 brd ff:ff:ff:ff:ff:ff link-netnsid 1
    inet 172.18.0.2/16 brd 172.18.255.255 scope global eth1
       valid_lft forever preferred_lft forever
```

通过查看地址，发现这个命名空间连接了两个网络，一个eth1是连接了　`docker_gwbridge`　，另外一个eth0连接了　`ingress` 这个网络。

```bash
~ # ip route
default via 172.18.0.1 dev eth1
10.0.0.0/24 dev eth0 proto kernel scope link src 10.0.0.2
172.18.0.0/16 dev eth1 proto kernel scope link src 172.18.0.2

~ # iptables -nvL -t mangle
Chain PREROUTING (policy ACCEPT 22 packets, 2084 bytes)
 pkts bytes target     prot opt in     out     source               destination
   12   806 MARK       tcp  --  *      *       0.0.0.0/0            0.0.0.0/0            tcp dpt:8080 MARK set 0x100

Chain INPUT (policy ACCEPT 14 packets, 1038 bytes)
 pkts bytes target     prot opt in     out     source               destination
    0     0 MARK       all  --  *      *       0.0.0.0/0            10.0.0.5             MARK set 0x100

Chain FORWARD (policy ACCEPT 8 packets, 1046 bytes)
 pkts bytes target     prot opt in     out     source               destination

Chain OUTPUT (policy ACCEPT 14 packets, 940 bytes)
 pkts bytes target     prot opt in     out     source               destination

Chain POSTROUTING (policy ACCEPT 22 packets, 1986 bytes)
 pkts bytes target     prot opt in     out     source               destination
~ # ipvsadm
IP Virtual Server version 1.2.1 (size=4096)
Prot LocalAddress:Port Scheduler Flags
  -> RemoteAddress:Port           Forward Weight ActiveConn InActConn
FWM  256 rr
  -> 10.0.0.6:0                   Masq    1      0          0
  -> 10.0.0.7:0                   Masq    1      0          0
```

通过 ipvs 做了负载均衡

- 这是一个stateless load balancing
- 这是三层的负载均衡，不是四层的 LB is at OSI Layer 3 (TCP), not Layer 4 (DNS)
- 以上两个限制可以通过Nginx或者HAProxy LB proxy解决 （https://docs.docker.com/engine/swarm/ingress/）

## 内部负载均衡和 VIP

创建一个 mynet 的 overlay 网络，创建一个 service

```bash
vagrant@swarm-manager:~$ docker network ls
NETWORK ID     NAME              DRIVER    SCOPE
afc8f54c1d07   bridge            bridge    local
128fd1cb0fae   docker_gwbridge   bridge    local
0ea68b0d28b9   host              host      local
14fy2l7a4mci   ingress           overlay   swarm
lpirdge00y3j   mynet             overlay   swarm
a8edf1804fb6   none              null      local
vagrant@swarm-manager:~$ docker service create --name web --network mynet --replicas 2 containous/whoami
jozc1x1c1zpyjl9b5j5abzm0g
overall progress: 2 out of 2 tasks
1/2: running   [==================================================>]
2/2: running   [==================================================>]
verify: Service converged
vagrant@swarm-manager:~$ docker service ls
ID             NAME      MODE         REPLICAS   IMAGE                      PORTS
jozc1x1c1zpy   web       replicated   2/2        containous/whoami:latest
vagrant@swarm-manager:~$ docker service ps web
ID             NAME      IMAGE                      NODE            DESIRED STATE   CURRENT STATE            ERROR     PORTS
pwi87g86kbxd   web.1     containous/whoami:latest   swarm-worker1   Running         Running 47 seconds ago
xbri2akxy2e8   web.2     containous/whoami:latest   swarm-worker2   Running         Running 44 seconds ago
vagrant@swarm-manager:~$
```

创建一个 client

```bash
vagrant@swarm-manager:~$ docker service create --name client --network mynet xiaopeng163/net-box:latest ping 8.8.8.8
skbcdfvgidwafbm4nciq82env
overall progress: 1 out of 1 tasks
1/1: running   [==================================================>]
verify: Service converged
vagrant@swarm-manager:~$ docker service ls
ID             NAME      MODE         REPLICAS   IMAGE                        PORTS
skbcdfvgidwa   client    replicated   1/1        xiaopeng163/net-box:latest
jozc1x1c1zpy   web       replicated   2/2        containous/whoami:latest
vagrant@swarm-manager:~$ docker service ps client
ID             NAME       IMAGE                        NODE            DESIRED STATE   CURRENT STATE            ERROR     PORTS
sg9b3dqrgru4   client.1   xiaopeng163/net-box:latest   swarm-manager   Running         Running 28 seconds ago
vagrant@swarm-manager:~$
```

尝试进入client这个容器，去ping web这个service name， 获取到的IP 10.0.1.30，称之为VIP（虚拟IP）

```bash
vagrant@swarm-manager:~$ docker container ls
CONTAINER ID   IMAGE                        COMMAND          CREATED          STATUS          PORTS     NAMES
36dce35d56e8   xiaopeng163/net-box:latest   "ping 8.8.8.8"   19 minutes ago   Up 19 minutes             client.1.sg9b3dqrgru4f14k2tpxzg2ei
vagrant@swarm-manager:~$ docker container exec -it 36dc sh
/omd # curl web
Hostname: 6039865a1e5d
IP: 127.0.0.1
IP: 10.0.1.32
IP: 172.18.0.3
RemoteAddr: 10.0.1.37:40972
GET / HTTP/1.1
Host: web
User-Agent: curl/7.69.1
Accept: */*

/omd # curl web
Hostname: c3b3e99b9bb1
IP: 127.0.0.1
IP: 10.0.1.31
IP: 172.18.0.3
RemoteAddr: 10.0.1.37:40974
GET / HTTP/1.1
Host: web
User-Agent: curl/7.69.1
Accept: */*

/omd # curl web
Hostname: 6039865a1e5d
IP: 127.0.0.1
IP: 10.0.1.32
IP: 172.18.0.3
RemoteAddr: 10.0.1.37:40976
GET / HTTP/1.1
Host: web
User-Agent: curl/7.69.1
Accept: */*

/omd #
/omd # ping web -c 2
PING web (10.0.1.30): 56 data bytes
64 bytes from 10.0.1.30: seq=0 ttl=64 time=0.044 ms
64 bytes from 10.0.1.30: seq=1 ttl=64 time=0.071 ms

--- web ping statistics ---
2 packets transmitted, 2 packets received, 0% packet loss
round-trip min/avg/max = 0.044/0.057/0.071 ms
/omd #
```

这个虚拟 IP 在一个特殊的网络命令空间里，这个空间连接在 mynet 这个 overlay 的网络上。

通过 docker network inspect mynet 可以看到这个命名空间，叫 lb-mynet：

```bash
"Containers": {
"36dce35d56e87d43d08c5b9a94678fe789659cb3b1a5c9ddccd7de4b26e8d588": {
    "Name": "client.1.sg9b3dqrgru4f14k2tpxzg2ei",
    "EndpointID": "e8972d0091afaaa091886799aca164b742ca93408377d9ee599bdf91188416c1",
    "MacAddress": "02:42:0a:00:01:24",
    "IPv4Address": "10.0.1.36/24",
    "IPv6Address": ""
},
"lb-mynet": {
    "Name": "mynet-endpoint",
    "EndpointID": "e299d083b25a1942f6e0f7989436c3c3e8d79c7395a80dd50b7709825022bfac",
    "MacAddress": "02:42:0a:00:01:25",
    "IPv4Address": "10.0.1.37/24",
    "IPv6Address": ""
}
```

通过下面的命令，找到这个命名空间的名字：

```bash
vagrant@swarm-manager:~$ sudo ls /var/run/docker/netns/
1-14fy2l7a4m  1-lpirdge00y  dfb766d83076  ingress_sbox  lb_lpirdge00
vagrant@swarm-manager:~$
```

名字叫 `lb_lpirdge00`

通过 nsente r进入到这个命名空间的sh里， 可以看到刚才的 VIP地址 10.0.1.30

```bash
vagrant@swarm-manager:~$ sudo nsenter --net=/var/run/docker/netns/lb_lpirdge00 sh
#
# ip a
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
    valid_lft forever preferred_lft forever
50: eth0@if51: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP group default
    link/ether 02:42:0a:00:01:25 brd ff:ff:ff:ff:ff:ff link-netnsid 0
    inet 10.0.1.37/24 brd 10.0.1.255 scope global eth0
    valid_lft forever preferred_lft forever
    inet 10.0.1.30/32 scope global eth0
    valid_lft forever preferred_lft forever
    inet 10.0.1.35/32 scope global eth0
    valid_lft forever preferred_lft forever
#
```

和 ingress 网络一样，可以查看 iptables，ipvs 的负载均衡， 基本就可以理解负载均衡是怎么一回事了。 Mark=0x106, 也就是 262（十进制），会轮询把请求发给 10.0.1.31 和 10.0.1.32

```bash
# iptables -nvL -t mangle
Chain PREROUTING (policy ACCEPT 128 packets, 11198 bytes)
pkts bytes target     prot opt in     out     source               destination

Chain INPUT (policy ACCEPT 92 packets, 6743 bytes)
pkts bytes target     prot opt in     out     source               destination
72  4995 MARK       all  --  *      *       0.0.0.0/0            10.0.1.30            MARK set 0x106
    0     0 MARK       all  --  *      *       0.0.0.0/0            10.0.1.35            MARK set 0x107

Chain FORWARD (policy ACCEPT 36 packets, 4455 bytes)
pkts bytes target     prot opt in     out     source               destination

Chain OUTPUT (policy ACCEPT 101 packets, 7535 bytes)
pkts bytes target     prot opt in     out     source               destination

Chain POSTROUTING (policy ACCEPT 128 packets, 11198 bytes)
pkts bytes target     prot opt in     out     source               destination
# ipvsadm
IP Virtual Server version 1.2.1 (size=4096)
Prot LocalAddress:Port Scheduler Flags
-> RemoteAddress:Port           Forward Weight ActiveConn InActConn
FWM  262 rr
-> 10.0.1.31:0                  Masq    1      0          0
-> 10.0.1.32:0                  Masq    1      0          0
FWM  263 rr
-> 10.0.1.36:0                  Masq    1      0          0
```

这个流量会走 mynet 这个 overlay 网络。



## 部署多 service 应用

如何像 docker-compose 一样部署多服务应用。

所用的源码文件 https://github.com/xiaopeng163/flask-redis

创建一个 mynet 的 overlay 网络：

```bash
vagrant@swarm-manager:~$ docker network ls
NETWORK ID     NAME              DRIVER    SCOPE
afc8f54c1d07   bridge            bridge    local
128fd1cb0fae   docker_gwbridge   bridge    local
0ea68b0d28b9   host              host      local
14fy2l7a4mci   ingress           overlay   swarm
lpirdge00y3j   mynet             overlay   swarm
a8edf1804fb6   none              null      local
vagrant@swarm-manager:~$
```

创建一个 redis 的 service

```bash
vagrant@swarm-manager:~$ docker service create --network mynet --name redis redis:latest redis-server --requirepass ABC123
qh3nfeth3wc7uoz9ozvzta5ea
overall progress: 1 out of 1 tasks
1/1: running   [==================================================>]
verify: Service converged
vagrant@swarm-manager:~$ docker servce ls
docker: 'servce' is not a docker command.
See 'docker --help'
vagrant@swarm-manager:~$ docker service ls
ID             NAME      MODE         REPLICAS   IMAGE          PORTS
qh3nfeth3wc7   redis     replicated   1/1        redis:latest
vagrant@swarm-manager:~$ docker service ps redis
ID             NAME      IMAGE          NODE            DESIRED STATE   CURRENT STATE            ERROR     PORTS
111cpkjn4a0k   redis.1   redis:latest   swarm-worker2   Running         Running 19 seconds ago
vagrant@swarm-manager:~$
```

创建一个 flask 的 service

```bash
vagrant@swarm-manager:~$ docker service create --network mynet --name flask --env REDIS_HOST=redis --env REDIS_PASS=ABC123 -p 8080
:5000 xiaopeng163/flask-redis:latest
y7garhvlxah592j5lmqv8a3xj
overall progress: 1 out of 1 tasks
1/1: running   [==================================================>]
verify: Service converged
vagrant@swarm-manager:~$ docker service ls
ID             NAME      MODE         REPLICAS   IMAGE                            PORTS
y7garhvlxah5   flask     replicated   1/1        xiaopeng163/flask-redis:latest   *:8080->5000/tcp
qh3nfeth3wc7   redis     replicated   1/1        redis:latest
vagrant@swarm-manager:~$ docker service ps flask
ID             NAME      IMAGE                            NODE            DESIRED STATE   CURRENT STATE            ERROR     PORTS
quptcq7vb48w   flask.1   xiaopeng163/flask-redis:latest   swarm-worker1   Running         Running 15 seconds ago
vagrant@swarm-manager:~$ curl 127.0.0.1:8080
Hello Container World! I have been seen 1 times and my hostname is d4de54036614.
vagrant@swarm-manager:~$ curl 127.0.0.1:8080
Hello Container World! I have been seen 2 times and my hostname is d4de54036614.
vagrant@swarm-manager:~$ curl 127.0.0.1:8080
Hello Container World! I have been seen 3 times and my hostname is d4de54036614.
vagrant@swarm-manager:~$ curl 127.0.0.1:8080
Hello Container World! I have been seen 4 times and my hostname is d4de54036614.
vagrant@swarm-manager:~$
```



## Swarm stack 部署多 Service 应用

先在 swarm manager 节点上安装一下 docker-compose

```bash
vagrant@swarm-manager:~$ sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
vagrant@swarm-manager:~$ sudo chmod +x /usr/local/bin/docker-compose
```

clone 代码仓库

```bash
vagrant@swarm-manager:~$ git clone https://github.com/xiaopeng163/flask-redis
Cloning into 'flask-redis'...
remote: Enumerating objects: 22, done.
remote: Counting objects: 100% (22/22), done.
remote: Compressing objects: 100% (19/19), done.
remote: Total 22 (delta 9), reused 7 (delta 2), pack-reused 0
Unpacking objects: 100% (22/22), 8.60 KiB | 1.07 MiB/s, done.
vagrant@swarm-manager:~$ cd flask-redis
vagrant@swarm-manager:~/flask-redis$ ls
Dockerfile  LICENSE  README.md  app.py  docker-compose.yml
vagrant@swarm-manager:~/flask-redis$
```

环境清理

```
vagrant@swarm-manager:~/flask-redis$ docker system prune -a -f
```

镜像构建和提交， 

```
vagrant@swarm-manager:~/flask-redis$ docker-compose build
vagrant@swarm-manager:~/flask-redis$ docker image ls
REPOSITORY                TAG          IMAGE ID       CREATED         SIZE
xiaopeng163/flask-redis   latest       5efb4fcbcfc3   6 seconds ago   126MB
python                    3.9.5-slim   c71955050276   3 weeks ago     115MB
```

提交镜像到 dockerhub

```bash
vagrant@swarm-manager:~/flask-redis$ docker login
Login with your Docker ID to push and pull images from Docker Hub. If you don't have a Docker ID, head over to https://hub.docker.com to create one.
Username: xiaopeng163
Password:
WARNING! Your password will be stored unencrypted in /home/vagrant/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credentials-store

Login Succeeded
vagrant@swarm-manager:~/flask-redis$ docker-compose push
WARNING: The REDIS_PASSWORD variable is not set. Defaulting to a blank string.
Pushing flask (xiaopeng163/flask-redis:latest)...
The push refers to repository [docker.io/xiaopeng163/flask-redis]
f447d33c161b: Pushed
f7395da2fd9c: Pushed
5b156295b5a3: Layer already exists
115e0863702d: Layer already exists
e10857b94a57: Layer already exists
8d418cbfaf25: Layer already exists
764055ebc9a7: Layer already exists
latest: digest: sha256:c909100fda2f4160b593b4e0fb692b89046cebb909ae90546627deca9827b676 size: 1788
vagrant@swarm-manager:~/flask-redis$
```

通过 stack 启动服务

```bash
vagrant@swarm-manager:~/flask-redis$ env REDIS_PASSWORD=ABC123 docker stack deploy --compose-file docker-compose.yml flask-demo
Ignoring unsupported options: build

Creating network flask-demo_default
Creating service flask-demo_flask
Creating service flask-demo_redis-server
vagrant@swarm-manager:~/flask-redis$
vagrant@swarm-manager:~/flask-redis$ docker stack ls
NAME         SERVICES   ORCHESTRATOR
flask-demo   2          Swarm
vagrant@swarm-manager:~/flask-redis$ docker stack ps flask-demo
ID             NAME                        IMAGE                            NODE            DESIRED STATE   CURRENT STATE
ERROR     PORTS
lzm6i9inoa8e   flask-demo_flask.1          xiaopeng163/flask-redis:latest   swarm-manager   Running         Running 23 seconds ago

ejojb0o5lbu0   flask-demo_redis-server.1   redis:latest                     swarm-worker2   Running         Running 21 seconds ago

vagrant@swarm-manager:~/flask-redis$ docker stack services flask-demo
ID             NAME                      MODE         REPLICAS   IMAGE                            PORTS
mpx75z1rrlwn   flask-demo_flask          replicated   1/1        xiaopeng163/flask-redis:latest   *:8080->5000/tcp
z85n16zsldr1   flask-demo_redis-server   replicated   1/1        redis:latest
vagrant@swarm-manager:~/flask-redis$ docker service ls
ID             NAME                      MODE         REPLICAS   IMAGE                            PORTS
mpx75z1rrlwn   flask-demo_flask          replicated   1/1        xiaopeng163/flask-redis:latest   *:8080->5000/tcp
z85n16zsldr1   flask-demo_redis-server   replicated   1/1        redis:latest
vagrant@swarm-manager:~/flask-redis$ curl 127.0.0.1:8080
Hello Container World! I have been seen 1 times and my hostname is 21d63a8bfb57.
vagrant@swarm-manager:~/flask-redis$ curl 127.0.0.1:8080
Hello Container World! I have been seen 2 times and my hostname is 21d63a8bfb57.
vagrant@swarm-manager:~/flask-redis$ curl 127.0.0.1:8080
Hello Container World! I have been seen 3 times and my hostname is 21d63a8bfb57.
vagrant@swarm-manager:~/flask-redis$
```



## 在 Swarm 中使用 Secret

文档： https://docs.docker.com/engine/swarm/secrets/

### 创建secret

有两种方式

- 从标准的收入读取

  ```bash
  vagrant@swarm-manager:~$ echo abc123 | docker secret create mysql_pass -
  4nkx3vpdd41tbvl9qs24j7m6w
  vagrant@swarm-manager:~$ docker secret ls
  ID                          NAME         DRIVER    CREATED         UPDATED
  4nkx3vpdd41tbvl9qs24j7m6w   mysql_pass             8 seconds ago   8 seconds ago
  vagrant@swarm-manager:~$ docker secret inspect mysql_pass
  [
      {
          "ID": "4nkx3vpdd41tbvl9qs24j7m6w",
          "Version": {
              "Index": 4562
          },
          "CreatedAt": "2021-07-25T22:36:51.544523646Z",
          "UpdatedAt": "2021-07-25T22:36:51.544523646Z",
          "Spec": {
              "Name": "mysql_pass",
              "Labels": {}
          }
      }
  ]
  vagrant@swarm-manager:~$ docker secret rm mysql_pass
  mysql_pass
  vagrant@swarm-manager:~$
  ```

- 从文件读取

  ```bash
  vagrant@swarm-manager:~$ ls
  mysql_pass.txt
  vagrant@swarm-manager:~$ more mysql_pass.txt
  abc123
  vagrant@swarm-manager:~$ docker secret create mysql_pass mysql_pass.txt
  elsodoordd7zzpgsdlwgynq3f
  vagrant@swarm-manager:~$ docker secret inspect mysql_pass
  [
      {
          "ID": "elsodoordd7zzpgsdlwgynq3f",
          "Version": {
              "Index": 4564
          },
          "CreatedAt": "2021-07-25T22:38:14.143954043Z",
          "UpdatedAt": "2021-07-25T22:38:14.143954043Z",
          "Spec": {
              "Name": "mysql_pass",
              "Labels": {}
          }
      }
  ]
  vagrant@swarm-manager:~$
  ```

  

### secret 的使用

参考 https://hub.docker.com/_/mysql

```bash
vagrant@swarm-manager:~$ docker service create --name mysql-demo --secret mysql_pass --env MYSQL_ROOT_PASSWORD_FILE=/run/secrets/mysql_pass mysql:5.7
wb4z2ximgqaefephu9f4109c7
overall progress: 1 out of 1 tasks
1/1: running   [==================================================>]
verify: Service converged
vagrant@swarm-manager:~$ docker service ls
ID             NAME         MODE         REPLICAS   IMAGE       PORTS
wb4z2ximgqae   mysql-demo   replicated   1/1        mysql:5.7
vagrant@swarm-manager:~$ docker service ps mysql-demo
ID             NAME           IMAGE       NODE            DESIRED STATE   CURRENT STATE            ERROR     PORTS
909429p4uovy   mysql-demo.1   mysql:5.7   swarm-worker2   Running         Running 32 seconds ago
vagrant@swarm-manager:~$
```

## 在 Swarm 中使用 local volume

`docker-compose.yml`

```yml
version: "3.8"

services:
  db:
    image: mysql:5.7
    environment:
      - MYSQL_ROOT_PASSWORD_FILE=/run/secrets/mysql_pass
    secrets:
      - mysql_pass
    volumes:
      - data:/var/lib/mysql

volumes:
  data:

secrets:
  mysql_pass:
    file: mysql_pass.txt
```



`mysql_pass.txt`

```bash
vagrant@swarm-manager:~$ more mysql_pass.txt
abc123
vagrant@swarm-manager:~$
```



## 在 Swarm 部署投票 App

源码：https://github.com/dockersamples/example-voting-app







































