# 9 Docker vs Podman

![podman-logo](https://dockertips.readthedocs.io/en/latest/_images/podman-logo.png)

## Podman

`Podman` 是一个基于 `Linux` 系统的 `daemon-less` 的容器引擎。 可以用来开发，管理和运行 `OCI` 标准的容器. podman可以运行在root或者非root用户模式。

Podman 是 Red Hat 在2018年推出的，源代码开放。

官方网站 https://podman.io/

OCI https://opencontainers.org/

### 和 docker 的区别

- 最主要的区别是podman是Daemonless的，而Docker在执行任务的时候，必须依赖于后台的docker daemon
- podman不需要使用root用户或者root权限，所以更安全。
- podman可以创建pod，pod的概念和Kubernetes 里定义的pod类似
- podman运行把镜像和容器存储在不同的地方，但是docker必须存储在docker engineer所在的本地
- podman是传统的fork-exec 模式，而docker是 client-server 架构

Docker 架构

![podman-vs-docker-1](https://dockertips.readthedocs.io/en/latest/_images/docker-vs-podman-1.png)

Podman 架构

![podman-vs-docker-2](https://dockertips.readthedocs.io/en/latest/_images/docker-vs-podman-2.png)

## Podman 的安装

[Vagrant 搭建的文件](podman-vagrant.zip)

参考文档：https://podman.io/getting-started/installation

## Podman 创建 pod

## Docker 的非 root 模式

[Vagrantfile](docker-rootless.zip)

文档 https://docs.docker.com/engine/security/rootless/

rootless 在使用之前需要：

```bash
$ export DOCKER_HOST=unix:///run/user/1000/docker.sock
```

