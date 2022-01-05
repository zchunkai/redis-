# redis实现分布式锁


    @Autowired
    @Qualifier("jedisTemplate")
    private RedisTemplate<String, String> jedis;

    /**
     * 加锁
     * @param key   唯一标志
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
    /**
     * 解锁
     * @param key
     * @param value
     */
    public void unlock(String key, String value) {
        try {
            String currentValue = jedis.opsForValue().get(key);
            if (!StringUtils.isEmpty(currentValue) && currentValue.equals(value)) {
                jedis.opsForValue().getOperations().delete(key);
            }

        } catch (Exception e) {
           e.printStackTrace();
            System.out.println("redis分布式锁解锁出现异常!");
        }
    }
    
     /**
     * Description 判断当前锁是否使用,如果正在使用则1秒后重试,3次后返回
     * @date 2021/10/26
     * @param key
     * @param value
     */
    public boolean expire(String key, String value){
        if (lock(key,value)){
            return true;
        }
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(1000L);
                if (lock(key,value)){
                    return true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    
