(

  "intersect"
  [(dim :number :grain #(> (:grain %) 1)) (dim :number)] ; grain 1 are taken care of by specific rule
  (compose-numbers %1 %2) 

  "intersect (with and)"
  [(dim :number :grain #(> (:grain %) 1)) #"(?i)and" (dim :number)] ; grain 1 are taken care of by specific rule
  (compose-numbers %1 %3) 

   ;;
 ;; Integers
 ;;
 
  "integer (0..19)"
  #"(?i)(niks|nul|nihil|een|twee|drie|veertien|vier|vijf|zestien|zes|zeventien|zeven|achttien|acht|negentien|negen|elf|twaalf|dertien|vijftien)"
  ; fourteen must be before four, or it won't work because the regex will stop at four
  {:dim :number
   :integer true
   :value (get {"niks" 0 "nul" 0 "nihil" 0 "een" 1 "twee" 2 "drie" 3 "vier" 4 "vijf" 5
              "zes" 6 "zeven" 7 "acht" 8 "negen" 9 "tien" 10 "elf" 11
              "twaalf" 12 "dertien" 13 "viertien" 14 "vijftien" 15 "zestien" 16
              "zeventien" 17 "achttien" 18 "negentien" 19}
              (-> %1 :groups first .toLowerCase))}
  
  "tien"
  #"(?i)tien"
  {:dim :number :integer true :value 10 :grain 1}
  
  "honderd"
  #"(?i)honderd?"
  {:dim :number :integer true :value 100 :grain 2}

  "duizend"
  #"(?i)duizend?"
  {:dim :number :integer true :value 1000 :grain 3}

  "miljoen"
  #"(?i) miljoen?"
  {:dim :number :integer true :value 1000000 :grain 6}

  "paar"
  #"(een )?paar?"
  {:dim :number :integer true :value 2}
  
  "aantal" ; TODO set assumption
  #"(een )?aantal"
  {:dim :number :integer true :precision :approximate :value 3}

  "heel (20..90)"
  #"(?i)(twintig|dertig|veertig|vijftig|zestig|zeventig|tachtig|negentig)"
  {:dim :number
   :integer true
   :value (get {"twintig" 20 "dertig" 30 "veertig" 40 "vijftig" 50 "zestig" 60
              "zeventig" 70 "tachtig" 80 "negentig" 90}
             (-> %1 :groups first .toLowerCase))
   :grain 1}

  "heel 21..99"
  [(integer 10 90 #(#{20 30 40 50 60 70 80 90} (:value %))) (integer 1 9)]
  {:dim :number
   :integer true
   :value (+ (:value %1) (:value %2))}

  "heel (numeriek)"
  #"(\d{1,18})"
  {:dim :number
   :integer true
   :value (Long/parseLong (first (:groups %1)))}
  
  "heel tot duizendste ,"
  #"(\d{1,3}(,\d\d\d){1,5})"
  {:dim :number
   :integer true
   :value (-> (:groups %1)
            first
            (clojure.string/replace #"," "")
            Long/parseLong)}
  
  ; composition
  
  "aantal honderden"
  [(integer 1 99) (integer 100 100)]
  {:dim :number
   :integer true
   :value (* (:value %1) (:value %2))
   :grain (:grain %2)}

  "aantal duizendsten"
  [(integer 1 999) (integer 1000 1000)]
  {:dim :number
   :integer true
   :value (* (:value %1) (:value %2))
   :grain (:grain %2)}

  "aantal miljoenen"
  [(integer 1 99) (integer 1000000 1000000)]
  {:dim :number
   :integer true
   :value (* (:value %1) (:value %2))
   :grain (:grain %2)}

  ;;
  ;; Decimals
  ;;
  
  "decimaal getal"
  #"(\d*\.\d+)"
  {:dim :number
   :value (Double/parseDouble (first (:groups %1)))}

  "nummer komma nummer"
  [(dim :number #(not (:number-prefixed %))) #"(?i)komma" (dim :number #(not (:number-suffixed %)))]
  {:dim :number
   :value (+ (* 0.1 (:value %3)) (:value %1))}
   

  "decimaal met duizenden afscheider"
  #"(\d+(,\d\d\d)+\.\d+)"
  {:dim :number
   :value (-> (:groups %1)
            first
            (clojure.string/replace #"," "")
            Double/parseDouble)}

  ;; negative number
  "nummers met voorvoegsel -, negatief of min"
  [#"(?i)-|min?|negatief?" (dim :number #(not (:number-prefixed %)))]
  (let [multiplier -1
        value      (* (:value %2) multiplier)
        int?       (zero? (mod value 1)) ; often true, but we could have 1.1111K
        value      (if int? (long value) value)] ; cleaner if we have the right type
    (assoc %2 :value value
              :integer int?
              :number-prefixed true)) ; prevent "- -3km" to be 3 billions


  ;; suffixes

  ; note that we check for a space-like char after the M, K or G
  ; to avoid matching 3 Mandarins
  "nummer achtervoegsels (K, M, G)"
  [(dim :number #(not (:number-suffixed %))) #"(?i)([kmg])(?=[\W\$€]|$)"]
  (let [multiplier (get {"k" 1000 "m" 1000000 "g" 1000000000}
                        (-> %2 :groups first .toLowerCase))
        value      (* (:value %1) multiplier)
        int?       (zero? (mod value 1)) ; often true, but we could have 1.1111K
        value      (if int? (long value) value)] ; cleaner if we have the right type
    (assoc %1 :value value
              :integer int?
              :number-suffixed true)) ; prevent "3km" to be 3 billions

  ;;
  ;; Ordinal numbers
  ;;
  
  "rangtelwoorden (eerste..19de)"
  #"(?i)(eerste|tweede|derde|vierde|vijfde|zesde|zevenste|achtste|negende|tiende|elfde|twaalfde|dertiende|veertiende|vijftiende|zestiende|zeventiende|achttiende|negentiende)"
  {:dim :ordinal
   :value (get {"eerste" 1 "tweede" 2 "derde" 3 "vierde" 4 "vijfde" 5
              "zesde" 6 "zevende" 7 "achtste" 8 "negende" 9 "tiende" 10 "elfde" 11
              "twaalfde" 12 "dertiende" 13 "veertiende" 14 "vijftiende" 15 "zestiende" 16
              "zeventiende" 17 "achttiende" 18 "negentiende" 19}
              (-> %1 :groups first .toLowerCase))}

  "rangtelwoorden (cijfers)"
  #"0*(\d+) ?(e|ste|de)"
  {:dim :ordinal
   :value (read-string (first (:groups %1)))}  ; read-string not the safest

  
  )
