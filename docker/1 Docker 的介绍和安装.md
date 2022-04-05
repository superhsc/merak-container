# 1 Docker 的介绍和安装

## 容器技术的介绍

> ✔ 容器 Container 指的是一种技术，而 Docker 只是一个容器技术的实现，是让容器技术普及开来的最成功的实现

### 容器正在引领基础架构的一场新的革命

![](http://assets.processon.com/chart_image/624ac17d1e085307894a4bd8.png)

#### 什么是容器 Container ？

容器是一种快速的打包技术。

> Package Sotfware into Standardized Units for Development, Shipment and Deployment

三个大特点，如下：

- 标准化
- 轻量级
- 容易移植

#### 为什么容器技术会出现 ？

容器技术出现之前 

![why_container1](https://dockertips.readthedocs.io/en/latest/_images/why_container_1.png)

容器技术出现之后

![why_container2](https://dockertips.readthedocs.io/en/latest/_images/why_container_2.png)

[容器 vs 虚拟机](https://k21academy.com/docker-kubernetes/docker-vs-virtual-machine/)

![container_vs_vm](https://dockertips.readthedocs.io/en/latest/_images/container_vs_vm.png)





Linux Container 容器技术诞生于 2008 年（Docker 诞生于 2013 年），解决了 IT 世界里 “集装箱运输问题”。

Linux Container，简称 LXC。是一种内核轻量级的操作系统层虚拟化技术。

Linux Container 主要是由 [Namespace](https://en.wikipedia.org/wiki/Linux_namespaces) 和 [Cgroups](https://en.wikipedia.org/wiki/Cgroups) 两大机制来保证实现。

- Namespace ，命名空间，主要用于资源的隔离，诞生于 2002 年；

- Cgroups，Control Cgroups ，负责资源管理控制，比如：

  - 进程组使用 CPU/MEM 的限制

  - 进程组的优先级控制

  - 进程组的挂起和恢复等等

    由 Google 贡献，2008 年合并到了 Linux Kernel .

### 容器的标准化

`docker != contianer`

2015 年，由 Google，Docker  以及红帽等厂商联合发起了 OCI（Open Container Initiative） 组织，致力于容器技术的[标准化](https://opencontainers.org/)

**容器运行时标准 runtime spec**

简单来讲，该标准规定了容器的基本操作规范，比如如何下载镜像，创建容器以及启动容器等。

**容器镜像标准 image spec**

该标准主要定义了镜像的基本格式。

### 容器关乎"速度"

容器的优点，关乎六个速度

1. 加速软件开发
2. 加速程序编译和构建
3. 加速测试
4. 加速部署
5. 加速更新
6. 加速故障恢复

### 容器的快速发展和普及

到 2020 年，全球以及超过 50% 的公司在生产环境中使用了 container -- [Gartner](https://www.docker.com/blog/docker-index-shows-continued-massive-developer-adoption-and-activity-to-build-and-share-apps-with-docker/)

## Docker 的架构

![dockerarch](https://dockertips.readthedocs.io/en/latest/_images/docker-architecture.PNG)

## Docker 的安装

### 在 Windows 系统上安装 Docker

下载 [Docker Desktop for Windows](https://docs.docker.com/docker-for-windows/install/)，如果使用 Windows 10 ，建议首先安装 WSL2 。

- 如果 Windows 10 没有安装 WSL 2 ，选择 Hyper-V backend and Windows Containers.
  - 安装 Wid
- 如果 Windows 10 已经安装了 WSL 2 ，具体参考 [Windows10开发环境搭建(B站)](https://space.bilibili.com/364122352/channel/detail?cid=166238) 或 [Windows10开发环境搭建(YouTube)](https://www.youtube.com/playlist?list=PLfQqWeOCIH4ACS0037k1KLNIv5f646jbr) .

### 在 Mac 系统上安装 Docker 

下载 [Docker Desktop for Mac](https://docs.docker.com/docker-for-mac/install/)。这里需要注意下版本：

- intel 芯片，选择 Mac With Intel Chip
- M1 芯片，选择 Mac With Apple Chip

### 在 Linux 系统上安装 Docker

注意：需要使用命令 `cat /etc/centos-release` 看下 Linux 的版本。

#### 方式一，此方式会下载最新的版本 ：

第一步；获取安装脚本，地址是：https://get.docker.com/.

第二步；执行命令 `curl -fsSL get.docker.com -o get-docker.sh`.

第三步：执行脚本即可

第四步：启动并加入开机启动，执行命令：`systemctl start dokcer`和`sytemctl enable docker`

第五步：验证，执行命令：docker version(需要 root 权限)，结果如下：

![image-20220404190905134](C:\Users\superhsc\AppData\Roaming\Typora\typora-user-images\image-20220404190905134.png)

#### 方式二，指定特定的版本：

第一步：添加依赖
```bash
yum install -y yum-utils device-mapper-persistent-data lvm2
```

第二步：设置 yum 源
```bash
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
```

第三步：查看仓库中所有 Docker 版本
```bash
yum list docker-ce --showduplicates | sort -r
```

第四步：安装 Docker
```bash
yum -y install docker-ce
```

第五步：启动并加入开机启动
```bash
systemctl start docker
systemctl enable docker
```

第六步：验证，执行命令 docker version，结果如方式一的第五步

### Dokcer Machine 搭建 Docker 环境

参考地址：https://docs.docker.com/machine/

### 通过 Vagrant 搭建 Linux Docker 环境

Vagrant 是一个快速创建虚拟机的工具。

参考地址：https://space.bilibili.com/364122352/channel/detail?cid=174004

## 问题总结

### 问题一：Windows 10 家庭版安装 Hyper-V

此问题，在官方中是不支持的，但是民间有一些方式可以尝试，不过我没有尝试过。

https://www.itechtics.com/enable-hyper-v-windows-10-home/

根据经验，可以如下操作：

- 升级到 Windows Pro ;
- 不用 hyper-v，使用 virtualbox 或者 vmware 创建 Linux 虚拟机。

### 问题二：docker 在 Windows 上启动失败，报错： the docker client must be run with elevated privileges to connect

![docker-install-error](https://dockertips.readthedocs.io/en/latest/_images/win-docker-install-error.png)



遇到此问题，只需要管理员方式执行 CMD 命令 `netst winsock reset` 即可。

