(ns csvdb.core
  (:require [clojure.core.match :refer [match]])
  )

;;deref или @ для доступа к текущему состоянию ссылки, атома или агента

;Ссылки
;Синхронное, координированное изменение данных
;Основаны на Software Transactional Memory (MVCC)
;Atomicity, Consistence, Isolation
;Изменения только в рамках транзакций!
;Транзакция повторяется при конфликте с другой транзакцией
;Транзакция прекращается при генерации исключения
;Поддержка функций-валидаторов и функций- наблюдателей

;Ссылки: использование
;Создание: (ref x & опции)!
;Начало транзакции: (dosync ...) Изменение: alter, commute или ref-set Блокировка: ensure

(defn transfer-money [from to amount]
  (dosync
   (if (< @from amount)
     (throw (IllegalStateException.
              (str "Account has less money that required! "
                   @from " < " amount)))
     (do (alter from - amount)
       (alter to + amount)))))


(def ^:private acc-1 (ref 1000))
(def ^:private acc-2 (ref 1000))

(transfer-money acc-1 acc-2 500)

@acc-1
@acc-2


(defn add-to-deposit [to amount]
  (dosync
   (commute to + amount)));;коммутативное тока внутри выполнит

@acc-1

(add-to-deposit acc-1 100)

(defn write-log [log-msg]
  (io!
    (println log-msg)))

(dosync (write-log "test"))

;;Atoms

;Синхронное, некоординированное изменение Основная функция – swap!
;Поддержка валидаторов и наблюдателей Прекращает повторы при исключении


(def ^:private counters-atom (atom {}))
(defn inc-counter [name]
  (swap! counters-atom update-in [name]
         (fnil inc 0)))
(defn dec-counter [name]
  (swap! counters-atom update-in [name]
         (fnil dec 0)))
(defn reset-counter [name]
  (swap! counters-atom assoc name 0))

@counters-atom
(inc-counter :test)
(inc-counter :another-test)
(reset-counter :test)

;;Agents
;Асинхронное, некоординированное изменение – fire & forget
;Функции: send – bounded thread pool, send-off – unbounded thread pool
;Валидаторы и наблюдатели
;Возможность обработки ошибок при выполнении кода

(def ^:private counters-agent (agent {}))
(defn a-inc-counter [name]
  (send counters-agent update-in [name]
        (fnil inc 0)))
(defn a-dec-counter [name]
  (send counters-agent update-in [name]
        (fnil dec 0)))
(defn a-reset-counter [name]
  (send counters-agent assoc name 0))


; Агенты и ошибки

(def err-agent (agent 1))
(send err-agent (fn [_] (throw
                          (Exception. "we have a problem!"))))

(send err-agent identity)

(def err-agent (agent 1
                      :error-mode :continue))
(send err-agent (fn [_] (throw
                          (Exception. "we have a problem!"))))
(send err-agent inc)
@err-agent


;Vars

;Изолированное изменение в рамках одного потока Изменение применяется ко всему вызываемому коду Var должна быть объявлена как :dynamic
;binding – переопределение значений:
;!   Работает при использовании agents, pmap & futures !   Не работает с lazy sequences
;alter-var-root – изменение top-level значения


(def ^:dynamic *test-var* 20);; определение динамичекой переменной

(defn add-to-var [num]
  (+ num *test-var*))

(defn print-var [txt]
    (println txt *test-var*))

(defn run-thread [x]
    (.run (fn []
            (print-var (str "Thread " x " before:"))
            (binding [*test-var* (rand-int 10000)] ;; переопределение vars
              (print-var (str "Thread " x " after:"))))))

(doseq [x (range 3)] (run-thread x)) ;; run 3 thread executions

(defn run-thread2 [x]
  (.run (fn []
          (binding [*test-var* (rand-int 10000)]
            (println "Thread " x " var=" *test-var*)
            (set! *test-var* (rand-int 10000));; переопределение с помощтю set
            (println "Thread " x " var2=" *test-var*)))))


(doseq [x (range 3)] (run-thread2 x))


(defn run-thread3 [x]
  (.run (fn []
          (set! *test-var* (rand-int 10000))
          (println "Thread " x " var2=" *test-var*))))

(run-thread3 10)

(alter-var-root #'*test-var* (constantly 10));; #' макрос процедуры чтения


;;Валидаторы
;; ссылки, атомы, агенты, vars имеют общую функциональность это возможность задания функций валидаторов
;; для проверки значений устанавливаемых в процессе измененй а так же задание функицй наблюдателей которые будут вызываться
;; при изменении значений

;; вылидаторы будут принимать каждое новое значение и должны вернуть false или throw exception if state is unacceptable
(def a (atom 2));; define an atom
(set-validator! a pos?);; set func pos as a validator проверяет является ли число позитивным

(swap! a dec)
(swap! a dec)

;;Наблюдатели  можно задать сколько угодно функций наблюдателей для одного объекта

(def a (atom 1))

(add-watch a "watch 1: "
           (fn [k r o n] (println k r o n)))

(add-watch a "watch 2: "
           (fn [k r o n] (println k r o n)))

(swap! a inc)

(remove-watch a "watch 1: ")!

(swap! a inc)!

(def ^:dynamic b 1)

(add-watch (var b) "dynamic: "
           (fn [k r o n] (println k r o n)))

(alter-var-root (var b) (constantly 42))

(binding [b 10] (println b))

;;  Изменяемое состояние (разное)
;Transients (переходные структуры данных)
;Изменяемые поля в deftype
;Локальные vars

;;Transients когда надо выполнить большое изменение данных для одной структуры данныз
;;  у нас же данные неизменемяе вот это и надо чтоб предотвартить копиорвание данныз
;; преобразуем стандартную структуру в переходнуз
(defn vrange [n];;  использование стандартной неизменяемой структуры
  (loop [i 0 v []]
    (if (< i n)
      (recur (inc i) (conj v i))
      v)))

;; принудительная изоляция для других потоков происходит неявно
;; изменяемые данныз становятся недоступны для других потоков
(defn vrange2 [n]
  (loop [i 0 v (transient [])];; преобразуем в переходную структуру
    (if (< i n)
      (recur (inc i) (conj! v i))
      (persistent! v))));; после манипуляций переводим в персистентную

(time (def v (vrange 1000000)))
(time (def v2 (vrange2 1000000)))

;; Изменяемые поля в deftype
;; для создания низкоуровневыз структор
(defprotocol TestProtocol
  (get-data [this])
  (set-data [this o]))
(deftype Test [^:unsynchronized-mutable x-var]
  TestProtocol
  (set-data [this o] (set! x-var o))
  (get-data [this] x-var))

(def a (Test. 10))
(get-data a)


(set-data a 42)
(get-data a)

;; Локальные vars
;with-local-vars позволяет определить локальные vars, с которыми можно работать через var-set & var- get (или @)

(defn factorial [x]!
  (with-local-vars [acc 1, cnt x]!
    (while (> @cnt 0)!
      (var-set acc (* @acc @cnt))!
      (var-set cnt (dec @cnt)))!
    @acc))