package vwm2;

import java.util.List;
import java.util.Map;
import org.ejml.simple.SimpleMatrix;

public class TermByDoc {

    private double[][] matrixData;
    /**
     * Spocita vahy termu.
     * @param files seznam souboru
     * @param termMaxFreq mapa term -> maximalni vyskyt v dokumentu napric celou kolekci
     * @param documentsContainingTerm mapa term -> pocet dokumentu obsahujuci dany term
     */
    public TermByDoc(List<String> files, Map<String, Integer> termMaxFreq, Map<String, Integer> documentsContainingTerm, int numberOfFiles) {
        matrixData = new double[termMaxFreq.size()][files.size()];
        
        for (int i = 0; i < files.size(); i++) {
            Map<String, Integer> termsInDoc =  Model.loadDocument(files.get(i)); // pocita pocet termu v ramci jednoho souboru
            int j = 0; // aktualni term, kvuli ty matici
            for(String term : termMaxFreq.keySet()){ // iterace pres vsechny termy v kolekci
                double weight = 0.0;
                
                if (termsInDoc.containsKey(term)) { // pokud se term objevil v souboru i
                    double tf = termsInDoc.get(term) / termMaxFreq.get(term); // vyskyt termu v souboru i / maximalni vyskyt termu 
                    weight = computeWeight(tf, documentsContainingTerm.get(term), numberOfFiles);
                }
                matrixData[j][i] = weight;
                j++;
            }
        }
    }

    private double computeWeight(double tf, int df, int n) {
        return tf * Math.log10((double) n / (double) df) / Math.log10(2); // log2(x) = log10(x)/log10(2)
    }

    public SimpleMatrix createSimpleMatrix() {
        return new SimpleMatrix(matrixData);
    }

}
