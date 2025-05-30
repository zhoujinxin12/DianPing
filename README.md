仿大众点评项目

项目介绍：本项目是一款仿大众点评的生活服务平台，支持用户发布探店笔记、参与优惠券秒杀、订单支付及点赞、关注、收藏等功能。通过Redis缓存实现高并发秒杀，RabbitMQ消息队列保障异步下单与流量削峰，Spring状态机管理支付状态流转。采用Redisson分布式锁解决分布式事务问题，并结合Nginx负载均衡与服务集群设计高可用架构。延迟队列和补偿机制提升数据一致性，同时利用Redis数据结构优化热点查询、排行榜和签到等功能，全面提升系统性能和用户体验。

项目亮点：

使用Redis缓存解决分布式场景下的Session共享问题、提高商铺查询效率、采用旁路缓存策略保证数据一致性，解决缓存击穿和雪崩。

通过双拦截器设计，解决Token自动刷新问题。

对热点数据（抢购券、热门商铺）采用逻辑过期策略解决缓存击穿，和使用Lua脚本解决超卖问题。

实现基于Redis全局ID生成器，保证了ID的唯一性、高可用、高性能、递增性和安全性。

通过 RabbitMQ 实现异步下单，有效进行流量削峰填谷，并加快响应增加用户体验。

利用消息确认、Spring重试、死信队列和分布式锁，确保消息可靠性和业务幂等性。

利用延迟队列实现超时订单取消，结合Spring状态机保证业务幂等性和正确性。

使用AOP统一记录用户行为日志，配合死信队列实现异常情况告警和处理。

基于Redis SortedSet实现高效的用户点赞查询和点赞排行榜功能。

通过Feed推模式实现博客内容精准投喂，利用 SortedSet 支持滚动分页功能。

使用Redis GEO数据结构实现附近商铺查询，通过BitMap压缩用户签到数据量，提升存储效率。


