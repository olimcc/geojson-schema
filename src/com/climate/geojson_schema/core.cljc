;  Copyright 2014 The Climate Corporation

;  Licensed under the Apache License, Version 2.0 (the "License");
;  you may not use this file except in compliance with the License.
;  You may obtain a copy of the License at

;      http://www.apache.org/licenses/LICENSE-2.0

;  Unless required by applicable law or agreed to in writing, software
;  distributed under the License is distributed on an "AS IS" BASIS,
;  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;  See the License for the specific language governing permissions and
;  limitations under the License.
(ns com.climate.geojson-schema.core
  (:require
    [schema.core :refer [Any optional-key required-key eq one pred both either maybe named conditional Str Num Keyword]]))

(def ^:private position [Num])

(def ^:private geojson-base
  {(optional-key :bbox) [Num]
   ;; GeoJSON objects may contain foreign members
   ;; https://tools.ietf.org/html/rfc7946#section-6.1
   Keyword Any})

(def Point
  "For type \"Point\" the :coordinates member must be a single position.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.1.2>."
  (merge geojson-base
         {:coordinates position
          :type (eq "Point")}))

(def MultiPoint
  "For type \"MultiPoint\", the :coordinates member must be an array of
  positions.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.1.3>."
  (merge geojson-base
         {:coordinates [position]
          :type (eq "MultiPoint")}))

(def ^:private linear-string-coordinates
  [(one position "first")
   (one position "second")
   position])

(def LineString
  "For type \"LineString\", the :coordinates member must be an array of
  LineString coordinate arrays.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.1.4>."
  (merge geojson-base
         {:coordinates linear-string-coordinates
          :type (eq "LineString")}))

;; Linear Ring
;;;A Linear ring is a closed loop, so it must have
;;;at least 3 vertices.
;;;
;;;        Cord 0 *and* Cord 3
;;;                #
;;;               @ @
;;;              @   @
;;;             @     @
;;;            @       @
;;;           @         @
;;;          @           @
;;;         @             @
;;;        @               @
;;;       #                 #
;;;      @'''''''''''''''''''@
;;; Cord 1                    Cord 2
(defn- closed-loop
  "A loop is closed if it has at least 4 coordinates and the first coordinate
  is the last. There is no requirement in the spec that the closed shape not
  intersect itself."
  [coordinate-seq]
  (and (= (first coordinate-seq)
          (last coordinate-seq))
       (>= (count coordinate-seq)
          4)))

(def ^:private linear-ring-coordinates
  (both [position] (pred closed-loop 'closed)))

(def LinearRing
  "A LinearRing is closed LineString with 4 or more positions. The first and
  last positions are equivalent (they represent equivalent points). Though a
  LinearRing is not explicitly represented as a GeoJSON geometry type, it is
  referred to in the Polygon geometry type definition."
  (merge geojson-base
         {:coordinates linear-ring-coordinates
          :type (eq "LineString")}))

(def MultiLineString
  "For type \"MultiLineString\", the :coordinates member must be an array of
  LineString coordinate arrays.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.1.5>."
  (merge geojson-base
         {:coordinates [linear-string-coordinates]
          :type (eq "MultiLineString")}))

(def ^:private polygon-coords
  [linear-ring-coordinates])

(def Polygon
  "For type \"Polygon\", the :coordinates member must be an array of LinearRing
  coordinate arrays. For Polygons with multiple rings, the first must be the
  exterior ring and any others must be interior rings or holes.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.1.6>."
  (merge geojson-base
         {:coordinates polygon-coords
          :type (eq "Polygon")}))

(def MultiPolygon
  "For type \"MultiPolygon\", the :coordinates member must be an array of
  Polygon coordinate arrays.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.1.7>."
  (merge geojson-base
         {:coordinates [polygon-coords]
          :type (eq "MultiPolygon")}))

(def Geometry
  "A geometry is a GeoJSON object where the type member's value is one of the
  following strings: \"Point\", \"MultiPoint\", \"LineString\",
  \"MultiLineString\", \"Polygon\", \"MultiPolygon\", or \"GeometryCollection\".

  A GeoJSON geometry object of any type other than \"GeometryCollection\" must
  have a member with the name \"coordinates\". The value of the coordinates
  member is always an array. The structure for the elements in this array is
  determined by the type of geometry.

  This Geometry schema is everything excluding GeometryCollection.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.1>."
  (either Point
          MultiPoint
          LineString
          MultiLineString
          Polygon
          MultiPolygon))

(def GeometryCollection
  "A GeoJSON object with type \"GeometryCollection\" is a geometry object which
  represents a collection of geometry objects.

  A geometry collection must have a member with the name \"geometries\". The value
  corresponding to \"geometries\" is an array. Each element in this array is a
  GeoJSON geometry object.

  A GeometryCollection should not include other GeometryCollections.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.1.8>."
  (merge geojson-base
         {:geometries [Geometry]
          :type (eq "GeometryCollection")}))

(def Feature
  "A GeoJSON object with the type \"Feature\" is a feature object.

  - A feature object must have a member with the name \"geometry\". The value of
    the geometry member is a geometry object as defined above or a JSON null
    value.

  - A feature object must have a member with the name \"properties\". The value
    of the properties member is an object (any JSON object or a JSON null value).

  - If a feature has a commonly used identifier, that identifier should be
    included as a member of the feature object with the name \"id\".

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.2>."
  (merge geojson-base
         {:geometry Geometry
          :type (eq "Feature")
          :properties (maybe Any)
          (optional-key :id) (either Str Num)}))

(def FeatureCollection
  "A GeoJSON object with the type \"FeatureCollection\" is a feature collection
  object.

  An object of type \"FeatureCollection\" must have a member with the name
  \"features\". The value corresponding to \"features\" is an array. Each
  element in the array is a feature object as defined above.

  Cf. <https://tools.ietf.org/html/rfc7946#section-3.3>."
  (merge geojson-base
         {:features [Feature]
          :type (eq "FeatureCollection")}))

(def GeoJSON
  "This is any valid GeoJSON object.

  Cf. <https://tools.ietf.org/html/rfc7946>."
  (either Geometry
          GeometryCollection
          Feature
          FeatureCollection))
