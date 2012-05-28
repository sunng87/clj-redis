(defproject org.clojars.sunng/clj-redis "0.0.13-SNAPSHOT"
  :description "Clojure Redis client library"
  :url "https://github.com/sunng87/clj-redis"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [redis.clients/jedis "1.5.2"]]
  :profile {"1.4" [[org.clojure/clojure "1.4.0"]]}
  :warn-on-reflection true)

