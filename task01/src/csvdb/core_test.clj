(ns csvdb.core
  (:require [clojure-csv.core :as csv])
  (:import [java.util Date])
  )


(def figs [{:type :rect   :w 10 :h 5}
           {:type :rect   :w 10 :h 5}
           {:type :rect   :w 10 :h 5}
           {:type :circle :r 10}
           {:type :rect   :w 10 :h 5}
           {:type :rect   :w 10 :h 5}])

;; fig -> :rect | :circle
;;vultimethods for polymorphism
(defn dispatch [fig] (:type fig))

(defmulti p2 dispatch);; по результату  определяет какой метод вызывать

(defmethod p2 :circle [fig]
  (* 2 Math/PI (:r fig)))

(defmethod p2 :rect [fig]
  (* 2 (+ (:w fig)  (:h fig))))

(defmethod p2 :default [fig]
  0)

(map p2 figs)

;; допустим мы хотим сохранить в базу в зав от класса драйвера
;; диспатчинг происходит по первому аргументу
;;иерархии расширяются в рантайме
(defmulti store (fn [db key value] (class db)))
(defmethod store com.redis.RedisDB [db key value]
  (.store db (str key) (str value)))

(def bot {
           :memory :file
           :iface :console
           :brain :default
           })


(defmulti remember (fn [bot k v] (:memory bot)))
(defmulti recall (fn [bot k] (:memory bot)))


(defmulti run-cmd (fn [bot cmd] (:brain bot)))



;;__________________________________________________________--
;;протоколы тоже для полиморфизма
;;диспатчинг происходит по классу первого аргумента типо как интрефейс счастный случай мультиметодов


(defprotocol ICal
    (day    [_]) ;;набор методов в интрефейсе
    (month [_ base-month] )
    (year  [_]))

(defn format-cal [cal]
  (str (day cal) "." (month cal 1) "." (year cal)))

;;implementation

(extend-type Date
  ICal
  (day [this] (.getDate this))
  (month [this base-month] (+ base-month (.getMonth this)))
  (year [this] (+ 1900 (.getYear this))))

(extend-protocol ICal
  Date
  (day [this] (.getDate this))
  (month [this base-month] (+ base-month (.getMonth this)))
  (year [this] (+ 1900 (.getYear this))))

(format-cal (Date.))
;;протоколы на лету

(defn create-icall [d m y]
  (reify ICal;; функция прямо в теле создает временный обхект который яв импелемнтациией  по сути реализация интерефейса из ничего
         (day   [_] d)
         (month [_ b] (+ b m))
         (year  [_] y)))


(def p {:x 1 :y 2})
(:x p)
;;возможности оптимизации
;; объекты можно как создовать как хэщ мап как вектор
;; но это несет свой оверхэд связанный с хранением этого в памяти + доступ тоже есть свои вычисление эжша и найти его в таблице
;; если точек миллионы миллиарды то не оптимально
;;

(deftype PointT [x y]);;создает типо класс

(def p (PointT. 10 10));; create object
(.-x p);; доступ к java полю класса класс иммутабельный мы не можем писать .-[fiedl name java class]


(defrecord PointR [x y]) ;; closure way
(def p (PointR. 10 11))
(:x p) ;; такого нету в дефтайпе типо реализует хэщмап
;; можно добавлять поля которых в исходном рекорде не было  и есть метод equals

(= (PointR. 10 10 ) (PointR. 10 10 ))


;;рекорды могут реализовывать протоколы и нитерфейсы
(defprotocol IFormat
  (format [this]))

(defrecord Call [d m y])


(defrecord Call [d m y]
  IFormat;; реализация протокола внутри
  (format [_] (str d "." m "." y))
  java.lang.Comparable ;; реализация интерфейса
  (compareTo [_ o2]
    (compare
      [y m d]
      [(.-y o2) (.-d o2) (.-m o2)]))
  )

(extend-protocol IFormat
  Call
  (format [this] (str (.-d this) "." (.-m this) "." (.-y this))))

(format (Call. 12 12 1984))
(.compareTo (Call. 12 12 1984)  (Call. 12 12 1984))
(compare (Call. 12 12 1984)  (Call. 12 12 1984))

;;Вызов java

(import '[java.io File])

(File. "." "some.test") ;; создание нового java обекта типо File и пут 2 строки в конструктор
(.toUpperCase "abc");;вызов java метода
(Math/sqrt 25);; вызов статического метода
(.-field Obj);; доступ к полю
(set! *warn-on-reflection* true) ;;флаг для отображение варнингов

;;Type hints to remove ambigious call

;;before [] we notify about  that return type will be integer
(defn strlen ^Integer [^String s] ;; it is possible to put type there or below
  (.length ^String s)) ;; явное указание принимаемого типа

(strlen "abc")


(def ^String x "abc")
(StringBuilder. x)

;;reify proxy
(import '[java.util ArrayList Collections Comparator Timer TimerTask])

(reify Comparator;; on the fly implement interface
       (compare [this a b];; this надо указывать явно
                (- b a)))

;;proxy alow extend from defined class and implement interface позволяет наследоваться от классов
(proxy [SuperClass IFaces ...] [constructor-args]
       (method [args] ...)) ;; this присутствует неявно

(let [task (proxy [TimerTask] []
       (run []
            (println (rand)))
       )]
  (.schedule (Timer.) task 1000)
  )
;; file working
(require '[clojure.java.io :as io])

(spit "file.txt" "data")
(slurp "file.txt");; works with URLs Sockets REader etc

;;global vars it is possible to override them
*in*
*out*
*err*
;;типо  try catch c автозакрытием
;; creare beffred in -> wr    *out*  -> wr
(with-open [wr (io/writer "file.txt")]
  (binding [*out* wr] ;; говорим что текуший отпут будет нашим врайтером который мы тока что создали
    (print "Hello")
    (flush)))

;;(with-out-str) ;; типо

(with-open [rd (io/reader "file.txt")]
  (vec
   (for [line (line-seq rd)]
     line)))
(io/resource "log4j.properties")

;;EDN format data transfering serializing desiarializing data vs JSON

(spit "f.edn" {:a 1})
(clojure.edn/read-string (slurp "f.edn"))

#inst "2012-08-08"

;;#clojure.uuid

;;METADATA
;;прицепить к любому объекту хэшмапу
;;дать хинты компиляторы чтоб поменять ему поведение

(def m {:a 1})
(with-meta m {:doc "123"});;write metadad
(meta #'m);;read metadata

(def ^{:private true} x 0)
(def ^:private e 0)
(defn f [x u z])
(defn ^:private f [x u z])
(defn- f [x u z])

;;gen-classs generate java class


