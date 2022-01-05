package cn.vko.eduorder.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Description 分布式锁
 *
 * @author zck
 * @date 2021/9/19
 */
@Component
public class RedisLock {

    @Autowired
    @Qualifier("jedisTemplate")
    private RedisTemplate<String, String> jedis;


    /**
     * Description 当前锁被占用,3次等待后返回false
     * @author zck
     * @date 2021/10/26
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
/*
        if (jedis.opsForValue().setIfAbsent(key, value)) {     //这个其实就是setnx命令，只不过在java这边稍有变化，返回的是boolea
            //避免死锁，且只让一个线程拿到锁
            String currentValue = jedis.opsForValue().get(key);
            //判断锁是否过期
            if (!StringUtils.isEmpty(currentValue) && Long.parseLong(currentValue) < System.currentTimeMillis()) {
                //获取上一个锁的时间
                String oldValues = jedis.opsForValue().getAndSet(key, value);

            *//*
               只会让一个线程拿到锁
               如果旧的value和currentValue相等，只会有一个线程达成条件，因为第二个线程拿到的oldValue已经和currentValue不一样了
             *//*
                if (!StringUtils.isEmpty(oldValues) && oldValues.equals(currentValue)) {
                    return true;
                }
            }
            return true;
        }*/
//        return false;
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

}
