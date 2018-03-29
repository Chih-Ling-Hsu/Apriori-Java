import java.lang.*;
import java.util.*;
import java.io.*;

public class Apriori{

    /** the name of the transcation file */
    private String inputPath;
    /** total number of transactions in transcation file */
    private int numTransactions; 
    /** list of transactions in transcation file */
    private List<String[]> transactions = new ArrayList<String[]>();
    /** minimum support for a frequent itemset in percentage, e.g. 0.8 */
    private double minSup; 
    /** minimum confidence for a rule in percentage, e.g. 0.8 */
    private double minConf;
    /** the list of collected frequent itemsets */
    private Map<String[], Integer> freqItemsets;
    /** the list of association rules */
    private Map<String, Double> associationRules;

    

    public  Apriori(String[] args){
    	inputPath = args[0];
        minSup = Double.valueOf(args[1]).doubleValue();
        log("Threshold of Support:" + minSup);
        if(args.length>=3){
            minConf = Double.valueOf(args[2]).doubleValue();
            log("Threshold of Confidence:" + minConf);  
        } 
    }



    public Map<String, Double> findAssociationRules(){      
        /** the list of association rules */
        associationRules = new HashMap<String, Double>();

        for(String[] itemset: freqItemsets.keySet()){
            List<String[]> candidates = new ArrayList<String[]>();
            for(String item : itemset){
                candidates.add(new String[] {item});
            }
            genRule(itemset, freqItemsets.get(itemset), candidates);
        }

        return associationRules;
    }

    public Map<String[], Integer> findFreqItemsets(){
        
        /** the list of collected frequent itemsets */
        freqItemsets = new HashMap<String[], Integer>();
        /** the list of the candidate frequent itemsets */
        List<String[]> candidates = new ArrayList<String[]>();
        /** the list of the (k-1)th frequent itemsets */
        List<String[]> itemsets = new ArrayList<String[]>();

        itemsets = genItemsetsOfSize1(inputPath);
        //log("Threshold of Support Count:" + minSup*numTransactions);
        //log("Threshold of Confidence:" + minConf);
        //log("Number of transactions:" + numTransactions);
        //log("Number of frequent 1-itemsets: " + itemsets.size());
        //log("---");
        
        int k;      // the size of a itemset in itemsets
        for(k=2; itemsets.size()>0; k++){
            
            candidates = genCandidates(itemsets);
    	    //log("Generated ["+candidates.size()+"] unique candidate itemsets of size "+(k));
            itemsets.clear();
            
            if(candidates.size()==0) break;

            itemsets = extractFreqitemsets(candidates);
            //log("Extracted ["+ itemsets.size()+ "] frequent itemsets of size "+ k);
            candidates.clear();

            //log("---");
        }
        return freqItemsets;
    }
    private int countSupport(String[] itemset){
        int count = 0;
        for(String[] t : transactions){
            for(int a=0, b=0; b<t.length; b++){
                if(t[b].equals(itemset[a])){
                    a++;
                }
                if(a==itemset.length){
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private void genRule(String[] itemset, int count, List<String[]> candidates){
        int k = itemset.length;
        int m = candidates.get(0).length;

        if(k >= m+1){
            for(int i=candidates.size()-1; i>=0; i--){
                String[] candidate = candidates.get(i);

                Set<String> f = new HashSet<String>(Arrays.asList(itemset));
                Set<String> h = new HashSet<String>(Arrays.asList(candidate));
                f.removeAll(h);
                String[] antecedent = f.toArray(new String[f.size()]);
                Arrays.sort(antecedent);

                double conf = (double)count/countSupport(antecedent);
                if (conf >= minConf){
                    String rule = Arrays.toString(antecedent) +" -> "+ Arrays.toString(candidate);
                    //log(rule + ", Confidence: " + conf);
                    associationRules.put(rule, conf);
                }
                else{
                    candidates.remove(i);
                }
            }
            candidates = genCandidates(candidates);
            if(candidates.size()!=0){
                genRule(itemset, count, candidates);
            }
        }
        return;
    }

    private List<String[]> extractFreqitemsets(List<String[]> candidates){
        List<String[]> itemsets = new ArrayList<String[]>();
        double threshold = minSup*numTransactions;
        int count[] = new int[candidates.size()];

        for(String[] t : transactions){
            for(int i=0; i<candidates.size(); i++){
                String[] itemset = candidates.get(i);
                for(int a=0, b=0; b<t.length; b++){
                    if(t[b].equals(itemset[a])){
                        a++;
                    }
                    if(a==itemset.length){
                        count[i]++;
                        break;
                    }
                }
            }
        }

        //log("The following candidates are pruned:");
        for (int i=0; i<candidates.size(); i++){
            if(count[i] >= threshold){
                itemsets.add(candidates.get(i));
                freqItemsets.put(candidates.get(i), Integer.valueOf(count[i]));
            }
            //else{
            //    log(Arrays.toString(candidates.get(i)) + ", "+count[i]);
            //}
        }
        return itemsets;
    }
    private List<String[]> genCandidates(List<String[]> itemsets){
        List<String[]> candidates = new ArrayList<String[]>();
        Set<String> candidateStrings = new HashSet<String>();

        for(int i=0; i<itemsets.size(); i++) {
            for(int j=0; j<itemsets.size(); j++) {
                if(i==j) continue;

                String[] X = itemsets.get(i);
                String[] Y = itemsets.get(j);

                int ndifferent = 0;
                String diffItem = "";
                for(int a=0; a<X.length; a++){
                    boolean identical = false;
                    for(int b=0; b<Y.length; b++){
                        if(Y[b].equals(X[a])){
                            identical = true;
                            break;
                        }
                    }
                    if(!identical){
                        ndifferent++;
                        diffItem = X[a];
                    }
                }

                assert(ndifferent>0);

                if (ndifferent==1) {
                    String[] candidate =  new String[Y.length+1];
                    for(int b=0; b<Y.length; b++){
                        candidate[b] = Y[b];
                    }
                    candidate[Y.length] = diffItem;

                	Arrays.sort(candidate);
                    String candidateString = Arrays.toString(candidate);
                    if(candidateStrings.contains(candidateString)) continue;
                    else{
                        candidateStrings.add(candidateString);
                        candidates.add(candidate);
                    }
                }
            }
        }
        return candidates;
    }

    private List<String[]> genItemsetsOfSize1(String inputPath){
        Map<String, Integer> itemsOfSize1 = new HashMap<String, Integer>();
        List<String[]> itemsets = new ArrayList<String[]>();
        numTransactions = 0;
        try{
            BufferedReader data_in = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));
            while (data_in.ready()) {
                String[] t = data_in.readLine().split("[,]+[\\s]*");
                Arrays.sort(t);
                transactions.add(t);

                for (String item : t){
                    Integer count = itemsOfSize1.get(item);
                    if (count == null) {
                        itemsOfSize1.put(item, 1);
                        count = 1;
                    }
                    else {
                        itemsOfSize1.put(item, count + 1);
                    }
                }                

                numTransactions ++;
            }
            data_in.close();

            double threshold = minSup*numTransactions;
            for(String item: itemsOfSize1.keySet()){
                if(itemsOfSize1.get(item) >= threshold){
                    String[] itemset = new String[] {item};
                    itemsets.add(itemset);
                    freqItemsets.put(itemset, itemsOfSize1.get(item));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return itemsets;
    }

    private void log(String message){
        System.out.println(message);
    }


}