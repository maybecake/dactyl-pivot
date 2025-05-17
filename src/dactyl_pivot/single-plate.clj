(ns dactyl-pivot.single-plate
    (:refer-clojure :exclude [use import])
    (:require [scad-clj.scad :refer :all]
              [scad-clj.model :refer :all]
              [unicode-math.core :refer :all]))

;; Default values for standalone usage
(def default-params
  {:keyswitch-height 14.15
   :keyswitch-width 14.15
   :sa-profile-key-height 12.7
   :plate-thickness 4
   :side-nub-thickness 4
   :retention-tab-thickness 1.5
   :create-side-nubs? true})

;; Default values for closed version
(def default-params-closed
  {:keyswitch-height 14.15
   :keyswitch-width 14.15
   :sa-profile-key-height 12.7
   :plate-thickness 4
   :side-nub-thickness 4
   :retention-tab-thickness 1.5
   :create-side-nubs? true})

(defn create-single-plate [{:keys [keyswitch-height
                                 keyswitch-width
                                 sa-profile-key-height
                                 plate-thickness
                                 side-nub-thickness
                                 retention-tab-thickness
                                 create-side-nubs?]
                          :or {create-side-nubs? true}}]
    (let [retention-tab-hole-thickness (- (+ plate-thickness 0.5) retention-tab-thickness)
          mount-width (+ keyswitch-width 3)
          mount-height (+ keyswitch-height 3)
          
          ;; Creates the top wall of the switch cutout
          top-wall (->> (cube (+ keyswitch-width 3) 1.5 plate-thickness)
                        (translate [0
                                    (+ (/ 1.5 2) (/ keyswitch-height 2))
                                    (/ plate-thickness 2)]))
          
          ;; Creates the left wall of the switch cutout
          left-wall (->> (cube 1.5 (+ keyswitch-height 3) plate-thickness)
                         (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                     0
                                     (/ plate-thickness 2)]))
          
          ;; Creates the side nub that helps secure the switch in place
          side-nub (->> (binding [*fn* 30] (cylinder 0.75 2.75))  ;; Creates a cylinder for the nub
                        (rotate (/ π 2) [1 0 0])  ;; Rotates to horizontal position
                        (translate [(+ (/ keyswitch-width 2)) 0 1])  ;; Positions at edge of switch
                        (hull (->> (cube 1.5 2.75 plate-thickness)  ;; Creates the main nub body
                                   (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                               0
                                               (/ side-nub-thickness 2)])))
                        (translate [0 0 (- plate-thickness side-nub-thickness)]))  ;; Adjusts height
          
          ;; Combines the walls and optional side nub into one half of the plate
          plate-half (union top-wall left-wall (if create-side-nubs? (with-fn 100 side-nub)))]
      
      ;; Creates the complete plate by mirroring the half plate in both X and Y directions
      (union plate-half
             (->> plate-half
                  (mirror [1 0 0])  ;; Mirror across X axis
                  (mirror [0 1 0])))))  ;; Mirror across Y axis

(defn create-single-plate-closed [{:keys [keyswitch-height
                                        keyswitch-width
                                        sa-profile-key-height
                                        plate-thickness
                                        side-nub-thickness
                                        retention-tab-thickness
                                        create-side-nubs?]
                                 :or {create-side-nubs? true}}]
    (let [retention-tab-hole-thickness (- (+ plate-thickness 0.5) retention-tab-thickness)
          mount-width (+ keyswitch-width 3)
          mount-height (+ keyswitch-height 3)
          
          ;; Creates the top wall of the switch cutout with closed version dimensions
          top-wall (->> (cube (+ keyswitch-width 3) 1.5 (+ plate-thickness 0.5))
                        (translate [0
                                    (+ (/ 1.5 2) (/ keyswitch-height 2))
                                    (- (/ plate-thickness 2) 0.25)]))
          
          ;; Creates the left wall of the switch cutout with closed version dimensions
          left-wall (->> (cube 1.8 (+ keyswitch-height 3) (+ plate-thickness 0.5))
                         (translate [(+ (/ 1.8 2) (/ keyswitch-width 2))
                                     0
                                     (- (/ plate-thickness 2) 0.25)]))
          
          ;; Creates the side nub that helps secure the switch in place (closed version)
          side-nub (->> (binding [*fn* 30] (cylinder 1 2.75))
                        (rotate (/ π 2) [1 0 0])
                        (translate [(+ (/ keyswitch-width 2)) 0 1])
                        (hull (->> (cube 1.5 2.75 side-nub-thickness)
                                   (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
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
                                  (mirror [0 1 0])))]
      
      ;; Creates the complete plate with retention tab cutouts
      (difference
       (union plate-half
              (->> plate-half
                   (mirror [1 0 0])
                   (mirror [0 1 0])))
       (->> top-nub-pair
            (rotate (/ π 2) [0 0 1])))))

;; Generate the SCAD files with both versions
(do
  ;; Create the output directory if it doesn't exist
  (.mkdir (java.io.File. "things"))
  
  ;; Export both plate versions side by side in a single SCAD file
  (spit "things/single-plates.scad"
        (write-scad
         (union
          ;; Open version on the left
          (translate [-15 0 0] (create-single-plate default-params))
          ;; Closed version on the right
          (translate [15 0 0] (create-single-plate-closed default-params-closed)))))) 