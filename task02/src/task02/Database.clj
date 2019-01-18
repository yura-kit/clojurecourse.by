(ns task02.Database
  (:use [task02 db query])
  (:gen-class;;генерим java class
    :main false
    :methods [#^{:static true} [InitDatabase []       void]
              #^{:static true} [Select       [String] String]]))
  ;; Объявить класс task02.Database с двумя статическими функциями доступными из Java:
  ;;  - void InitDatabase() - должна выполнять начальную загрузку данных используя функцию
  ;;       task02.db/load-initial-data
  ;;  - String Select(String query) - должна выполнять запрос к базе данных
  ;;       (task02.query/perform-query) и возвращать результат в виде строки в формате EDN.

  ;; Hint: load-initial-data, pr-str, perform-query
  ; :implement-me


  (defn -InitDatabase []
    (task02.db/load-initial-data))

  (defn -Select [^String query]
    (pr-str (task02.query/perform-query query)))

