# SBS Async
## 解释
一个异步执行框架，用于点对点控制异步执行。基于异步回调使用，适配场景Android startActivityForResult。
## 优点
将指定任务的判定和执行集成到一起，可以通过外部的Controller控制指定任务的判定，具体使用实例参见提供的example文件。
同时支持外部异步控制和内部回调异步控制。

## 注意
此框架专注于在异步执行环境中组织不同的任务达成一个顺序流，并不能使得同步环境转变为异步环境。
更不是为了将同步环境转化为异步环境而生。