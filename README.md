# 第四届阿里中间件性能挑战赛初赛

### 1 前言

本次比赛是本人第一次参与程序设计类比赛，两名队友来自浙江大学人工智能实验室，我来自上海交通大学机电所。此刻我们三人已经研一下，对于未来十分迷茫，希望通过比赛为自己挣一份简历。

![image](https://github.com/Qiwc/Service-Mesh-Agent/blob/master/images/1.jpg)

初赛，我们使用JAVA，全代码建立在Netty基础上，看完[《Netty实战》](https://github.com/waylau/essential-netty-in-action)就直接上手干了！当然看之前要懂JAVA NIO，特别是Selector，才能理解Netty线程模型。

初赛时间正赶我们三人都在搞老板的活，进了复赛放飞自我。最终成绩虽然108名，但是也上了**6000+**的分数，思路与前排java大佬的思路基本差不多，只是没有针对数据做特定的优化。

[题目链接](https://code.aliyun.com/middlewarerace2018/docs?spm=5176.12281978.0.0.88f754ab9kdBzo&accounttraceid=b17ca8cc-a7c0-4ada-97fd-492470b5fe8d)

[前排JAVA大佬开源代码，条理清晰，建议与本代码对比看，各自有各自的优点](https://www.cnkirito.moe/dubboMesh/)

------

### 2 结构

![image](https://github.com/Qiwc/Service-Mesh-Agent/blob/master/images/struct.png)

- 每个Agent既要作为服务端又要作为客户端，这期间要做协议转换。为了通用性，CA与PA之间的协议用的Google的ProtoBuf。
- 所有链接均采用长链接，省去重复握手挥手时间
- Agent作为客户端的Channel与作为服务端的Channel注册在同一个EventLoop上，节省线程切换时间。这是一个非常值得关注的优化点。
- 从CA发送到PA的数据开始，加入id字段。为了CA作为客户端收到PA传回的数据后的到当时传出的id，就能找到CA作为服务端时接受该请求的通道，最后经过这个通道返回C。同理，PA中也需要这个id串下去。具体可以进入代码体会。

------

### 3 总结

这个初赛，搭建起测试环境还是比较麻烦的，锻炼了一下工程能力。

学习了Socket、NIO中Selector、Netty的使用，认识到了Reactor线程模型强大的功能，后续有时间的话我要学习一下Netty源码。

这是我从小白开始的第一个‘项目’