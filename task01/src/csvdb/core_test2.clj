(ns csvdb.core.SomeClass;;generate java classs with this name
  (:gen-class);;option
  )

(defn some-func [x y])  ;; non java method clojure function


(defn -method [x];; java method
  (println x))

(.method (csvdb.core.SomeClass.) "abc") ;;обращение к java class

(defn -main [& args] ;; main method
  (println args))

