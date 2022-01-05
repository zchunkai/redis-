# redis实现分布式锁
    /**
     * 加锁
     * @param key   商品id
     * @param value 超时时间
     * @return
     */
    public boolean lock(String key, String value) {
        return jedis.execute(new SessionCallback<Boolean>() {
            List<Object> exec = null;
            @Override
            @SuppressWarnings({"unchecked"}) //抑制警告
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                jedis.opsForValue().setIfAbsent(key,value);
                jedis.expire(key, Long.parseLong(value),TimeUnit.MILLISECONDS);
                exec = operations.exec();
                if(exec.size() > 0) {
                    return (Boolean) exec.get(0);
                }
                return false;
            }
        });
    }
