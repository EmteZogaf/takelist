(ns takelist.util
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.codecs.base64 :as b64]
            [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.spec :as s]
            [clojure.string :as str]
            [ring.util.response :as ring]
            [takelist.spec]))

(defn only
  "Like first but throws on more than one element."
  [coll]
  (when (next coll)
    (throw (Exception. "The collection contains more than one element.")))
  (first coll))

(defn unsafe-unsign
  "Unsign JWT without verifying the signature or validating claims.

  Use only when the origin of the token is known!"
  [token]
  (let [[_ payload] (str/split token #"\." 3)]
    (-> (b64/decode payload)
        (codecs/bytes->str)
        (json/parse-string keyword))))

(s/fdef error-resp
  :args (s/cat :status :takelist.http/error-status
               :msg string?)
  :ret map?
  :fn #(= (-> % :args :status)
          (-> % :ret :status)))

(defn error-resp [status msg]
  (-> (ring/response msg)
      (ring/content-type "text/plain")
      (ring/status status)))

(defn- to-time-str [formatter date]
  (-> formatter
      (time-format/with-zone (time/time-zone-for-id "Europe/Berlin"))
      (time-format/unparse date)))

(defn to-hour-minute-str [date]
  (to-time-str (:hour-minute time-format/formatters) date))

(defn to-date-time-str [date]
  (to-time-str (time-format/formatter "dd.MM.yyyy HH:mm:ss") date))
