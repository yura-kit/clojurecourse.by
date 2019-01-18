(ns task02.query
  (:use [task02 helpers db])
  (:require [clojure.core.match :refer [match]]))

;; Функция выполняющая парсинг запроса переданного пользователем
;;
;; Синтаксис запроса:
;; SELECT table_name [WHERE column comp-op value] [ORDER BY column] [LIMIT N] [JOIN other_table ON left_column = right_column]
;;
;; - Имена колонок указываются в запросе как обычные имена, а не в виде keywords. В
;;   результате необходимо преобразовать в keywords
;; - Поддерживаемые операторы WHERE: =, !=, <, >, <=, >=
;; - Имя таблицы в JOIN указывается в виде строки - оно будет передано функции get-table для получения объекта
;; - Значение value может быть либо числом, либо строкой в одинарных кавычках ('test').
;;   Необходимо вернуть данные соответствующего типа, удалив одинарные кавычки для строки.
;;
;; - Ключевые слова --> case-insensitive
;;
;; Функция должна вернуть последовательность со следующей структурой:
;;  - имя таблицы в виде строки
;;  - остальные параметры которые будут переданы select
;;
;; Если запрос нельзя распарсить, то вернуть nil

;; Примеры вызова:
;; > (parse-select "select student")
;; ("student")
;; > (parse-select "select student where id = 10")
;; ("student" :where #<function>)
;; > (parse-select "select student where id = 10 limit 2")
;; ("student" :where #<function> :limit 2)
;; > (parse-select "select student where id = 10 order by id limit 2")
;; ("student" :where #<function> :order-by :id :limit 2)
;; > (parse-select "select student where id = 10 order by id limit 2 join subject on id = sid")
;; ("student" :where #<function> :order-by :id :limit 2 :joins [[:id "subject" :sid]])
;; > (parse-select "werfwefw")
;; nil
(def str-to-operation {"=" =
              "!=" not=
              "<" <
              ">" >
              "<=" <=
              ">=" >=})


(defn- get-value [value]
  (if (re-matches #"'.*'" value)
    value
    (parse-int value)))

(defn make-where-function [& args]
  (let [column (keyword (nth args 0))
        operation (str-to-operation (nth args 1))
        value (get-value (nth args 2))]
    #(operation (column %) value )
    ))

(defn select? [s] (= (clojure.string/lower-case s) "select"))
(defn limit? [s] (= (clojure.string/lower-case s) "limit"))
(defn where? [s] (= (clojure.string/lower-case s) "where"))
(defn order? [s] (= (clojure.string/lower-case s) "order"))
(defn by? [s] (= (clojure.string/lower-case s) "by"))
(defn join? [s] (= (clojure.string/lower-case s) "join"))
(defn on? [s] (= (clojure.string/lower-case s) "on"))

(defn- parse-tokenized-request [tokenized-str res]
    (match tokenized-str
         [] res
         [(_ :guard select?) table & rest]
           (recur  rest (conj res table))

         [(_ :guard limit?) n & rest]
           (recur  rest (concat res [:limit (parse-int n)]))

         [(_ :guard where?) left op right & rest]
           (recur  rest (concat res [:where (make-where-function left op right)]))

         [(_ :guard order?) (_ :guard by?) order-by-col & rest]
           (recur  rest (concat res [:order-by (keyword order-by-col)]))

         [(_ :guard join?) join_table (_ :guard on?) left_column "=" right_column & rest]
           (recur  rest (concat res [:joins [[(keyword left_column) join_table (keyword right_column)]]]))

         :else res
         ))


(defn parse-select [^String sel-string]
  (let [tokenized-str (-> sel-string
                          (clojure.string/split #" ")
                          )]
    (parse-tokenized-request tokenized-str [])))

(parse-select "select student where id = 10 order by id limit 2")



;; Выполняет запрос переданный в строке.  Бросает исключение если не удалось распарсить запрос

;; Примеры вызова:
;; > (perform-query "select student")
;; ({:id 1, :year 1998, :surname "Ivanov"} {:id 2, :year 1997, :surname "Petrov"} {:id 3, :year 1996, :surname "Sidorov"})
;; > (perform-query "select student order by year")
;; ({:id 3, :year 1996, :surname "Sidorov"} {:id 2, :year 1997, :surname "Petrov"} {:id 1, :year 1998, :surname "Ivanov"})
;; > (perform-query "select student where id > 1")
;; ({:id 2, :year 1997, :surname "Petrov"} {:id 3, :year 1996, :surname "Sidorov"})
;; > (perform-query "not valid")
;; exception...
(defn perform-query [^String sel-string]
  (if-let [query (parse-select sel-string)]
    (apply select (get-table (first query)) (rest query))
    (throw (IllegalArgumentException. (str "Can't parse query: " sel-string)))))
