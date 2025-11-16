# 分布式多级缓存框架 - 项目说明书

## 一、项目概述

### 1.1 项目简介

`distribute-multi-level-cache` 是一个基于 Spring Boot 的分布式多级缓存框架，支持 **Caffeine（本地缓存）** 和 **Redis（分布式缓存）** 的组合使用。该框架提供了灵活的缓存策略、装饰器模式和缓存同步机制，能够有效提升应用性能并保证分布式环境下的缓存一致性。

### 1.2 核心特性

- ✅ **多级缓存支持**：支持 Caffeine、Redis 以及 Caffeine+Redis 多级缓存组合
- ✅ **缓存同步机制**：基于 Redis Pub/Sub 实现分布式缓存同步
- ✅ **装饰器模式**：支持缓存值重建、缓存同步等装饰器扩展
- ✅ **策略模式**：支持自定义缓存策略（CacheStrategy）
- ✅ **Spring Cache 集成**：完全兼容 Spring Cache 注解（@Cacheable、@CacheEvict 等）
- ✅ **自动配置**：基于 Spring Boot AutoConfiguration 实现零配置接入

### 1.3 技术栈

- **Spring Boot**: 2.4.0
- **Caffeine**: 2.8.5（本地缓存）
- **Redis**: 通过 Spring Data Redis + Lettuce 6.0.1
- **Jackson**: 2.11.3（序列化）

---

## 二、项目架构

### 2.1 模块结构

```
distribute-multi-level-cache/
├── infra-multi-level-cache/          # 核心模块
│   ├── config/                       # 配置类
│   ├── manager/                      # 缓存管理器
│   ├── decorators/                   # 装饰器
│   ├── strategys/                    # 缓存策略
│   ├── sync/                         # 缓存同步
│   └── util/                         # 工具类
└── infra-multi-level-cache-example/  # 示例模块
```

### 2.2 核心架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Cache API                     │
│              (@Cacheable, @CacheEvict)                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              CompositeCacheManager                      │
│  (统一管理 Caffeine、Redis、Caffeine+Redis)              │
└──────┬──────────────┬──────────────┬───────────────────┘
       │              │              │
       ▼              ▼              ▼
┌──────────┐   ┌──────────┐   ┌──────────────┐
│Caffeine  │   │  Redis   │   │Caffeine+Redis│
│CacheMgr  │   │CacheMgr  │   │CacheManager  │
└────┬─────┘   └────┬─────┘   └──────┬───────┘
     │              │                 │
     ▼              ▼                 ▼
┌─────────────────────────────────────────────┐
│         MultipleCache (责任链模式)          │
│  ┌──────────┐    ┌──────────┐              │
│  │Caffeine  │───▶│  Redis   │              │
│  │  Node    │    │  Node    │              │
│  └──────────┘    └──────────┘              │
└─────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────┐
│      CacheDecorationBuilder (装饰器模式)     │
│  ┌──────────────┐  ┌──────────────────┐    │
│  │CacheSync     │  │ValueRebuild      │    │
│  │Decorator     │  │Decorator         │    │
│  └──────────────┘  └──────────────────┘    │
└─────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────┐
│         CacheStrategy (策略模式)              │
│    (自定义缓存读写策略)                       │
└─────────────────────────────────────────────┘
```

---

## 三、核心组件详解

### 3.1 多级缓存核心类

#### 3.1.1 MultipleCache

**位置**: `com.peanut.infra.multilevelcache.MultipleCache`

**职责**: 实现 Spring Cache 接口，作为多级缓存的统一入口。

**关键方法**:
- `get(Object key)`: 从缓存中获取值
- `put(Object key, Object value)`: 写入缓存
- `evict(Object key)`: 删除缓存
- `clear()`: 清空缓存

**设计模式**: 外观模式（Facade Pattern）

#### 3.1.2 MultipleCacheNode

**位置**: `com.peanut.infra.multilevelcache.MultipleCacheNode`

**职责**: 多级缓存节点，实现责任链模式，支持链式缓存查询。

**核心逻辑**:
```java
// 查询逻辑：先查当前节点，未命中则查下一级，命中后回填当前节点
public ValueWrapper get(Object key) {
    ValueWrapper value = cache.get(key);
    if (null == value && hasNext()) {
        value = next.get(key);
        if (null != value) {
            cache.putIfAbsent(key, value.get());  // 回填
        }
    }
    return value;
}

// 写入逻辑：先写入下一级，再写入当前级（保证一致性）
public void put(Object key, Object value) {
    if (hasNext()) {
        next.put(key, value);
    }
    cache.put(key, value);
}
```

**设计模式**: 责任链模式（Chain of Responsibility）

### 3.2 缓存管理器

#### 3.2.1 CaffeineCacheManagerAdapter

**位置**: `com.peanut.infra.multilevelcache.manager.CaffeineCacheManagerAdapter`

**职责**: Caffeine 本地缓存管理器适配器，负责创建和管理 Caffeine 缓存实例。

**配置项**:
- `expireAfterAccess`: 访问后过期时间（秒）
- `expireAfterWrite`: 写入后过期时间（秒）
- `maximumSize`: 最大容量
- `initialCapacity`: 初始容量
- `enableSoftRef`: 是否启用软引用
- `disableSync`: 是否禁用同步（默认 true，即不开启同步）

#### 3.2.2 RedisCacheManagerAdapter

**位置**: `com.peanut.infra.multilevelcache.manager.RedisCacheManagerAdapter`

**职责**: Redis 分布式缓存管理器适配器，负责创建和管理 Redis 缓存实例。

**配置项**:
- `expire`: 过期时间（秒）
- `strategy`: 缓存策略 Bean 名称

#### 3.2.3 CaffeineRedisCacheManager

**位置**: `com.peanut.infra.multilevelcache.manager.CaffeineRedisCacheManager`

**职责**: 组合 Caffeine 和 Redis 的多级缓存管理器。

**工作流程**:
1. 创建 Caffeine 缓存管理器
2. 创建 Redis 缓存管理器
3. 使用 `MultipleCache.builder()` 构建多级缓存链：Caffeine → Redis
4. 应用装饰器（如值重建、同步等）

### 3.3 装饰器模式

#### 3.3.1 AbstractCacheDecorator

**位置**: `com.peanut.infra.multilevelcache.decorators.AbstractCacheDecorator`

**职责**: 抽象装饰器基类，所有装饰器都继承此类。

**设计模式**: 装饰器模式（Decorator Pattern）

#### 3.3.2 CacheSyncDecorator

**位置**: `com.peanut.infra.multilevelcache.decorators.CacheSyncDecorator`

**职责**: 缓存同步装饰器，实现分布式缓存同步。

**功能**:
- **发布事件**: 当缓存发生 `put`、`evict`、`clear` 操作时，发布同步事件
- **处理事件**: 接收其他节点的同步事件，更新本地缓存

**同步流程**:
```
节点A执行 put(key, value)
    ↓
CacheSyncDecorator 拦截
    ↓
发布 PutEvent 到 Redis Channel
    ↓
其他节点（节点B、C...）接收事件
    ↓
更新本地缓存
```

#### 3.3.3 CachedValueRebuildDecorator

**位置**: `com.peanut.infra.multilevelcache.decorators.CachedValueRebuildDecorator`

**职责**: 缓存值重建装饰器，支持从缓存中读取数据后进行值转换。

**使用场景**: 
- 缓存中存储的是 `User` 对象，读取时需要转换为 `VIPUser`
- 缓存中存储的是旧版本数据，需要升级为新版本

**接口**: `CachedValueRebuilder<K, V>`
```java
public interface CachedValueRebuilder<K, V> {
    V rebuild(K key, V value);
}
```

### 3.4 缓存同步机制

#### 3.4.1 CacheSyncManager

**位置**: `com.peanut.infra.multilevelcache.sync.CacheSyncManager`

**职责**: 缓存同步管理器接口。

**实现类**: `RedisCacheSyncManager`（基于 Redis Pub/Sub）

**核心方法**:
- `publish(CacheSyncEvent event)`: 发布同步事件
- `handle(CacheSyncEvent event)`: 处理同步事件
- `getChannelName()`: 获取同步通道名称（默认: `cache-sync`）

#### 3.4.2 CacheSyncEvent

**位置**: `com.peanut.infra.multilevelcache.sync.modle.CacheSyncEvent`

**职责**: 缓存同步事件模型。

**事件类型**:
- `PutEvent`: 缓存写入事件
- `EvictEvent`: 缓存删除事件
- `ClearEvent`: 缓存清空事件

**事件字段**:
- `host`: 发送事件的节点主机名
- `cacheName`: 缓存名称
- `key`: 缓存键
- `value`: 缓存值（仅 PutEvent 有）

#### 3.4.3 CacheSyncMessageListener

**位置**: `com.peanut.infra.multilevelcache.sync.CacheSyncMessageListener`

**职责**: Redis 消息监听器，监听 `cache-sync` 通道的同步事件。

### 3.5 缓存策略

#### 3.5.1 CacheStrategy

**位置**: `com.peanut.infra.multilevelcache.strategys.CacheStrategy`

**职责**: 缓存策略接口，定义缓存的基本操作。

**核心方法**:
- `doGet(NC nativeCache, K key)`: 获取缓存
- `doPut(NC nativeCache, K key, V value)`: 写入缓存
- `doEvict(NC nativeCache, K key)`: 删除缓存
- `doClear(NC nativeCache)`: 清空缓存

**实现类**:
- `AbstractCaffeineCacheStrategy`: Caffeine 缓存策略基类
- `AbstractRedisCacheStrategy`: Redis 缓存策略基类
- `DefaultRedisCacheStrategy`: Redis 默认策略

**使用场景**: 自定义缓存序列化、压缩、加密等逻辑。

### 3.6 自动配置

#### 3.6.1 CacheManagerAutoConfiguration

**位置**: `com.peanut.infra.multilevelcache.config.CacheManagerAutoConfiguration`

**职责**: Spring Boot 自动配置类，负责创建所有缓存相关的 Bean。

**配置的 Bean**:
1. `RedisTemplate<String, Object>`: Redis 操作模板
2. `StringRedisTemplate`: 字符串 Redis 模板
3. `TaskExecutor syncCacheTaskExecutor`: 缓存同步线程池
4. `RedisMessageListenerContainer`: Redis 消息监听容器
5. `CacheSyncManager`: 缓存同步管理器
6. `CacheSyncMessageListener`: 缓存同步消息监听器
7. `CompositeCacheManager`: 组合缓存管理器（最终暴露给 Spring）

**配置流程**:
```
读取 application.properties 配置
    ↓
根据配置创建对应的 CacheManager
    ├── Caffeine CacheManager (如果配置了 caffeine)
    ├── Redis CacheManager (如果配置了 redis)
    └── Caffeine+Redis CacheManager (如果配置了 multiple)
    ↓
组合到 CompositeCacheManager
    ↓
注册到 Spring 容器
```

#### 3.6.2 CacheConfigProperties

**位置**: `com.peanut.infra.multilevelcache.config.CacheConfigProperties`

**职责**: 缓存配置属性类，绑定 `multiple-cache.*` 配置项。

**配置结构**:
```properties
# Caffeine 缓存配置
multiple-cache.caffeine[0].name=caffeine-demo
multiple-cache.caffeine[0].expireAfterAccess=30
multiple-cache.caffeine[0].maximumSize=1000

# Redis 缓存配置
multiple-cache.redis[0].name=redis-demo
multiple-cache.redis[0].expire=100

# 多级缓存配置
multiple-cache.multiple[0].name=testCache
multiple-cache.multiple[0].caffeine.expireAfterAccess=30
multiple-cache.multiple[0].redis.expire=100
```

---

## 四、设计模式应用

### 4.1 责任链模式（Chain of Responsibility）

**应用位置**: `MultipleCacheNode`

**实现**: 通过 `next` 指针形成链表，查询时依次遍历各级缓存。

**优势**: 
- 解耦缓存层级关系
- 易于扩展新的缓存层级

### 4.2 装饰器模式（Decorator Pattern）

**应用位置**: `AbstractCacheDecorator` 及其子类

**实现**: 
- `CacheSyncDecorator`: 添加同步功能
- `CachedValueRebuildDecorator`: 添加值重建功能

**优势**:
- 动态扩展缓存功能
- 功能组合灵活

### 4.3 策略模式（Strategy Pattern）

**应用位置**: `CacheStrategy` 接口及其实现

**实现**: 通过配置不同的 `CacheStrategy` Bean，实现不同的缓存操作逻辑。

**优势**:
- 支持自定义缓存策略
- 易于替换和扩展

### 4.4 适配器模式（Adapter Pattern）

**应用位置**: `CaffeineCacheManagerAdapter`、`RedisCacheManagerAdapter`

**实现**: 将 Caffeine 和 Redis 适配为 Spring Cache 的 `CacheManager`。

**优势**:
- 统一缓存接口
- 兼容 Spring Cache 生态

### 4.5 建造者模式（Builder Pattern）

**应用位置**: `MultipleCache.MultipleCacheBuilder`、`CacheDecorationBuilder`

**实现**: 通过链式调用构建复杂的缓存对象。

**优势**:
- 代码可读性强
- 构建过程清晰

---

## 五、使用指南

### 5.1 依赖引入

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.peanut.infra</groupId>
    <artifactId>infra-multi-level-cache</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 5.2 配置文件

在 `application.properties` 中配置：

```properties
# Redis 连接配置
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.lettuce.pool.max-active=8

# 多级缓存配置（Caffeine + Redis）
multiple-cache.multiple[0].name=testCache
multiple-cache.multiple[0].caffeine.expireAfterAccess=30
multiple-cache.multiple[0].caffeine.initialCapacity=100
multiple-cache.multiple[0].caffeine.maximumSize=1000
multiple-cache.multiple[0].caffeine.disableSync=false  # 开启同步
multiple-cache.multiple[0].redis.expire=100

# 可选：配置装饰器
multiple-cache.multiple[0].decorators=demoCachedValueRebuilder
```

### 5.3 代码使用

#### 5.3.1 使用 Spring Cache 注解

```java
@Service
public class DemoService {
    
    @Cacheable(cacheNames = "testCache", key = "#id")
    public List<User> cacheTest(String id) {
        // 业务逻辑
        User user = new User();
        user.setAge(22);
        user.setName("xxx");
        return Arrays.asList(user);
    }
    
    @CacheEvict(cacheNames = "testCache", key = "#id")
    public void evictCache(String id) {
        // 删除缓存
    }
}
```

#### 5.3.2 自定义缓存值重建器

```java
@Component
public class DemoCachedValueRebuilder implements CachedValueRebuilder<String, Object> {
    
    @Override
    public Object rebuild(String key, Object value) {
        if (value instanceof User) {
            VIPUser vipUser = new VIPUser();
            BeanUtils.copyProperties(value, vipUser);
            vipUser.setLevel(1);
            return vipUser;
        }
        return value;
    }
}
```

#### 5.3.3 自定义缓存策略

```java
@Component("customRedisCacheStrategy")
public class CustomRedisCacheStrategy extends AbstractRedisCacheStrategy {
    
    public CustomRedisCacheStrategy(String cacheName) {
        super(cacheName);
    }
    
    @Override
    public Object doGet(Object nativeCache, Object key) {
        // 自定义获取逻辑
        return super.doGet(nativeCache, key);
    }
    
    @Override
    public boolean doPut(Object nativeCache, Object key, Object value) {
        // 自定义写入逻辑
        return super.doPut(nativeCache, key, value);
    }
}
```

然后在配置中引用：
```properties
multiple-cache.redis[0].strategy=customRedisCacheStrategy
```

### 5.4 缓存同步机制

**工作原理**:
1. 节点 A 执行 `cache.put(key, value)`
2. `CacheSyncDecorator` 拦截操作，发布 `PutEvent` 到 Redis Channel
3. 其他节点（B、C...）的 `CacheSyncMessageListener` 接收事件
4. 各节点更新本地缓存

**注意事项**:
- 只有 Caffeine 缓存需要同步（`disableSync=false`）
- Redis 缓存天然共享，无需同步
- 同步事件包含 `host` 字段，可避免自己处理自己发布的事件（但当前实现未做此过滤）

---

## 六、配置说明

### 6.1 Caffeine 缓存配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `name` | String | - | 缓存名称（必填） |
| `expireAfterAccess` | long | 3600 | 访问后过期时间（秒） |
| `expireAfterWrite` | long | 0 | 写入后过期时间（秒） |
| `maximumSize` | int | 200 | 最大容量 |
| `initialCapacity` | int | 10 | 初始容量 |
| `cacheLoader` | String | - | 缓存加载器 Bean 名称 |
| `decorators` | String | - | 装饰器 Bean 名称（逗号分隔） |
| `disableSync` | boolean | true | 是否禁用同步 |
| `enableSoftRef` | boolean | false | 是否启用软引用 |
| `strategy` | String | - | 缓存策略 Bean 名称 |

### 6.2 Redis 缓存配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `name` | String | - | 缓存名称（必填） |
| `expire` | long | - | 过期时间（秒，必填） |
| `strategy` | String | - | 缓存策略 Bean 名称 |
| `decorators` | String | - | 装饰器 Bean 名称（逗号分隔） |

### 6.3 多级缓存配置

| 配置项 | 类型 | 说明 |
|--------|------|------|
| `name` | String | 缓存名称（必填） |
| `caffeine` | Object | Caffeine 配置（参考 6.1） |
| `redis` | Object | Redis 配置（参考 6.2） |
| `decorators` | String | 装饰器 Bean 名称（逗号分隔） |

---

## 七、工作流程

### 7.1 缓存查询流程（多级缓存）

```
用户调用 @Cacheable 方法
    ↓
CompositeCacheManager.getCache("testCache")
    ↓
CaffeineRedisCacheManager.getCache("testCache")
    ↓
MultipleCache.get(key)
    ↓
MultipleCacheNode (Caffeine).get(key)
    ├── 命中 → 返回
    └── 未命中 → MultipleCacheNode (Redis).get(key)
                    ├── 命中 → 回填 Caffeine → 返回
                    └── 未命中 → 执行 valueLoader → 写入 Redis → 写入 Caffeine → 返回
```

### 7.2 缓存写入流程（多级缓存）

```
用户调用 cache.put(key, value)
    ↓
MultipleCache.put(key, value)
    ↓
MultipleCacheNode (Caffeine).put(key, value)
    ↓
先写入下一级: MultipleCacheNode (Redis).put(key, value)
    ↓
再写入当前级: Caffeine.put(key, value)
    ↓
CacheSyncDecorator 拦截
    ↓
发布 PutEvent 到 Redis Channel
    ↓
其他节点接收并更新本地缓存
```

### 7.3 缓存同步流程

```
节点A: cache.put(key, value)
    ↓
CacheSyncDecorator.put()
    ├── super.put() → 写入本地缓存
    └── cacheSyncManager.publish(new PutEvent(...))
            ↓
        RedisCacheSyncManager.publish()
            ↓
        redisTemplate.convertAndSend("cache-sync", event)
            ↓
节点B、C...: RedisMessageListenerContainer 接收消息
    ↓
CacheSyncMessageListener.onMessage()
    ↓
AbstractCacheSyncManager.handle(event)
    ↓
查找对应的 CacheSyncEventHandler
    ↓
CacheSyncDecorator.handlePut(event)
    ↓
更新本地缓存
```

---

## 八、扩展点

### 8.1 自定义缓存策略

实现 `AbstractCaffeineCacheStrategy` 或 `AbstractRedisCacheStrategy`：

```java
@Component("myCacheStrategy")
public class MyCacheStrategy extends AbstractCaffeineCacheStrategy {
    // 实现自定义逻辑
}
```

### 8.2 自定义缓存值重建器

实现 `CachedValueRebuilder` 接口：

```java
@Component
public class MyValueRebuilder implements CachedValueRebuilder<String, Object> {
    @Override
    public Object rebuild(String key, Object value) {
        // 自定义重建逻辑
        return value;
    }
}
```

### 8.3 自定义缓存同步管理器

实现 `CacheSyncManager` 接口：

```java
@Component
public class MyCacheSyncManager implements CacheSyncManager {
    // 实现自定义同步逻辑（如使用 RabbitMQ、Kafka 等）
}
```

### 8.4 自定义装饰器

继承 `AbstractCacheDecorator`：

```java
public class MyCacheDecorator extends AbstractCacheDecorator<Cache> {
    public MyCacheDecorator(Cache target) {
        super(target);
    }
    
    @Override
    public ValueWrapper get(Object key) {
        // 自定义逻辑
        return super.get(key);
    }
}
```

---

## 九、注意事项

### 9.1 缓存一致性

- **多级缓存一致性**: 写入时先写下一级，再写当前级，保证数据一致性
- **分布式一致性**: 通过 Redis Pub/Sub 实现，但存在网络延迟，非强一致性
- **建议**: 对一致性要求高的场景，考虑使用 Redis 作为唯一缓存源

### 9.2 缓存穿透/击穿/雪崩

- **穿透**: 框架未提供默认防护，建议在业务层或通过自定义策略实现
- **击穿**: 可通过 Caffeine 的 `CacheLoader` 实现单机互斥
- **雪崩**: 建议设置随机过期时间

### 9.3 性能优化

- **Caffeine 配置**: 根据业务场景调整 `maximumSize` 和过期时间
- **Redis 连接池**: 合理配置 `spring.redis.lettuce.pool.*`
- **同步线程池**: 可通过 `syncCacheTaskExecutor` Bean 自定义

### 9.4 序列化

- 框架使用 Jackson 进行序列化
- 确保缓存对象可序列化（实现 `Serializable` 或使用 Jackson 注解）

---

## 十、常见问题

### Q1: 如何只使用 Caffeine 缓存？

**A**: 只配置 `multiple-cache.caffeine`，不配置 `multiple-cache.redis` 和 `multiple-cache.multiple`。

### Q2: 如何只使用 Redis 缓存？

**A**: 只配置 `multiple-cache.redis`，不配置其他缓存类型。

### Q3: 缓存同步不生效？

**A**: 检查以下几点：
1. `disableSync` 是否设置为 `false`
2. Redis 连接是否正常
3. `CacheSyncMessageListener` 是否正常注册
4. 查看日志是否有同步消息发送/接收记录

### Q4: 如何禁用某个缓存的同步？

**A**: 在 Caffeine 配置中设置 `disableSync=true`。

### Q5: 自定义策略不生效？

**A**: 确保：
1. 策略类已注册为 Spring Bean
2. Bean 名称与配置中的 `strategy` 一致（首字母小写）
3. 策略类实现了对应的抽象策略类（`AbstractCaffeineCacheStrategy` 或 `AbstractRedisCacheStrategy`）

---

## 十一、项目结构说明

### 11.1 核心模块（infra-multi-level-cache）

```
com.peanut.infra.multilevelcache/
├── MultipleCache.java                    # 多级缓存入口
├── MultipleCacheNode.java                # 多级缓存节点（责任链）
├── CacheDecorationHandler.java           # 装饰器处理器接口
├── CachedValueRebuilder.java             # 值重建器接口
├── config/                                # 配置包
│   ├── CacheConfigProperties.java        # 配置属性类
│   └── CacheManagerAutoConfiguration.java # 自动配置类
├── manager/                               # 缓存管理器包
│   ├── CaffeineCacheManagerAdapter.java  # Caffeine 管理器
│   ├── RedisCacheManagerAdapter.java     # Redis 管理器
│   ├── CaffeineRedisCacheManager.java    # 多级缓存管理器
│   └── RedisCacheManagerProxy.java      # Redis 管理器代理
├── decorators/                            # 装饰器包
│   ├── AbstractCacheDecorator.java       # 抽象装饰器
│   ├── CacheDecorationBuilder.java       # 装饰器建造者
│   ├── CacheSyncDecorator.java           # 同步装饰器
│   └── CachedValueRebuildDecorator.java  # 值重建装饰器
├── strategys/                             # 策略包
│   ├── CacheStrategy.java                 # 策略接口
│   ├── AbstractCaffeineCacheStrategy.java # Caffeine 策略基类
│   ├── AbstractRedisCacheStrategy.java    # Redis 策略基类
│   └── impl/
│       └── DefaultRedisCacheStrategy.java # Redis 默认策略
├── sync/                                  # 同步包
│   ├── CacheSyncManager.java              # 同步管理器接口
│   ├── AbstractCacheSyncManager.java      # 同步管理器基类
│   ├── RedisCacheSyncManager.java         # Redis 同步管理器
│   ├── CacheSyncMessageListener.java      # 消息监听器
│   ├── CacheSyncEventHandler.java         # 事件处理器接口
│   └── modle/                             # 事件模型
│       ├── CacheSyncEvent.java            # 同步事件基类
│       ├── PutEvent.java                  # 写入事件
│       ├── EvictEvent.java                # 删除事件
│       └── ClearEvent.java                # 清空事件
├── constans/                              # 常量包
│   ├── CacheConstants.java                # 缓存常量
│   └── CacheType.java                     # 缓存类型枚举
└── util/                                  # 工具包
    └── HostUtil.java                      # 主机工具类
```

### 11.2 示例模块（infra-multi-level-cache-example）

```
com.peanut.infra.multilevelcache.example/
├── MultiLevelCacheExampleApplication.java # 启动类
├── DemoCachedValueRebuilder.java         # 值重建器示例
├── controller/
│   └── DemoController.java                # 控制器示例
├── service/
│   └── DemoService.java                   # 服务示例
├── User.java                              # 用户实体
└── VIPUser.java                           # VIP 用户实体
```

---

## 十二、总结

### 12.1 项目优势

1. **架构清晰**: 采用多种设计模式，代码结构清晰，易于理解和扩展
2. **功能完善**: 支持多级缓存、同步、装饰器等完整功能
3. **易于集成**: 基于 Spring Boot AutoConfiguration，零配置接入
4. **扩展性强**: 提供丰富的扩展点，支持自定义策略、装饰器等

### 12.2 适用场景

- ✅ 高并发读多写少的场景
- ✅ 需要本地缓存 + 分布式缓存的场景
- ✅ 对缓存一致性要求不是特别严格的场景
- ✅ 需要缓存值转换/重建的场景

### 12.3 改进建议

1. **缓存同步优化**: 添加事件去重和幂等性处理，避免自己处理自己发布的事件
2. **缓存穿透防护**: 提供默认的空值缓存机制
3. **监控指标**: 集成 Micrometer，提供缓存命中率等指标
4. **配置验证**: 增强配置校验，提前发现配置错误
5. **文档完善**: 补充更多使用示例和最佳实践

---

**文档版本**: v1.0  
**最后更新**: 2024年  
**作者**: peanut
