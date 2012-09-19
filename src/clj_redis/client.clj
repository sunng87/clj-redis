(ns clj-redis.client
  (:import java.net.URI)
  (:import (redis.clients.jedis Jedis JedisPool JedisPoolConfig JedisPubSub Pipeline Response))
  (:import (redis.clients.jedis BinaryClient$LIST_POSITION))
  (:require [clojure.string :as str])
  (:refer-clojure :exclude [get set keys type]))

(def ^{:private true} local-url
  "redis://127.0.0.1:6379")

(defn init
  ([] (init {}))
  ([{:keys [url host port timeout test-on-borrow db] :as opts}]
     (let [uri (URI. (or url local-url))
           tout (or timeout 2000)
           host (or host (.getHost uri))
           port (or port (if (pos? (.getPort uri)) (.getPort uri) 6379))
           uinfo (.getUserInfo uri)
           pass (and uinfo (last (str/split uinfo #":")))
           config (JedisPoolConfig.)
           db (or db 0)]
       (when test-on-borrow
         (.setTestOnBorrow config test-on-borrow))
       (JedisPool. config host port tout pass db)))
  ([k1 v1 & {:as opts}]
     (init (assoc opts k1 v1))))

(defn lease [^JedisPool p f]
  (let [j (.getResource p)]
    (try
      (f j)
      (finally
        (.returnResource p j)))))

(defn ping [p]
  (lease p (fn [^Jedis j] (.ping j))))

(defn flush-all [p]
  (lease p (fn [^Jedis j] (.flushAll j))))


;; Keys

(defn exists [p ^String k]
  (lease p (fn [^Jedis j] (.exists j k))))

(defn del [p ks]
  (lease p (fn [^Jedis j] (.del j ^"[Ljava.lang.String;" (into-array ks)))))

(defn keys [p & [^String pattern]]
  (lease p (fn [^Jedis j] (seq (.keys j (or pattern "*"))))))

(defn rename [p ^String k ^String nk]
  (lease p (fn [^Jedis j] (.rename j k nk))))

(defn renamenx [p ^String k ^String nk]
  (lease p (fn [^Jedis j] (.renamenx j k nk))))

(defn expire [p ^String k ^Integer s]
  (lease p (fn [^Jedis j] (.expire j k s))))

(defn expireat [p ^String k ^Long ut]
  (lease p (fn [^Jedis j] (.expireAt j k ut))))

(defn ttl [p ^String k]
  (lease p (fn [^Jedis j] (.ttl j k))))

(defn persist [p ^String k]
  (lease p (fn [^Jedis j] (.persist j k))))

(defn move [p ^String k ^Integer db]
  (lease p (fn [^Jedis j] (.move j k db))))

(defn type [p ^String k]
  (lease p (fn [^Jedis j] (.type j k))))


;; Strings

(defn incr [p ^String k]
  (lease p (fn [^Jedis j] (.incr j k))))

(defn incrby [p ^String k ^Long v]
  (lease p (fn [^Jedis j] (.incrBy j k v))))

(defn decr [p ^String k]
  (lease p (fn [^Jedis j] (.decr j k))))

(defn decrby [p ^String k ^Long v]
  (lease p (fn [^Jedis j] (.decrBy j k v))))

(defn get [p ^String k]
  (lease p (fn [^Jedis j] (.get j k))))

(defn set [p ^String k ^String v]
  (lease p (fn [^Jedis j] (.set j k v))))

(defn mget [p & keys]
  (lease p (fn [^Jedis j] (.mget j ^"[Ljava.lang.String;" (into-array keys)))))

(defn mset [p & keys]
  (lease p (fn [^Jedis j] (.mset j ^"[Ljava.lang.String;" (into-array keys)))))

(defn msetnx [p & keys]
  (lease p (fn [^Jedis j] (.msetnx j ^"[Ljava.lang.String;" (into-array keys)))))

(defn getset [p ^String k ^String v]
  (lease p (fn [^Jedis j] (.getSet j k v))))

(defn append [p ^String k ^String v]
  (lease p (fn [^Jedis j] (.append j k v))))

(defn getrange [p ^String k ^Integer start ^Integer end]
  (lease p (fn [^Jedis j] (.substr j k start end))))

(defn setrange [p ^String k ^Long offset ^String value]
  (lease p (fn [^Jedis j] (.setrange j k offset value))))

(defn setnx [p ^String k ^String v]
  (lease p (fn [^Jedis j] (.setnx j k v))))

(defn setex [p ^String k ^Integer s ^String v]
  (lease p (fn [^Jedis j] (.setex j k s v))))

(defn getbit [p ^String k ^Long offset]
  (lease p (fn [^Jedis j] (.getbit j k offset))))

(defn setbit [p ^String k ^Long offset ^Boolean value]
  (lease p (fn [^Jedis j] (.setbit j k offset value))))

(defn strlen [p ^String k]
  (lease p (fn [^Jedis j] (.strlen j k))))

; Lists

(defn lpush [p ^String k & v]
  (lease p (fn [^Jedis j] (.lpush j k ^"[Ljava.lang.String;" (into-array String v)))))

(defn rpush [p ^String k & v]
  (lease p (fn [^Jedis j] (.rpush j k ^"[Ljava.lang.String;" (into-array String v)))))

(defn lset [p ^String k ^Long i ^String v]
  (lease p (fn [^Jedis j] (.lset j k i v))))

(defn llen [p ^String k]
  (lease p (fn [^Jedis j] (.llen j k))))

(defn lindex [p ^String k ^Long i]
  (lease p (fn [^Jedis j] (.lindex j k i))))

(defn lpop [p ^String k]
  (lease p (fn [^Jedis j] (.lpop j k))))

(defn blpop [p ks ^Integer t]
  (lease p
   (fn [^Jedis j]
     (if-let [pair (.blpop j t ^"[Ljava.lang.String;" (into-array ks))]
       (seq pair)))))

(defn rpop [p ^String k]
  (lease p (fn [^Jedis j] (.rpop j k))))

(defn brpop [p ks ^Integer t]
  (lease p
    (fn [^Jedis j]
      (if-let [pair (.brpop j t ^"[Ljava.lang.String;" (into-array ks))]
        (seq pair)))))

(defn lrange
  [p ^String k ^Long start ^Long end]
  (lease p (fn [^Jedis j] (seq (.lrange j k start end)))))

(defn ltrim
  [p ^String k ^Long start ^Long end]
  (lease p (fn [^Jedis j] (.ltrim j k start end))))

(defn lrem
  [p ^String k ^Long c ^String v]
  (lease p (fn [^Jedis j] (.lrem j k c v))))

(defn brpoplpush [p ^String src ^String dest ^Integer t]
  (lease p (fn [^Jedis j] (.brpoplpush j src dest t))))

(defonce position {:after BinaryClient$LIST_POSITION/AFTER
                   :before BinaryClient$LIST_POSITION/BEFORE})

(defn linsert [p ^String k pos ^String pivot ^String v]
  (lease p (fn [^Jedis j] (.linsert j k ^BinaryClient$LIST_POSITION (position pos) pivot v))))

(defn lpushx [p ^String k ^String v]
  (lease p (fn [^Jedis j] (.lpushx j k v))))

(defn rpushx [p ^String k ^String v]
  (lease p (fn [^Jedis j] (.rpushx j k v))))

; Sets

(defn sadd [p ^String k & m]
  (lease p (fn [^Jedis j] (.sadd j k ^"[Ljava.lang.String;" (into-array String m)))))

(defn srem [p ^String k & m]
  (lease p (fn [^Jedis j] (.srem j k ^"[Ljava.lang.String;" (into-array String m)))))

(defn spop [p ^String k]
  (lease p (fn [^Jedis j] (.spop j k))))

(defn scard [p ^String k]
  (lease p (fn [^Jedis j] (.scard j k))))

(defn smembers [p ^String k]
  (lease p (fn [^Jedis j] (seq (.smembers j k)))))

(defn sismember [p ^String k ^String m]
  (lease p (fn [^Jedis j] (.sismember j k m))))

(defn srandmember [p ^String k]
  (lease p (fn [^Jedis j] (.srandmember j k))))

(defn smove [p ^String k ^String d ^String m]
  (lease p (fn [^Jedis j] (.smove j k d m))))

(defn sdiff [p & keys]
  (lease p (fn [^Jedis j] (.sdiff j ^"[Ljava.lang.String;" (into-array String keys)))))

(defn sdiffstore [p ^String dest & keys]
  (lease p (fn [^Jedis j] (.sdiffstore j dest ^"[Ljava.lang.String;" (into-array String keys)))))

(defn sinter [p & keys]
  (lease p (fn [^Jedis j] (.sinter j ^"[Ljava.lang.String;" (into-array String keys)))))

(defn sinterstore [p ^String d & keys]
  (lease p (fn [^Jedis j] (.sinterstore j d ^"[Ljava.lang.String;" (into-array String keys)))))

(defn sunion [p & keys]
  (lease p (fn [^Jedis j] (.sunion j ^"[Ljava.lang.String;" (into-array String keys)))))

(defn sunionstore [p ^String d & keys]
  (lease p (fn [^Jedis j] (.sunionstore j d ^"[Ljava.lang.String;" (into-array String keys)))))


; Sorted sets

(defn zadd [p ^String k ^Double r ^String m]
  (lease p (fn [^Jedis j] (.zadd j k r m))))

(defn zcount [p ^String k ^Double min ^Double max]
  (lease p (fn [^Jedis j] (.zcount j k min max))))

(defn zcard [p ^String k]
  (lease p (fn [^Jedis j] (.zcard j k))))

(defn zrank [p ^String k ^String m]
  (lease p (fn [^Jedis j] (.zrank j k m))))

(defn zrevrank [p ^String k ^String m]
  (lease p (fn [^Jedis j] (.zrevrank j k m))))

(defn zscore [p ^String k ^String m]
  (lease p (fn [^Jedis j] (.zscore j k m))))

(defn zrangebyscore
  ([p ^String k ^Double min ^Double max]
    (lease p (fn [^Jedis j] (seq (.zrangeByScore j k min max)))))
  ([p ^String k ^Double min ^Double max ^Integer offset ^Integer count]
    (lease p (fn [^Jedis j] (seq (.zrangeByScore j k min max offset count))))))

(defn zrangebyscore-withscore
  ([p ^String k ^Double min ^Double max]
    (lease p (fn [^Jedis j] (seq (.zrangeByScoreWithScores j k min max)))))
  ([p ^String k ^Double min ^Double max ^Integer offset ^Integer count]
    (lease p (fn [^Jedis j] (seq (.zrangeByScoreWithScores j k min max offset count))))))

(defn zrange [p ^String k ^Long start ^Long end]
  (lease p (fn [^Jedis j] (seq (.zrange j k start end)))))

(defn zrevrange [p ^String k ^Long start ^Long end]
  (lease p (fn [^Jedis j] (seq (.zrevrange j k start end)))))

(defn zincrby [p ^String k ^Double s ^String m]
  (lease p (fn [^Jedis j] (.zincrby j k s m))))

(defn zrem [p ^String k & m]
  (lease p (fn [^Jedis j] (.zrem j k ^"[Ljava.lang.String;" (into-array String m)))))

(defn zremrangebyrank [p ^String k ^Long start ^Long end]
  (lease p (fn [^Jedis j] (.zremrangeByRank j k start end))))

(defn zremrangebyscore [p ^String k ^Double start ^Double end]
  (lease p (fn [^Jedis j] (.zremrangeByScore j k start end))))

(defn zinterstore [p ^String d k]
  (lease p (fn [^Jedis j] (.zinterstore j d ^"[Ljava.lang.String;" (into-array k)))))

(defn zunionstore [p ^String d k]
  (lease p (fn [^Jedis j] (.zunionstore j d ^"[Ljava.lang.String;" (into-array k)))))

(defn zrevrangebyscore
  ([p ^String k ^Double max ^Double min]
     (lease p (fn [^Jedis j] (seq (.zrevrangeByScore j k max min)))))
  ([p ^String k ^Double max ^Double min ^Integer offset ^Integer count]
     (lease p (fn [^Jedis j] (seq (.zrevrangeByScore j k max min offset count))))))

(defn zrevrangebyscore-withscore
  ([p ^String k ^Double max ^Double min]
     (lease p (fn [^Jedis j] (seq (.zrevrangeByScoreWithScores j k max min)))))
  ([p ^String k ^Double max ^Double min ^Integer offset ^Integer count]
     (lease p (fn [^Jedis j] (seq (.zrevrangeByScoreWithScores j k max min offset count))))))

; Hashes

(defn hget [p ^String k ^String f]
  (lease p (fn [^Jedis j] (.hget j k f))))

(defn hmget [p ^String k & fs]
  (lease p (fn [^Jedis j] (seq (.hmget j k ^"[Ljava.lang.String;" (into-array fs))))))

(defn hset [p ^String k ^String f ^String v]
  (lease p (fn [^Jedis j] (.hset j k f v))))

(defn hmset [p ^String k ^java.util.Map h]
  (lease p (fn [^Jedis j] (.hmset j k h))))

(defn hsetnx [p ^String k ^String f ^String v]
  (lease p (fn [^Jedis j] (.hsetnx j k f v))))

(defn hincrby [p ^String k ^String f ^Long v]
  (lease p (fn [^Jedis j] (.hincrBy j k f v))))

(defn hexists [p ^String k ^String f]
  (lease p (fn [^Jedis j] (.hexists j k f))))

(defn hdel [p ^String k & f]
  (lease p (fn [^Jedis j] (.hdel j k ^"[Ljava.lang.String;" (into-array String f)))))

(defn hlen [p ^String k]
  (lease p (fn [^Jedis j] (.hlen j k))))

(defn hkeys [p ^String k]
  (lease p (fn [^Jedis j] (.hkeys j k))))

(defn hvals [p ^String k]
  (lease p (fn [^Jedis j]  (seq (.hvals j k)))))

(defn hgetall [p ^String k]
  (lease p (fn [^Jedis j] (.hgetAll j k))))


; Pub-Sub

(defn publish [p ^String c ^String m]
  (lease p (fn [^Jedis j] (.publish j c m))))

(defn subscribe [p chs handler]
  (let [pub-sub (proxy [JedisPubSub] []
                  (onSubscribe [ch cnt])
                  (onUnsubscribe [ch cnt])
                  (onMessage [ch msg] (handler ch msg)))]
    (lease p (fn [^Jedis j]
      (.subscribe j pub-sub ^"[Ljava.lang.String;" (into-array chs))))))

(defmacro with-pipeline [jedis-pool argvec & body]
  `(lease ~jedis-pool
          (fn [^Jedis j#]
            (let [pipeline# (.pipelined j#)
                  results# ((fn ~argvec ~@body) ^Pipeline pipeline#)]
              (.sync ^Pipeline pipeline#)
              (cond
               (instance? Response results#)
               (.get ^Response results#)
               
               (and (sequential? results#)
                    (every? #(instance? Response %) results#))
               (doall (map #(.get ^Response %) results#))
               
               :else results#)))))

