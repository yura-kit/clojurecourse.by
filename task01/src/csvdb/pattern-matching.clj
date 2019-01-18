(ns csvdb.core
  (:require [clojure.core.match :refer [match]])
  )

(def some-text (vec (.split "select students where id > 10 " " ")))
;; clojure/core.match pattern matching разбор строки без использование регулярных выражений

(defn check-match [s]
  "find table from SQL and filtartion condition"
  (match s
         ["select" tbl & _] (do (println "table is:" tbl)
                                (check-match (vec (drop 2 s))));;выкидываем 2 найденых параметра и продолжаем поиск чтою найти  where
         ["where" a b c] (println "conditions:" a b c)
         :else (println "no matching")))

(check-match some-text)

