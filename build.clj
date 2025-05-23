(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.edn :as edn]))

(def lib 'wanishing/databricks-connect-installer)
(def version (-> (edn/read-string (slurp "deps.edn"))
                 :aliases
                 :databricks-connect-installer
                 :project
                 :version))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'wanishing.databricks-connect-installer.core}))
