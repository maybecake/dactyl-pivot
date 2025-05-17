(ns dactyl-pivot.single-plate
    (:refer-clojure :exclude [use import])
    (:require [scad-clj.scad :refer :all]
              [scad-clj.model :refer :all]
              [unicode-math.core :refer :all]))

;; Default values for standalone usage
(def default-params
  {:keyswitch-height 14.15
   :keyswitch-width 14.15
   :plate-thickness 4
   :side-nub-thickness 4
   :retention-tab-thickness 1.5
   :wall-thickness 5
   :create-side-nubs? true})

;; Default values for closed version
(def default-params-closed
  {:keyswitch-height 14.15
   :keyswitch-width 14.15
   :plate-thickness 4
   :side-nub-thickness 4
   :retention-tab-thickness 1.5
   :wall-thickness 1.8
   :create-side-nubs? true})

(defn create-single-plate [{:keys [keyswitch-height
                                 keyswitch-width
                                 plate-thickness
                                 side-nub-thickness
                                 retention-tab-thickness
                                 wall-thickness
                                 create-side-nubs?]
                          :or {create-side-nubs? true}}]
    (let [retention-tab-hole-thickness (- (+ plate-thickness 0.5) retention-tab-thickness)
          mount-width (+ keyswitch-width (* 2 wall-thickness))
          mount-height (+ keyswitch-height (* 2 wall-thickness))
          
          ;; Creates the top wall of the switch cutout
          top-wall (->> (cube mount-width wall-thickness plate-thickness)
                        (translate [0
                                    (+ (/ wall-thickness 2) (/ keyswitch-height 2))
                                    (/ plate-thickness 2)]))
          
          ;; Creates the left wall of the switch cutout
          left-wall (->> (cube wall-thickness mount-height plate-thickness)
                         (translate [(+ (/ wall-thickness 2) (/ keyswitch-width 2))
                                     0
                                     (/ plate-thickness 2)]))
          
          ;; Creates the side nub that helps secure the switch in place
          side-nub (->> (binding [*fn* 30] (cylinder 0.75 2.75))  ;; Creates a cylinder for the nub
                        (rotate (/ π 2) [1 0 0])  ;; Rotates to horizontal position
                        (translate [(+ (/ keyswitch-width 2)) 0 1])  ;; Positions at edge of switch
                        (hull (->> (cube wall-thickness 2.75 plate-thickness)  ;; Creates the main nub body
                                   (translate [(+ (/ wall-thickness 2) (/ keyswitch-width 2))
                                               0
                                               (/ side-nub-thickness 2)])))
                        (translate [0 0 (- plate-thickness side-nub-thickness)]))  ;; Adjusts height
          
          ;; Combines the walls and optional side nub into one half of the plate
          plate-half (union top-wall left-wall (if create-side-nubs? (with-fn 100 side-nub)))
          
          ;; Creates the complete plate by mirroring the half plate in both X and Y directions
          plate (union plate-half
                      (->> plate-half
                           (mirror [1 0 0])  ;; Mirror across X axis
                           (mirror [0 1 0])))]  ;; Mirror across Y axis
      
      ;; Return a map containing both the plate and its dimensions
      {:plate plate
       :dimensions {:mount-width mount-width
                   :mount-height mount-height
                   :wall-thickness wall-thickness
                   :plate-thickness plate-thickness}}))

(defn create-single-plate-closed [{:keys [keyswitch-height
                                        keyswitch-width
                                        plate-thickness
                                        side-nub-thickness
                                        retention-tab-thickness
                                        wall-thickness
                                        create-side-nubs?]
                                 :or {create-side-nubs? true}}]
    (let [retention-tab-hole-thickness (- (+ plate-thickness 0.5) retention-tab-thickness)
          mount_width (+ keyswitch-width (* 2 wall-thickness))
          mount_height (+ keyswitch-height (* 2 wall-thickness))
          
          ;; Creates the top wall of the switch cutout with closed version dimensions
          top-wall (->> (cube mount_width wall-thickness (+ plate-thickness 0.5))
                        (translate [0
                                    (+ (/ wall-thickness 2) (/ keyswitch-height 2))
                                    (- (/ plate-thickness 2) 0.25)]))
          
          ;; Creates the left wall of the switch cutout with closed version dimensions
          left-wall (->> (cube wall-thickness mount_height (+ plate-thickness 0.5))
                         (translate [(+ (/ wall-thickness 2) (/ keyswitch-width 2))
                                     0
                                     (- (/ plate-thickness 2) 0.25)]))
          
          ;; Creates the side nub that helps secure the switch in place (closed version)
          side-nub (->> (binding [*fn* 30] (cylinder 1 2.75))
                        (rotate (/ π 2) [1 0 0])
                        (translate [(+ (/ keyswitch-width 2)) 0 1])
                        (hull (->> (cube wall-thickness 2.75 side-nub-thickness)
                                   (translate [(+ (/ wall-thickness 2) (/ keyswitch-width 2))
                                               0
                                               (/ side-nub-thickness 2)])))
                        (translate [0 0 (- plate-thickness side-nub-thickness)]))
          
          ;; Combines the walls and optional side nub into one half of the plate
          plate-half (union top-wall left-wall (if create-side-nubs? (with-fn 100 side-nub)))
          
          ;; Creates the retention tab cutout
          top-nub (->> (cube 5 5 retention-tab-hole-thickness)
                       (translate [(+ (/ keyswitch-width 2.5)) 0 (- (/ retention-tab-hole-thickness 2) 0.5)]))
          top-nub-pair (union top-nub
                             (->> top-nub
                                  (mirror [1 0 0])
                                  (mirror [0 1 0])))
          
          ;; Creates the complete plate with retention tab cutouts
          plate (difference
                 (union plate-half
                        (->> plate-half
                             (mirror [1 0 0])
                             (mirror [0 1 0])))
                 (->> top-nub-pair
                      (rotate (/ π 2) [0 0 1])))]
      
      ;; Return a map containing both the plate and its dimensions
      {:plate plate
       :dimensions {:mount-width mount_width
                   :mount-height mount_height
                   :wall-thickness wall-thickness
                   :plate-thickness plate-thickness}}))

;; Generate the SCAD files with both versions
(do
  ;; Create the output directory if it doesn't exist
  (.mkdir (java.io.File. "things"))
  
  ;; Export both plate versions side by side in a single SCAD file
  (spit "things/single-plates.scad"
        (write-scad
         (union
          ;; Open version on the left
          (translate [-15 0 0] (:plate (create-single-plate default-params)))
          ;; Closed version on the right
          (translate [15 0 0] (:plate (create-single-plate-closed default-params-closed))))))) 