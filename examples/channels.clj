(ns channels
  (:require [conquerant.core :as c])
  (:import [java.util.concurrent Executors LinkedBlockingQueue TimeUnit]))

(defonce ^:private take-put-executor
  (Executors/newSingleThreadExecutor))

(defn- rand-wait-ms []
  (+ 3 (rand-int 2)))


(defn chan []
  (LinkedBlockingQueue.))

(defn take! [^LinkedBlockingQueue ch]
  (c/with-async-executor take-put-executor
    (c/async
     (if-let [x (.poll ch (rand-wait-ms) TimeUnit/MILLISECONDS)]
       x
       (take! ch)))))

(defn put! [^LinkedBlockingQueue ch x]
  (c/with-async-executor take-put-executor
    (c/async
     (when-not (.offer ch x (rand-wait-ms) TimeUnit/MILLISECONDS)
       (put! ch x)))))

(defn alts! [^LinkedBlockingQueue ch1 ^LinkedBlockingQueue ch2]
  (c/with-async-executor take-put-executor
    (c/async
     (if-let [x (.poll ch1 (rand-wait-ms) TimeUnit/MILLISECONDS)]
       [ch1 x]
       (alts! ch2 ch1)))))

(defn timeout! [ms]
  (let [c (chan)
        p (c/promise)]
    (c/async (let [_ (c/await p ms nil)]
               (put! c ::timeout)))
    c))


(comment

  ;; take! and put!
  ;; ==============
  (def c (chan))

  (dotimes [i 100]
    (c/async (let [x (c/await (take! c))]
               (println x))))

  (dotimes [i 100]
    (put! c :hi))


  ;; timeout!
  ;; ========
  @(take! (timeout! 100))


  ;; alts!
  ;; =====
  (def d (chan))

  (c/async
   (let [[ch x] (c/await (alts! d (timeout! 5000)))]
     (println x))))
