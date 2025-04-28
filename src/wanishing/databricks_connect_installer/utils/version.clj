(ns wanishing.databricks-connect-installer.utils.version
  (:require
    [clojure.string :as str]))


(defn parse-version
  "Parse version string into a map with :major, :minor, and :patch keys"
  [version-str]
  (let [[major minor patch] (str/split version-str #"\.")]
    {:major (parse-long major)
     :minor (parse-long (or minor "0"))
     :patch (parse-long (or patch "0"))}))


(comment
    
  (parse-version "3.12.0")  ;=> {:major 3, :minor 12, :patch 0}
  (parse-version "3.12")    ;=> {:major 3, :minor 12, :patch 0}
  (parse-version "3")       ;=> {:major 3, :minor 0, :patch 0}
)


(defn version->str
  "Convert version map back to string"
  [{:keys [major minor patch]}]
  (cond
    (and major minor patch) (str major "." minor "." patch)
    (and major minor) (str major "." minor)
    :else (str major)))


(comment
  (version->str {:major 3 :minor 12 :patch 0})  ;=> "3.12.0"
  (version->str {:major 3 :minor 12})           ;=> "3.12"
  (version->str {:major 3})                     ;=> "3"
)


(defn version>=
  "Compare if version1 is greater than or equal to version2"
  [v1 v2]
  (println "----")
  (println v1)
  (println v2)
  (println "----")
  (let [v1 (if (string? v1) (parse-version v1) v1)
        v2 (if (string? v2) (parse-version v2) v2)]
    (or (> (:major v1) (:major v2))
        (and (= (:major v1) (:major v2))
             (or (> (:minor v1) (:minor v2))
                 (and (= (:minor v1) (:minor v2))
                      (>= (:patch v1) (:patch v2))))))))


(comment

  (version>= {:major 15 :minor 4} {:major 15 :minor 4 :patch nil})
  ;; Can compare version maps
  (version>= {:major 3 :minor 12} {:major 3 :minor 11})  ;=> true
  ;; Or version strings
  (version>= "3.12.0" "3.11.0")                         ;=> true
  (version>= "3.12" "3.12")                             ;=> true
  (version>= "3.11" "3.12")                             ;=> false
)


(defn version<=
  "Compare if version1 is less than or equal to version2"
  [v1 v2]
  (version>= v2 v1))


(comment
  (version<= "3.11" "3.12")                             ;=> true
  (version<= {:major 3 :minor 11} {:major 3 :minor 12}) ;=> true
  (version<= "3.12" "3.12")                             ;=> true
)


(defn version-in-range?
  "Check if version is within min-version and max-version (inclusive)"
  [version min-version max-version]
  (and (version>= version min-version)
       (or (nil? max-version)
           (version<= version max-version))))


(comment
  ;; Check if version is in range (inclusive)
  (version-in-range? "3.11" "3.10" "3.12")              ;=> true
  (version-in-range? {:major 3 :minor 11}
                     {:major 3 :minor 10}
                     {:major 3 :minor 12})               ;=> true
  ;; Works with nil max-version (unbounded upper range)
  (version-in-range? "3.12" "3.10" nil)                 ;=> true
)
