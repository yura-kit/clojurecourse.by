(ns csvdb.core
  (:require [clojure.core.match :refer [match]])
  )

;;Параллельное и конкурентное программирование

;;Параллельное выполнение кода основаны на features

;Встроенные функции:
;pmap – параллельный аналог map
;pcalls – параллельное вычисление функций
;pvalues – параллельное вычисление блоков кода


(defn long-job [n]
  (Thread/sleep 3000)
  (+ n 10))


(time (doall (map long-job (range 4))));; doall для форсирование вычисления резалтов


(time (doall (pmap long-job (range 4))))

(time (doall (pvalues
              (do (Thread/sleep 3000) 1);; выполняются паралельно друг другу
              (do (Thread/sleep 3000) 2)
              (do (Thread/sleep 3000) 3))))

;;Futures

;Вычисляются в отдельном потоке паралельно с овновным кодом
;!  Результат кешируется
;!  Доступ через deref или @
;!  Блокировка если результата еще нет
;!  Возможность отмены выполнения

(def future-test
  (future (do (Thread/sleep 10000)
            :finished)))

@future-test ;; будет ждать результата

@future-test ;; сразу вернет значение

;; Delays

;;   Откладывает выполнение кода до доступа к результату

(defn use-delays [x]
  {:result (delay (println "Evaluating
result..." x) x)
   :some-info true})

(def a (use-delays 10))

a

@(:result a) ;; выполняется весь код delay

@(:result a) ;; возвращается только результат

(:result a)

;Promises
; передача данных другому потоку
;Координация между потоками выполнения
;Блокируется при доступе к еще не отправленным данным
;Результат кешируется

(def p (promise))

(do (future
      (Thread/sleep 5000)
      (deliver p :fred))
  @p)

; Блокировки

;locking обеспечивает блокировку доступа к объекту

(defn add-to-map [h k v]
  (locking h ;; macros loking 2 params object for lock + operation
    (.put h k v)))

(def h (java.util.HashMap.))
(add-to-map h "test" "value")!
h

;; Средства JVM: потоки и т.п
; !   Легкость вызова кода Java
;!   Функции без аргументов реализуют интерфейсы Runnable & Callable

(.run (Thread.
               #(println "Hello world!")))


;;Advanced Topics

;Reducers
;!  Введены в Clojure 1.5
;!  Не создают промежуточных коллекций
;!  Используют fork/join при выполнении fold
;!  Свои версии функций map, fold, filter, и т.п.
;!  Ресурсы:
;!     http://clojure.com/blog/2012/05/08/reducers-a-library-and-
;model-for-collection-processing.html
;!     http://clojure.com/blog/2012/05/15/anatomy-of-reducer.html !     http://adambard.com/blog/clojure-reducers-for-mortals/
;!     http://www.infoq.com/presentations/Clojure-Reducers

(require '[clojure.core.reducers :as r])
(use 'criterium.core)

(bench (reduce + (map inc v)))
(bench (r/reduce + (r/map inc v)))
(bench (r/fold + (r/map inc v)));;better

;; core.async

;Асинхронное программирование с помощью каналов
;!  Подобно goroutines в Go
;!  Поддерживает Clojure & ClojureScript
;!  Ресурсы:
;!     http://clojure.com/blog/2013/06/28/clojure-core-async-channels.html
;!     http://stuartsierra.com/2013/12/08/parallel-processing-with-core-async
;!     http://swannodette.github.io/2013/07/12/communicating-sequential-processes/
;!     http://blog.drewolson.org/blog/2013/07/04/clojure-core-dot-async-and-go-a- code-comparison/
;!     http://www.leonardoborges.com/writings/2013/07/06/clojure-core-dot-async- lisp-advantage/
;!     http://www.infoq.com/presentations/clojure-core-async !     http://www.infoq.com/presentations/core-async-clojure


;Avout
;!  Атомы и ссылки в распределенной среде
;!  Координация через ZooKeeper
;!  Разные backends для хранения состояния – MongoDB, SimpleDB, плюс возможность расширения
;!  Можно использовать стандартные функции – deref, наблюдатели, валидаторы
;!  Собственная версия функций для изменения состояния: swap!!, dosync!!, alter!!, etc.
;!  Подробно – на http://avout.io/


(use 'avout.core)
(def client (connect "127.0.0.1"))
(def r0 (zk-ref client "/r0" 0))
(def r1 (zk-ref client "/r1" []))
(dosync!! client
          (alter!! r0 inc)
          (alter!! r1 conj @r0))


;Pulsar
;Реализует различные конкурентные операции Включает поддержку акторной модели
;Pattern matching как в Erlang, включая двоичные данные
;Основана на Java библиотеке Quasar
;Ресурсы:
;!     http://blog.paralleluniverse.co/2013/05/02/quasar-pulsar/ !     http://puniverse.github.io/pulsar/


;Lamina
;Предназначена для анализа потоков данных Потоки как каналы
;Возможность параллелизации обработки данных
;Ресурсы
;!     https://github.com/ztellman/lamina
;!     http://adambard.com/blog/why-clojure-part-2-async- magic/

;Hadoop-based
;! clojure-hadoop (http://github.com/alexott/clojure-hadoop)
;! parkour (https://github.com/damballa/parkour)
;! PigPen (https://github.com/Netflix/PigPen)

;Ресурсы
;http://java.ociweb.com/mark/stm/article.html
;Clojure Programming by Chas Emerick, Brian Carper, Christophe Grand. O'Reilly, 2012
;http://aphyr.com/posts/306-clojure-from-the-ground-up- state
;http://www.infoq.com/presentations/Value-Identity-State- Rich-Hickey (видео)
;http://skillsmatter.com/podcast/clojure/you-came-for-the- concurrency-right (видео)