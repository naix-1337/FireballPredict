# MyMod — 项目索引

> Minecraft Forge 1.8.9 模组项目 | 生成于 2026-07-12

---

## 一、项目概览

| 项目 | 详情 |
|------|------|
| **类型** | Minecraft Forge Mod (1.8.9) |
| **Mod ID** | `naix_test` |
| **版本** | `1.0` |
| **包名** | `com.naix.naix_test` |
| **Forge** | 1.8.9-11.15.1.2318-1.8.9 |
| **MCP 映射** | `stable_20` (设计给 MC 1.8.8，可安全忽略警告) |
| **Gradle** | 4.10.3 (Wrapper, 已从 2.7 升级) |
| **Java** | JDK 8 (Corretto 8u452 @ `D:\Java\jdk8`) |
| **IDE** | IntelliJ IDEA（已配置运行） |

---

## 二、目录结构

```
MyMod/
├── build.gradle              # Gradle 构建脚本 (ForgeGradle 2.1)
├── gradle.properties         # Gradle 全局属性 (Java Home 等)
├── gradlew / gradlew.bat     # Gradle Wrapper (已修改 → 强制 JDK 8)
├── settings.gradle           # (不存在，使用默认)
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties   # → gradle-2.7-bin.zip
│
├── src/
│   └── main/
│       ├── java/com/example/examplemod/
│       │   └── ExampleMod.java        # 主 Mod 类 (唯一源码)
│       └── resources/
│           └── mcmod.info             # Forge Mod 元数据
│
├── .idea/                    # IntelliJ IDEA 项目配置
│   ├── gradle.xml            #   Gradle JVM → corretto-1.8
│   ├── misc.xml              #   项目 SDK → corretto-1.8, Java 8
│   └── workspace.xml         #   RunManager: Minecraft Client + Server
│
├── MyMod.iml                 # IDEA 模块文件
├── MyMod.ipr                 # IDEA 项目文件 (旧格式)
├── MyMod.iws                 # IDEA 工作区文件 (旧格式)
│
├── build/                    # 构建输出 (含反编译的 MC 源码)
├── run/                      # Minecraft 运行目录
├── eclipse/                  # (空) Eclipse 配置目录
│
├── README.txt                # 官方 Forge 安装说明
├── PROJECT_INDEX.md          # ← 本文件
├── .gitignore
└── .claude/settings.local.json
```

---

## 三、关键文件详解

### 3.1 构建配置

#### `build.gradle` (83行)
- **插件**: `net.minecraftforge.gradle.forge` v2.1-SNAPSHOT
- **Minecraft**: `version = "1.8.9-11.15.1.2318-1.8.9"`, `mappings = "stable_20"`
- **仓库**: `jcenter()` + `files.minecraftforge.net/maven`
- **版本**: group=`com.yourname.modid`, archivesBaseName=`modid`, version=`1.0`
- **关键任务**:
  - `setupDecompWorkspace` — 反编译 MC 源码 + 下载全量依赖
  - `build` — 编译打包 mod jar → `build/libs/modid-1.0.jar`
  - `runClient` — 启动 Minecraft 客户端
  - `runServer` — 启动 Minecraft 服务端
  - `genIntellijRuns` — 生成 IDEA 运行配置

#### `gradle.properties` (1行)
```properties
org.gradle.java.home=D:/Java/jdk8
```

#### `gradle/wrapper/gradle-wrapper.properties`
```
distributionUrl=https://services.gradle.org/distributions/gradle-2.7-bin.zip
```

### 3.2 源码

#### `src/main/java/com/example/examplemod/ExampleMod.java` (21行)
- `@Mod(modid = "examplemod", version = "1.0")` — Forge Mod 注解
- `@EventHandler public void init(FMLInitializationEvent)` — 初始化入口
- 当前功能：打印泥土方块的本地化名（示例代码）

#### `src/main/resources/mcmod.info` (16行 JSON)
- modid: `examplemod`, name: `Example Mod`
- 变量 `${version}` / `${mcversion}` 在构建时由 `build.gradle:processResources` 替换

### 3.3 Wrapper 脚本 (已修改)

`gradlew` + `gradlew.bat` 均在开头添加了：
- **gradlew** (bash): `JAVA_HOME="/d/Java/jdk8"`
- **gradlew.bat** (cmd): `set JAVA_HOME=D:\Java\jdk8`

**修改原因**: 系统默认 Java 为 JDK 21，Gradle 2.7 无法解析其版本号。

### 3.4 IDE 配置 (.idea/)

| 文件 | 关键内容 |
|------|---------|
| `gradle.xml` | Gradle JVM = `corretto-1.8` |
| `misc.xml` | Project JDK = `corretto-1.8`, languageLevel = `JDK_1_8` |
| `workspace.xml` | RunManager: Minecraft Client (`GradleStart`) + Minecraft Server (`GradleStartServer`) |

---

## 四、常用 Gradle 命令

```bash
# 构建
./gradlew build                      # 编译打包 → build/libs/modid-1.0.jar
./gradlew setupDecompWorkspace       # 初始化开发环境（首次运行）
./gradlew clean                      # 清理构建产物

# 运行
./gradlew runClient                  # 启动 Minecraft 客户端
./gradlew runServer                  # 启动 Minecraft 服务端

# IDE
./gradlew idea                       # 生成 IDEA 项目文件
./gradlew genIntellijRuns            # 生成 IDEA 运行配置
./gradlew eclipse                    # 生成 Eclipse 项目文件

# 调试
./gradlew --refresh-dependencies     # 强制刷新依赖缓存
./gradlew tasks --all                # 列出所有可用任务
./gradlew --stop                     # 停止 Gradle Daemon
```

---

## 五、环境依赖

| 依赖 | 路径 / 版本 |
|------|------------|
| **JDK 8** | `D:\Java\jdk8` (Amazon Corretto 8.0.452) |
| **JDK 21** | `D:\Java\jdk21` (Oracle 21.0.7, 非本项目使用) |
| **JRE 8** | `D:\Java\jre8u451` (备用) |
| **Gradle** | 2.7 (Wrapper 自动下载) |
| **ForgeGradle** | 2.1-SNAPSHOT (自动下载到 `~/.gradle/caches/`) |
| **Minecraft** | 1.8.9 + Forge 11.15.1.2318 |

---

## 六、已知问题与修复记录

### #1 Java 版本不兼容
- **现象**: `Could not determine java version from '21.0.7'`
- **原因**: Gradle 2.7 (2015) 无法解析 Java 21 版本号
- **修复**: 修改 `gradlew` / `gradlew.bat` 强制 `JAVA_HOME=D:\Java\jdk8`；添加 `gradle.properties`

### #2 genIntellijRuns NPE
- **现象**: `NullPointerException at Constants.addXml(289)` → RunManager 配置未生成
- **原因**: ForgeGradle 2.1 bug — workspace.xml 缺少 `<component name="RunManager">`
- **修复**: 手动在 `workspace.xml` 中添加 RunManager 节点，然后重新运行

---

## 七、下一步参考

- 修改 Mod 主类: `src/main/java/com/example/examplemod/ExampleMod.java`
- 修改 Mod 元数据: `src/main/resources/mcmod.info`
- 修改模组 ID/包名: 需同步修改 `build.gradle` + `ExampleMod.java` + `mcmod.info`
- 添加依赖库: 编辑 `build.gradle` → `dependencies {}` 块
- IDEA 中启动测试: Run Configurations → Minecraft Client
