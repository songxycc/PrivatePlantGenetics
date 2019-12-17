/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analysis.wheatHapMap;

import format.table.ColumnTable;
import format.table.RowTable;
import utils.Dyad;
import utils.IOUtils;
import utils.PStringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * @author feilu
 */
public class DeleteriousBiology {
    
    public DeleteriousBiology () {
        //this.countDeleteriousAndSyn();
        //this.identifyDeleteriousAndSyn();
        //this.mkVCFofDeleteriousAndSyn();
    }

    public void mkVCFofDeleteriousAndSyn () {
        String vmapDirS = "/Volumes/Fei_HDD_Mac/VMap2.1";
        String delInfoDirS = "/Users/feilu/Documents/analysisH/vmap2/004_deleteriousBiology/snp/del";
        String synInfoDirS = "/Users/feilu/Documents/analysisH/vmap2/004_deleteriousBiology/snp/syn";
        String delVcfDirS = "/Users/feilu/Documents/analysisH/vmap2/004_deleteriousBiology/vcf/del";
        String synVcfDirS = "/Users/feilu/Documents/analysisH/vmap2/004_deleteriousBiology/vcf/syn";
        List<File> fList = IOUtils.getFileListInDirEndsWith(vmapDirS, ".gz");
        fList.parallelStream().forEach(f -> {
            String chrS = f.getName().split("_")[0];
            String delInfoFileS = new File (delInfoDirS, chrS+"_SNP_anno.txt.gz").getAbsolutePath();
            String synInfoFileS = new File (synInfoDirS, chrS+"_SNP_anno.txt.gz").getAbsolutePath();
            String delVcfFileS = new File (delVcfDirS, chrS+"_del_vmap2.1.vcf.gz").getAbsolutePath();
            String synVcfFileS = new File (synVcfDirS, chrS+"_syn_vmap2.1.vcf.gz").getAbsolutePath();
            try {
                ColumnTable<String> t = new ColumnTable<>(delInfoFileS);
                int[] delsites = t.getColumnAsIntArray(2);
                Arrays.sort(delsites);
                t = new ColumnTable<>(synInfoFileS);
                int[] synsites = t.getColumnAsIntArray(2);
                Arrays.sort(synsites);
                BufferedWriter bwd = IOUtils.getTextGzipWriter(delVcfFileS);
                BufferedWriter bws = IOUtils.getTextGzipWriter(synVcfFileS);
                BufferedReader br = IOUtils.getTextGzipReader(f.getAbsolutePath());
                String temp = null;
                while ((temp = br.readLine()).startsWith("##")) {
                    bwd.write(temp); bwd.newLine();
                    bws.write(temp); bws.newLine();
                }
                bwd.write(temp); bwd.newLine();
                bws.write(temp); bws.newLine();
                List<String> l = new ArrayList<>();
                int index = -1;
                int pos = -1;
                while ((temp = br.readLine()) != null) {
                    l = PStringUtils.fastSplit(temp.substring(0, 60));
                    pos = Integer.parseInt(l.get(1));
                    if (Arrays.binarySearch(delsites, pos) >= 0) {
                        bwd.write(temp);bwd.newLine();
                    }
                    else if (Arrays.binarySearch(synsites, pos) >= 0) {
                        bws.write(temp);bws.newLine();
                    }
                }
                bwd.flush();bwd.close();
                bws.flush();bws.close();
                br.close();
                System.out.println(f.getName());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void identifyDeleteriousAndSyn () {
        String inDirS = "/Users/feilu/Documents/analysisH/vmap2/002_genicSNP/genicSNPAnnotation";
        String delDirS = "/Users/feilu/Documents/analysisH/vmap2/004_deleteriousBiology/snp/del";
        String synDirS = "/Users/feilu/Documents/analysisH/vmap2/004_deleteriousBiology/snp/syn";
        double siftThresh = 0.05;
        double gerpThresh = 1;
        List<File> fList = IOUtils.getFileListInDirEndsWith(inDirS, ".gz");
        LongAdder counterDel = new LongAdder();
        LongAdder counterSyn = new LongAdder();
        fList.stream().forEach(f -> {
            Dyad<String, List<String>> two = VMapDBUtils.getDBInfo(f.getAbsolutePath());
            String delFileS = new File (delDirS, f.getName()).getAbsolutePath();
            String synFileS = new File (synDirS, f.getName()).getAbsolutePath();
            String header = two.getFirstElement();
            List<String> recordList = two.getSecondElement();
            int typeIndex = -1;
            int siftIndex = -1;
            int gerpIndex = -1;
            int cnt = 0;
            int cntSyn = 0;
            String[] temp = header.split("\t");
            for (int i = 0; i < temp.length; i++) {
                if (temp[i].equals("Variant_type")) typeIndex = i;
                else if (temp[i].equals("SIFT_score")) siftIndex = i;
                else if (temp[i].equals("Gerp")) gerpIndex = i;
            }
            List<String> l = null;
            try {
                BufferedWriter bwd = IOUtils.getTextGzipWriter(delFileS);
                BufferedWriter bws = IOUtils.getTextGzipWriter(synFileS);
                bwd.write(header);
                bwd.newLine();
                bws.write(header);
                bws.newLine();
                for (int i = 0; i < recordList.size(); i++) {
                    l = PStringUtils.fastSplit(recordList.get(i));
                    if (l.get(typeIndex).startsWith("SYN")) {
                        cntSyn++;
                        bws.write(recordList.get(i));
                        bws.newLine();
                    }
                    if (!l.get(typeIndex).startsWith("NONS")) continue;
                    if (l.get(siftIndex).startsWith("N")) continue;
                    if (l.get(gerpIndex).startsWith("N")) continue;
                    if (Double.parseDouble(l.get(siftIndex)) < siftThresh && Double.parseDouble(l.get(gerpIndex)) > gerpThresh) {
                        cnt++;
                        bwd.write(recordList.get(i));
                        bwd.newLine();
                    }
                }
                bwd.flush();
                bwd.close();
                bws.flush();
                bws.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Del:\t").append(cnt).append("\tSyn:\t").append(cntSyn).append("\t").append(f.getName());
            System.out.println(sb.toString());
            counterDel.add(cnt);
            counterSyn.add(cntSyn);
        });
        StringBuilder sb = new StringBuilder();
        sb.append("Del:\t").append(counterDel.intValue()).append("\t").append(counterSyn.intValue());
        System.out.println(sb.toString());
    }

    public void countDeleteriousAndSyn() {
        String inDirS = "/Users/feilu/Documents/analysisH/vmap2/002_genicSNP/genicSNPAnnotation";
        double siftThresh = 0.05;
        double gerpThresh = 1;
        List<File> fList = IOUtils.getFileListInDirEndsWith(inDirS, ".gz");
        LongAdder counterDel = new LongAdder();
        LongAdder counterSyn = new LongAdder();
        fList.stream().forEach(f -> {
            RowTable<String> t = new RowTable<>(f.getAbsolutePath());
            int typeIndex = t.getColumnIndex("Variant_type");
            int siftIndex = t.getColumnIndex("SIFT_score");
            int gerpIndex = t.getColumnIndex("Gerp");
            int cnt = 0;
            int cntSyn = 0;
            for (int i = 0; i < t.getRowNumber(); i++) {
                if (t.getCell(i, typeIndex).startsWith("SYN")) cntSyn++;
                if (!t.getCell(i, typeIndex).startsWith("NONS")) continue;
                if (t.getCell(i, siftIndex).startsWith("N")) {
                    continue;
                }
                if (t.getCell(i, gerpIndex).startsWith("N")) continue;
                if (Double.parseDouble(t.getCell(i, siftIndex)) < siftThresh && Double.parseDouble(t.getCell(i, gerpIndex)) > gerpThresh) {
                    cnt++;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Del:\t").append(cnt).append("\tSyn:\t").append(cntSyn).append("\t").append(f.getAbsoluteFile());
            System.out.println(sb.toString());
            counterDel.add(cnt);
            counterSyn.add(cntSyn);
        });
        StringBuilder sb = new StringBuilder();
        sb.append("Del:\t").append(counterDel.intValue()).append("\t").append(counterSyn.intValue());
        System.out.println(sb.toString());
    }
}