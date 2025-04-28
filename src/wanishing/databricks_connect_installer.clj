#!/usr/bin/env bb

(ns wanishing.databricks-connect-installer
  (:require
    [wanishing.databricks-connect-installer.core :as core]))


(apply core/-main *command-line-args*)
