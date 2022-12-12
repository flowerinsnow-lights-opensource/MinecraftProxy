# MinecraftProxy
Minecraft服务器代理，基于Java NIO实现，通常用于搭建加速IP

# 配置文件
名称为`config.xml`，位于jar文件目录下，格式为XML，每一条都写注释了，请前往阅读...

在程序启动时，如果没有配置文件，会自动复制一份默认的

# 特点
1. 本程序允许伪造访问IP，即在握手头部写为服务器真实IP，只需要将配置文件`configuration.connection.target.accept`上的`sendthis`属性修改为`true`即可，绕过hypixel等的检查
2. ping的时候也可以使用伪造访问IP，实现允许ping hypixel这种IP检查的服务器
3. 可以分别指定验证成功连接的目标服务器和验证失败连接的目标服务器，如果验证失败，你可以将其转发到自己的服务器上，显示给用户看你自己的自定义消息

# 开源协议
本项目使用<u>[GNU Affero General Public License](https://www.gnu.org/licenses/agpl-3.0.zh-cn.html)</u>协议开源。

也就是说，您可以免费且自由地使用、修改、复制与再次分发该软件，前提是
1. 您的任何改动必须开源，不论是网络服务还是二次分发
2. 您的修改与复制的相关内容必须使用相同许可证
3. 您的二创内容需要声明该项目的版权信息
4. 具体内容以<u>[GNU](https://www.gnu.org/)</u>的许可协议为准

同时，所有您使用过程中的一切损失，由您自己承担，原作者不承担任何责任

如果您有其他需要，可以在[Issues](issuess)中向我提出申请或其他内容

# 引用声明
[SaussureaUtils](https://github.com/flowerinsnowdh/SaussureaUtils) By [flowerinsnowdh](https://github.com/flowerinsnowdh) (BSD-3-Clause license)

本项目不涉及Minecraft源码