package vwm2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.util.Pair;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;
import org.ejml.simple.SimpleMatrix;

public class Model {

    public Model() {

    }
    final static Charset ENCODING = StandardCharsets.UTF_8;
    List<String> files;
    private File folder;
    static int K = 50;
    private TermByDoc termByDocMatrix;
    private Map<String, Integer> docTermFreq;
    private Map<String, Integer> termMaxFreq;
    private DenseMatrix64F SU;
    private DenseMatrix64F VK;

    static List<String> readFile(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        return Files.readAllLines(path, ENCODING);
    }

    static List<String> readFiles(File dir) {
        List<String> fileNames = new ArrayList();
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.isDirectory()) {
                    fileNames.addAll(readFiles(child));
                } else {
                    fileNames.add(child.getAbsolutePath());
                }
            }
        }
        return fileNames;
    }

    /**
     *
     * @param files soubory na prohledani
     * @return Vraci pair map term -> maximalni pocet vyskytu v jednom souboru a
     * term -> pocet dokumentu obsahujici term napric cele kolekci
     */
    static Pair<Map<String, Integer>, Map<String, Integer>> extractTerms(List<String> files) {
        // maximalni vyskyt termu v jednom souboru
        Map<String, Integer> termMaxFreq = new TreeMap();

        // v kolika dokumentech je term pouzit
        Map<String, Integer> docTermFreq = new TreeMap();

        for (String file : files) {

            Map<String, Integer> documentTermCount = loadDocument(file);

            // na Max{fij}
            // projdu mapu, ktera obsahuje termy z aktualne prochazeneho souboru a pocet vyskytu
            // a porovnam to s mapou termu cele kolekce, pokud to tam jeste neni nebo aktualni soubor
            // ma vic vyskytu daneho termu, tak to tam pridam/zmenim pocet vyskytu. 
            // a zaroven pocitam, v kolika dokumentech se term vyskytuje
            for (String key : documentTermCount.keySet()) {
                int newCount = documentTermCount.get(key);
                if (termMaxFreq.containsKey(key)) {
                    int oldCount = termMaxFreq.get(key);
                    if (newCount > oldCount) {
                        termMaxFreq.put(key, newCount);
                    }

                    int usage = docTermFreq.get(key);
                    docTermFreq.put(key, usage + 1);
                } else {
                    termMaxFreq.put(key, newCount);
                    docTermFreq.put(key, 1);
                }

            }

        }

        return new Pair<>(termMaxFreq, docTermFreq);
    }

    /**
     * nacte jeden dokument do mapy
     *
     * @param file path
     * @return
     */
    static Map<String, Integer> loadDocument(String document) {
        Map<String, Integer> termFreqInDoc = new TreeMap();
        Stemmer s = new Stemmer();
        List<String> fileLines;

        try {
            fileLines = readFile(document);
        } catch (IOException e) {
            System.out.println("Nepodarilo se otevrit soubor " + document);
            return termFreqInDoc;
        }
        for (String line : fileLines) {
            String[] words = line.split(" ");

            for (String word : words) {
                s.add(word.toLowerCase().toCharArray(), word.length());
                s.stem();
                String stemmedTerm = s.trim(String.valueOf(s.getResultBuffer(), 0, s.getResultLength()));

                if (stemmedTerm.length() > 0) {
                    if (termFreqInDoc.containsKey(stemmedTerm)) {
                        int count = termFreqInDoc.get(stemmedTerm);
                        termFreqInDoc.put(stemmedTerm, count + 1);
                    } else {
                        termFreqInDoc.put(stemmedTerm, 1);
                    }
                }
            }
        }
        return termFreqInDoc;
    }

    static SimpleMatrix queryToVector(String query, Set<String> terms) {
        Stemmer s = new Stemmer();
        String[] words = query.split(" ");
        List<String> termsQuery = new ArrayList<>();

        for (String word : words) {
            s.add(word.toLowerCase().toCharArray(), word.length());
            s.stem();
            String stemmed = s.trim(String.valueOf(s.getResultBuffer(), 0, s.getResultLength()));

            if (stemmed.length() > 0) {
                if (!termsQuery.contains(stemmed)) {
                    termsQuery.add(stemmed);
                }
            }
        }

        SimpleMatrix matrix = new SimpleMatrix(terms.size(), 1);

        int i = 0;
        for (String term : terms) {
            if (termsQuery.contains(term)) {
                matrix.set(i++, 0, 1.0);
            } else {
                matrix.set(i++, 0, 0.0);
            }
        }
        return matrix;
    }

    static DenseMatrix64F termToConcept(DenseMatrix64F D, DenseMatrix64F SU) {
        DenseMatrix64F res = new DenseMatrix64F(SU.numRows, D.numCols);
        CommonOps.mult(SU, D, res);
        return res;
    }

    static double cosineDistance(DenseMatrix64F a, DenseMatrix64F b) {
        int size = a.getNumCols() > a.getNumRows() ? a.getNumCols() : a.getNumRows();

        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        double cosineDisance;

        for (int i = 0; i < size; i++) {
            dotProduct += a.get(i) * b.get(i);
            magnitude1 += Math.pow(a.get(i), 2);
            magnitude2 += Math.pow(b.get(i), 2);
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        if (magnitude1 != 0 && magnitude2 != 0) {
            cosineDisance = dotProduct / (magnitude1 * magnitude2);
        } else {
            cosineDisance = 0.0;
        }
        return cosineDisance;
    }

    /*
     * na vypis mapy serazeny podle hodnot
     */
    static <K, V extends Comparable<? super V>>
            SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                int res = e1.getValue().compareTo(e2.getValue());
                return (res != 0 ? res * (-1) : 1);
            }
        }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public void init(String folderPath) {
        folder = new File(folderPath);//pokus\\pokus
        files = readFiles(folder);
        // pair dvou map, prvni je term -> nejvyssi vyskyt termu pres celou kolekci = termMaxFreq
        // druha je term -> pocet dokumentui, ve kterych se objevuje                = docTermFreq
        Pair<Map<String, Integer>, Map<String, Integer>> extractedTerms = extractTerms(files);

        termMaxFreq = extractedTerms.getKey(); // term -> nejvyssi vyskyt termu pres velou kolekci

        docTermFreq = extractedTerms.getValue(); // term -> pocet dokumentu, ve kterych se objevuje 

        int numberOfFiles = files.size();
        TermByDoc termByDocMatrix = new TermByDoc(files, termMaxFreq, docTermFreq, numberOfFiles);
        SimpleMatrix A = termByDocMatrix.createSimpleMatrix();

        SingularValueDecomposition<DenseMatrix64F> SVD = DecompositionFactory.svd(A.numRows(), A.numCols(), true, true, false);
        SVD.decompose(A.getMatrix());

        DenseMatrix64F U = SVD.getU(null, false);
        DenseMatrix64F S = SVD.getW(null);
        DenseMatrix64F VT = SVD.getV(null, true);

        SingularOps.descendingOrder(U, false, S, VT, true);

        K = SVD.numberOfSingularValues() < K ? SVD.numberOfSingularValues() : K;
        //K = SVD.numberOfSingularValues();
        // SK 
        DenseMatrix64F SK = CommonOps.extract(S, 0, K, 0, K);

        // SK^(-1) 
        DenseMatrix64F SKInverted = new DenseMatrix64F(SK);
        CommonOps.invert(SKInverted);

        // UK 
        DenseMatrix64F UK = CommonOps.extract(U, 0, U.numRows, 0, K);

        // VK 
        VK = CommonOps.extract(VT, 0, K, 0, VT.numCols);

        // S^(-1)*U^T 
        SU = new DenseMatrix64F(SKInverted.numRows, UK.numRows);
        CommonOps.multTransB(SKInverted, UK, SU);

    }

    public List<Map.Entry<String, Double>> search(String file) {
        System.out.println("Hledas " + file);
        List<String> fileArray = new ArrayList<>();
        fileArray.add(file);
        TermByDoc query = new TermByDoc(fileArray, termMaxFreq, docTermFreq, files.size());

        // Query Q
        SimpleMatrix queryVector = query.createSimpleMatrix();

        // S^(-1)*U^T*Q #docs x 1 
        DenseMatrix64F resultVector = termToConcept(queryVector.getMatrix(), SU);

        Map<String, DenseMatrix64F> documentVectors = new TreeMap<>();
        for (int i = 0; i < K; i++) {
            DenseMatrix64F tmp = CommonOps.extractColumn(VK, i, null);
            documentVectors.put(files.get(i), tmp);
        }
        String folderPath = folder.getAbsolutePath() + "\\";
        Map<String, Double> cosineDistances = new TreeMap<>();
        for (Map.Entry<String, DenseMatrix64F> documentVector : documentVectors.entrySet()) {
            cosineDistances.put(documentVector.getKey().replace(folderPath, ""), cosineDistance(documentVector.getValue(), resultVector));
        }

        System.out.println(entriesSortedByValues(cosineDistances));
        int i = 0;
        List<Map.Entry<String, Double>> results = new ArrayList();


        for (Map.Entry row : entriesSortedByValues(cosineDistances)) {
            results.add(row);
            if (i++ > 15);
        }
        return results;
    }

}
