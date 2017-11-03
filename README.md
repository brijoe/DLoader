#ImageLoader

Android本地、网络图片加载库。

## 使用

下载[jar](jar/imageloader.jar)包，复制到项目的libs下，然后添加依赖。

任何需要的地方调用以下代码

```java
 ImageLoader.with(context).load(url, imageview);
```
注意权限：

```xml
 <uses-permission android:name="android.permission.INTERNET"></uses-permission>
 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"></uses-permission>

```

## 效果图

### 关于Sample

* Android Studio 用户，以module方式导入[sample/sample-imageloader-for-androidstudio](sample/sample-imageloader-for-androidstudio) 。
* Eclipse用户，以项目的方式，导入[sample/sample-imageloader-for-eclipse](sample/sample-imageloader-for-eclipse) 。

### 加载网络图片

<img src="imageloader_01.gif"  width="320px"/>

### 加载本地图片

<img src="imageloader_02.png"  width="320px"/>

<img src="imageloader_03.png"  width="320px"/>

## 特性

* 支持LIFO，FIFO 加载图片策略
* 支持内存缓存、硬盘缓存
* 支持网络图片加载
* 支持本地图片加载
* 支持file、drawable、assets、content等Schema图片的加载

##License
Copyright 2016 Bridge George

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

