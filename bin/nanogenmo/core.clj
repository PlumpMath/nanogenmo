(ns nanogenmo.core
  (:require [clojure.pprint]
            [opennlp.nlp]
            [opennlp.treebank]
            [opennlp.tools.filters])
  (:use [clojure.pprint]
        [opennlp.nlp]
        [opennlp.treebank]
        [opennlp.tools.filters])
  (:gen-class))

(use 'clojure.java.io)

(def get-sentences (make-sentence-detector "models/en-sent.bin"))
(def tokenize (make-tokenizer "models/en-token.bin"))
(def detokenize (make-detokenizer "models/english-detokenizer.xml"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def name-find (make-name-finder "models/namefind/en-ner-person.bin"))
(def chunker (make-treebank-chunker "models/en-chunker.bin"))
(def parser (make-treebank-parser "models/en-parser-chunking.bin"))

;;; Workaround until the bug gets fixed...
(defmacro fixed-chunk-filter
  "Declare a filter for treebank-chunked lists with the given name and regex."
  [n r]
  (let [docstring (str "Given a list of treebank-chunked elements, "
                       "return only the " n " in a list.")]
    `(defn ~n
       ~docstring
       [elements#]
       (filter (fn [t#] (re-find ~r (:tag t#))) 
               (remove #(nil? (:tag %)) elements#)))))

(fixed-chunk-filter fixed-noun-phrases #"^NP$")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;(defn input-source-text [source-text]
;  (with-open [rdr (reader source-text)]
;    (doseq [line (line-seq rdr)]
;      (println line))))

(defn strip-italics [text]
  "Removes _underscored italics_ from the text. It'd be nice to find a way to include these, later."
  (clojure.string/replace text "_" ""))

(defn strip-linebreaks [text]
  "Returns string with linebreaks stripped out."
   (clojure.string/replace 
     (clojure.string/replace text #"[\r\n]+" "_@_")
     "_@_" " "))
  
;(defn input-source-text-2 [source-text]
;  (get-sentences (slurp source-text)))

(defn mark-paragraphs [source-text]
  (clojure.string/replace source-text #"\r\n\r\n" "¶"))

(defn get-paragraphs [source-text]
  "Returns source-texts broken into a list of paragraphs."
  (clojure.string/split source-text #"¶"))

(defn categorize-paragraph [source-text]
  "Returns a category based on the contents of the source-text: dialog, action, or exposition."
  (cond
    (re-find #"\"" source-text) :dialogue
    (re-find #",$" source-text) :ends-with-comma ;only last sentence is dialogue
    ;(re-find #"\b(She|she|He|he)\b" source-text) :action
    :else :action))

(defn input-source-text-directly [source-text]
  "Takes cleaned source text and formats it into useful paragraphs."
  (get-paragraphs (strip-italics (strip-linebreaks 
                    (mark-paragraphs 
                       source-text)))))

(defn input-source-text-from-file [filename]
  "Takes cleaned source text and formats it into useful paragraphs."
  (input-source-text-directly 
    (slurp filename)))

(defn categorize-text [paragraphs]
  "Takes a vector of paragraphs and returns a categorized map of paragraphs."
  (map #(hash-map :category (categorize-paragraph %) :text %)
       paragraphs))

(defn paragraph-to-sentences [paragraph]
  "Takes paragraph-map-data and assocs the sentence breakdown."
  (assoc paragraph :sentences (get-sentences (:text paragraph))))
  ;(assoc paragraph (hash-map :sentences (get-sentences (:text paragraph)))))

(defn paragraph-to-typed-sentences [paragraphs]
  (map #(assoc % :categorized 
               (let [sentences (get-sentences (:text %))
                     category (:category %)]
                 (cond 
                   (= category :dialogue) {:dialogue sentences}
                   (= category :action) {:action sentences}
                   (= category :ends-with-comma) {:action (butlast sentences) 
                                                  :dialogue (last sentences)}
                   )))
         paragraphs))

;(defn detect-exposition-in-sentence [sentence]
;  (cond
;    (re-find #"\b(She|she|He|he)\b" sentence) :exposition
;    :else :action))
  
;(defn detect-exposition-in-paragraph [paragraph] 
;  "Look through the sentences in the paragraph and sort out which ones have action and which ones are mostly expository.
;Right now this is based on guessing via the presence of he/she, but a later refinement might want to look at the verb."
;  (let [action-sentences (:action (:categorized paragraph))]
;    (map #(detect-exposition-in-sentence %) action-sentences)
;    ))
  
(defn sentences-from-paragraphs [paragraphs]
  "Given a collection of paragraphs, grab just the sentences."
  (mapcat #(:sentences %) paragraphs))

 ; Take the text file and sort it into paragraphs, break those paragraphs down into sentence types
 
;(defn categorize-into-paragraphs [source-text]
;  (categorize-text
;    (input-source-text-directly source-text)))

(defn grab-sentences-of-type [paragraphs sentence-type]
  (sentences-from-paragraphs
         (map paragraph-to-sentences 
              (filter #(= (:category %) sentence-type)
                      paragraphs))))

(defn first-categorization [source-text type-list]
  (let [paragraphs (categorize-into-paragraphs source-text)]
    (map #(grab-sentences-of-type paragraphs %) type-list)))

(defn get-sentences-of-category [paragraphs category]
  (mapcat #(category (:categorized %)) paragraphs))

(defn get-actions [source-text]
  (get-sentences-of-category
         (paragraph-to-typed-sentences
           (categorize-text 
            (input-source-text-directly
              source-text)))
         :action))

(defn create-action [sentence]
  "Takes an action sentence and converts it to a valid action-sentence-function that can be called by the characters."
  )

(defn display-action [action]
  "Takes a formatted action-sentence-function and runs it, returning a string that can be printed."
  )

;(binding [*print-right-margin* nil]
;  (pprint
;    (get-actions (slurp "texts\\cleaned\\pnp_excerpt.txt"))))



(defn process-action [sentence]
  (noun-phrases (chunker (pos-tag (tokenize sentence)))))

(defn test-action-processing []
  (map #(process-action %)
       (get-actions (slurp "texts\\cleaned\\pnp_excerpt2.txt"))))

(with-open [wrtr (writer "texts\\output\\test3.txt")]
  (write
    (test-action-processing)
    :pretty true
    :stream wrtr))

(pprint (test-action-processing))

(pprint 
  (noun-phrases 
    (chunker 
      (pos-tag 
        (tokenize "And when the party entered the assembly room, it consisted of only five
altogether.")))))


    
    
    

(comment
(binding [*print-right-margin* nil]
  (pprint
    ;(map #(detect-exposition-in-paragraph %)
    (get-sentences-of-category
         (paragraph-to-typed-sentences
           (categorize-text 
            (input-source-text-directly
              (slurp "texts\\cleaned\\pnp_excerpt.txt"))))
         :action))))

           ;[:action :dialogue :starts-with-comma :exposition])))
 
   ;(input-source-text "texts\\cleaned\\pnp_excerpt.txt")
  
   ;(spit  "texts\\output\\test.txt"
   ;(binding [*print-right-margin* nil]
   ;(map #(pprint %)
   ;(pprint
 
(comment   
(defn test-pnp-processing []
  (with-open [wrtr (writer "texts\\output\\test2.txt")]
    (write
      ;(sentences-from-paragraphs
        ;(map paragraph-to-sentences 
             ;(filter #(= (:category %) :action)
                     (categorize-text
                       (input-source-text-from-file
                          "texts\\cleaned\\pnp_excerpt.txt"))
      :pretty true
      :stream wrtr))))
;(test-pnp-processing)
 
 

 

;(get-sentences (strip-linebreaks
;(get-paragraphs (strip-linebreaks (mark-paragraphs (slurp "texts\\cleaned\\pnp_excerpt.txt"))))

