[![](https://jitpack.io/v/brijoe/DLoader.svg)](https://jitpack.io/#brijoe/DLoader)

# DLoader

简易的Android网络图片加载库，适用于较小规模的网络图片加载需求，仅供学习和研究。

## 特性

* 支持LIFO，FIFO 加载图片策略
* 支持内存缓存、硬盘缓存
* 支持网络图片加载


## 使用


在Project 级别的build.gradle 文件中引入以下配置：

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

在你项目的module级别的 build.gradle文件中引入

```
dependencies {
		 ...
	   implementation 'com.github.brijoe:DLoader:1.0.0'
	    ...
	}

```

任何需要的地方调用以下代码

```
 DLoader.with(context).load(url, imageview);
```
注意权限：

```
 <uses-permission android:name="android.permission.INTERNET" />
 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

```

## Demo 效果

<img src="images/screenshot.jpg" width=250/>


## License
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

